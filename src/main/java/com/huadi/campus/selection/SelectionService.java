package com.huadi.campus.selection;

import com.huadi.campus.config.SelectionProperties;
import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.entity.Student;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.exception.ConflictException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.CourseSelectionMapper;
import com.huadi.campus.mapper.StudentMapper;
import com.huadi.campus.vo.SelectionResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 选课 / 退选的编排层。一次选课请求的处理顺序：
 * <pre>
 * 1. 本地缓存校验：学生、课程是否存在，是否在选课时间窗口内        —— 不碰数据库 / Redis
 * 2. StockService.reserve：预占名额（Redis Lua 或数据库原子 UPDATE） —— 热路径里唯一的共享状态写入
 * 3. SelectionPersistPublisher.publish：写入选课记录                 —— 同步 / 线程池 / MQ，三种模式可切换
 * 4. 响应：同步模式返回 SUCCESS，异步模式返回 PENDING（前端稍后刷新列表）
 * </pre>
 * 整个方法刻意不加 @Transactional：Redis 与 MQ 操作不该被包在数据库事务里，
 * 否则外部系统调用的耗时会一直占着数据库连接。请求线程只在"校验 / 扣减 / 落库"的瞬间持有连接，
 * 等待任何东西（缓存加载、Redis、MQ）的时候手里都没有连接——这是连接池不死锁的前提。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelectionService {

    private final SelectionLocalCache localCache;
    private final StockService stockService;
    private final DbStockService dbStockService;
    private final SelectionPersistPublisher publisher;
    private final SelectionPersistHandler handler;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final CourseSelectionMapper selectionMapper;
    private final SelectionMetrics metrics;
    private final SelectionProperties properties;

    public SelectionResultVO select(Long userId, Long courseId) {
        Long studentId = localCache.studentIdOf(userId, studentMapper);
        CourseStockView course = localCache.courseOf(courseId, courseMapper);
        if (course.isMissing()) {
            metrics.reserve(ReserveResult.NOT_FOUND);
            throw new ResourceNotFoundException("课程不存在");
        }
        if (!course.isOpenNow()) {
            metrics.reserve(ReserveResult.NOT_OPEN);
            throw new ConflictException("不在选课时间内");
        }

        ReserveResult result = reserveWithFallback(courseId, studentId);
        metrics.reserve(result);
        switch (result) {
            case NOT_FOUND -> throw new ResourceNotFoundException("课程不存在");
            case DUPLICATE -> throw new ConflictException("已选择该课程");
            case SOLD_OUT -> throw new ConflictException("课程名额已满");
            default -> { }
        }

        SelectionCommand command = SelectionCommand.of(studentId, courseId);
        boolean async = publisher.isAsync();
        try {
            publisher.publish(command);
        } catch (PublishUnavailableException e) {
            // 消息没交出去：请求线程自己把记录写了，保证"扣了名额就一定有记录"
            metrics.publishFailed();
            async = false;
            try {
                handler.handle(command);
            } catch (RuntimeException ex) {
                throw new SelectionPersistException(ex);
            }
        }
        return new SelectionResultVO(courseId, async ? SelectionResultVO.PENDING : SelectionResultVO.SUCCESS,
                remainingQuietly(courseId));
    }

    /** Redis 不可用时退回数据库原子扣减：牺牲吞吐换取可用性 */
    private ReserveResult reserveWithFallback(Long courseId, Long studentId) {
        if (properties.getStock() != SelectionProperties.StockMode.REDIS) {
            return stockService.reserve(courseId, studentId);
        }
        try {
            return stockService.reserve(courseId, studentId);
        } catch (StockUnavailableException e) {
            log.warn("Redis 不可用，选课降级为数据库原子扣减 courseId={}", courseId, e);
            metrics.degraded();
            return dbStockService.reserve(courseId, studentId);
        }
    }

    private Integer remainingQuietly(Long courseId) {
        try {
            return stockService.remaining(courseId);
        } catch (DataAccessException e) {
            return null;
        }
    }

    /**
     * 退选：数据库里把记录标记为"退选"并归还镜像名额，事务提交后再释放 Redis 预占。
     * 顺序很重要——如果先释放 Redis 再写库，数据库回滚时名额就多出来了。
     * <p>
     * 这里直接用事务连接查 studentId，不经过本地缓存：事务线程已经占着一个连接，
     * 再去等加载线程借第二个连接，连接池一满就是互相等待。
     */
    @Transactional
    public void drop(Long userId, Long courseId) {
        Long studentId = studentMapper.selectByUserId(userId)
                .map(Student::getId)
                .orElseThrow(() -> new ResourceNotFoundException("学生信息不存在"));
        CourseSelection selection = selectionMapper
                .selectByStudentAndCourse(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("未选择该课程"));
        if (selection.getStatus() != SelectionStatus.ENROLLED) {
            throw new ConflictException("该课程已退选");
        }
        selection.setStatus(SelectionStatus.DROPPED);
        selectionMapper.updateById(selection);
        courseMapper.releaseSeat(courseId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    stockService.releaseForDrop(courseId, studentId);
                } catch (RuntimeException e) {
                    log.error("退选后释放 Redis 名额失败，请调用刷新接口对账 courseId={}", courseId, e);
                }
            }
        });
    }
}
