package com.huadi.campus.selection;

import com.huadi.campus.entity.Course;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.CourseSelectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Redis 实现：名额是 String（剩余数），已选学生是 Set，两者由 Lua 脚本原子修改。
 * <p>
 * 为什么用 Lua：Redis 单线程执行脚本，"判断是否已选 → 判断余量 → 扣减 → 记录学生"四步之间
 * 不会被其它客户端的命令插入，等价于数据库里的原子 UPDATE，但没有磁盘与行锁开销。
 * <p>
 * 数据库里的 course.current_students 在此模式下只是一个镜像：由落库消费者 +1、退选 -1，
 * 供列表页展示，不参与名额判断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.selection.stock", havingValue = "redis")
public class RedisStockService implements StockService {

    private static final DefaultRedisScript<Long> OCCUPY = script("lua/stock_occupy.lua");
    private static final DefaultRedisScript<Long> RELEASE = script("lua/stock_release.lua");
    private static final DefaultRedisScript<Long> RELOAD = script("lua/stock_reload.lua");

    private final StringRedisTemplate redis;
    private final CourseMapper courseMapper;
    private final CourseSelectionMapper selectionMapper;
    private final RedisKeys keys;

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public ReserveResult reserve(Long courseId, Long studentId) {
        try {
            long code = occupy(courseId, studentId);
            if (code == -2) {
                // 缓存缺失（首次访问 / Redis 重启 / key 被淘汰）：从数据库加载一次再重试
                loadFromDatabase(courseId);
                code = occupy(courseId, studentId);
            }
            return switch ((int) code) {
                case 1 -> ReserveResult.ACQUIRED;
                case -1 -> ReserveResult.DUPLICATE;
                case -2 -> ReserveResult.NOT_FOUND;
                default -> ReserveResult.SOLD_OUT;
            };
        } catch (DataAccessException e) {
            // 连接失败 / 命令超时：交给上层降级到数据库实现
            throw new StockUnavailableException(e);
        }
    }

    private long occupy(Long courseId, Long studentId) {
        Long code = redis.execute(OCCUPY,
                List.of(keys.stock(courseId), keys.selected(courseId)),
                String.valueOf(studentId));
        return code == null ? 0 : code;
    }

    @Override
    public void onPersisted(SelectionCommand command) {
        // 名额在 Redis 里已扣；这里只维护数据库镜像列，供课程列表展示
        courseMapper.incrementStudents(command.courseId());
    }

    @Override
    public void releaseReservation(SelectionCommand command) {
        release(command.courseId(), command.studentId());
    }

    @Override
    public void releaseForDrop(Long courseId, Long studentId) {
        release(courseId, studentId);
    }

    private void release(Long courseId, Long studentId) {
        Long removed = redis.execute(RELEASE,
                List.of(keys.stock(courseId), keys.selected(courseId)),
                String.valueOf(studentId));
        if (removed == null || removed == 0) {
            log.warn("释放名额时学生不在集合中（可能缓存已被重建）courseId={} studentId={}", courseId, studentId);
        }
    }

    @Override
    public Integer remaining(Long courseId) {
        String value = redis.opsForValue().get(keys.stock(courseId));
        return value == null ? null : Integer.parseInt(value);
    }

    /** 启动 / 手动刷新：遍历所有课程，以数据库记录为准重建缓存。注意应在 MQ 积压为空时执行 */
    @Override
    public int warmUp() {
        List<Course> courses = courseMapper.selectList(null);
        for (Course course : courses) {
            reload(course);
        }
        log.info("Redis 名额缓存预热完成，共 {} 门课程", courses.size());
        return courses.size();
    }

    /** 单门课程重建：管理员改了名额上限 / 时间窗口后调用 */
    @Override
    public int warmUp(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            redis.delete(List.of(keys.stock(courseId), keys.selected(courseId)));
            return 0;
        }
        reload(course);
        return 1;
    }

    /** 懒加载：用 SET NX 做互斥，避免缓存失效瞬间大量请求同时查库（击穿）；没抢到锁的线程等锁持有者写完 */
    private void loadFromDatabase(Long courseId) {
        Boolean locked = redis.opsForValue().setIfAbsent(keys.loadLock(courseId), "1", Duration.ofSeconds(3));
        if (!Boolean.TRUE.equals(locked)) {
            for (int i = 0; i < 20 && !Boolean.TRUE.equals(redis.hasKey(keys.stock(courseId))); i++) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            return;
        }
        try {
            Course course = courseMapper.selectById(courseId);
            if (course != null) {
                reload(course);
            }
        } finally {
            redis.delete(keys.loadLock(courseId));
        }
    }

    private void reload(Course course) {
        List<String> enrolled = selectionMapper.listEnrolledStudentIds(course.getId()).stream()
                .map(String::valueOf)
                .toList();
        int max = course.getMaxStudents() == null ? 0 : course.getMaxStudents();
        String[] args = new String[enrolled.size() + 1];
        args[0] = String.valueOf(Math.max(0, max - enrolled.size()));
        for (int i = 0; i < enrolled.size(); i++) {
            args[i + 1] = enrolled.get(i);
        }
        redis.execute(RELOAD, List.of(keys.stock(course.getId()), keys.selected(course.getId())), args);
    }
}
