package com.huadi.campus.selection;

import com.huadi.campus.entity.Course;

/**
 * 名额预占层。把"名额在哪里扣"从业务流程里抽象出来，数据库版与 Redis 版可以互换。
 * <p>
 * 一致性约定：
 * <ul>
 *   <li>reserve 成功 = 为该学生预留了 1 个名额（但数据库里还没有选课记录）</li>
 *   <li>onPersisted 在选课记录写入数据库的同一事务内调用，用来同步数据库里的 current_students 镜像列</li>
 *   <li>releaseReservation 在落库失败 / 落库时发现重复时调用，把预留的名额还回去</li>
 *   <li>releaseForDrop 在退选事务提交后调用，释放的是已落库的那条记录对应的名额</li>
 * </ul>
 * 两个实现都保证 reserve / release 幂等：重复 release 不会多加名额。
 */
public interface StockService {

    ReserveResult reserve(Long courseId, Long studentId);

    void onPersisted(SelectionCommand command);

    void releaseReservation(SelectionCommand command);

    void releaseForDrop(Long courseId, Long studentId);

    /** 剩余名额（用于响应与监控），缓存不可用时返回 null */
    Integer remaining(Long courseId);

    /** 启动 / 手动刷新：以数据库为准重建缓存，返回刷新的课程数 */
    int warmUp();

    /**
     * 管理员改课后重建单门课程的缓存（名额上限、已选名单以数据库为准），返回 1 表示已刷新、0 表示该模式无缓存。
     * 应在选课低峰执行：MQ 里还没落库的预占不会被计入，重建后可能被多卖。
     */
    int warmUp(Long courseId);
}
