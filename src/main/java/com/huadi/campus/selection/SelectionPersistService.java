package com.huadi.campus.selection;

import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.mapper.CourseSelectionMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 把一条预占成功的选课指令写进数据库。无论走同步、线程池还是 MQ，最终都调用这里。
 * <p>
 * 幂等性：student_id + course_id 唯一约束是最后一道防线。消费者可能收到重复消息，
 * 两个请求也可能同时落库——这里只负责"写或抛"，由 SelectionPersistHandler 把
 * {@link org.springframework.dao.DuplicateKeyException} 翻译成 DUPLICATE 并归还名额。
 * <p>
 * 加锁顺序（MySQL/InnoDB）：必须先 UPDATE course 行拿 X 锁，再 INSERT 选课记录。
 * 反过来的话，INSERT 的外键检查会对 course 行加 S 锁，两个事务各持 S 锁再互相等 X 锁 → 死锁。
 * 这个死锁只有在真实 MySQL 上并发消费时才会出现（k6 压测时 50 条消息里死锁了 1 条）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelectionPersistService {

    private final CourseSelectionMapper selectionMapper;
    private final StockService stockService;
    private final MeterRegistry registry;

    private Timer persistTimer;

    @PostConstruct
    void initMetrics() {
        persistTimer = Timer.builder("campus.selection.persist")
                .description("选课记录落库耗时")
                .register(registry);
    }

    /**
     * @return PERSISTED 写入成功；DUPLICATE 已经是已选状态，本次没有写入
     * @throws org.springframework.dao.DuplicateKeyException 并发落库被唯一约束拦截（事务已回滚）
     */
    @Transactional
    public PersistOutcome persist(SelectionCommand command) {
        Timer.Sample sample = Timer.start(registry);
        try {
            CourseSelection selection = selectionMapper
                    .selectByStudentAndCourse(command.studentId(), command.courseId())
                    .orElseGet(CourseSelection::new);

            // 必须用 id 区分"已有记录"与"首次选课"，不能用 status（新对象没有状态）
            if (selection.getId() != null && selection.getStatus() == SelectionStatus.ENROLLED) {
                return PersistOutcome.DUPLICATE;
            }

            // 1) 先锁课程行（Redis 模式下顺便维护数据库镜像列 current_students）
            stockService.onPersisted(command);

            // 2) 再写选课记录：外键检查需要的 S 锁已被本事务的 X 锁覆盖，不会与其它消费者互相等待
            selection.setStudentId(command.studentId());
            selection.setCourseId(command.courseId());
            selection.setStatus(SelectionStatus.ENROLLED);
            if (selection.getId() == null) {
                selectionMapper.insert(selection);
            } else {
                // 退选过的课再次选择：复用原记录，不触发唯一约束
                selectionMapper.updateById(selection);
            }
            return PersistOutcome.PERSISTED;
        } finally {
            sample.stop(persistTimer);
        }
    }
}
