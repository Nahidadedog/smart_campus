package com.huadi.campus.selection;

import com.huadi.campus.entity.Course;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.CourseSelectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 纯数据库实现：名额就是 course.current_students，用一条带条件的原子 UPDATE 占用。
 * 没有缓存层，正确性完全由数据库行锁 + 唯一约束保证，是 Redis 版的兜底与对照基线。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbStockService implements StockService {

    private final CourseMapper courseMapper;
    private final CourseSelectionMapper selectionMapper;

    @Override
    public ReserveResult reserve(Long courseId, Long studentId) {
        if (selectionMapper.existsEnrolled(studentId, courseId)) {
            return ReserveResult.DUPLICATE;
        }
        // 返回 0 可能是"已满"也可能是"课程不存在"，统一按已满处理；存在性由上层本地缓存校验
        return courseMapper.occupySeat(courseId) > 0 ? ReserveResult.ACQUIRED : ReserveResult.SOLD_OUT;
    }

    @Override
    public void onPersisted(SelectionCommand command) {
        // 名额在 reserve 时已经 +1，这里无需再动数据库
    }

    @Override
    public void releaseReservation(SelectionCommand command) {
        courseMapper.releaseSeat(command.courseId());
    }

    @Override
    public void releaseForDrop(Long courseId, Long studentId) {
        // 退选流程里 releaseSeat 已与状态更新在同一事务中执行
    }

    @Override
    public Integer remaining(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getMaxStudents() == null) {
            return null;
        }
        return Math.max(0, course.getMaxStudents() - course.getCurrentStudents());
    }

    @Override
    public int warmUp() {
        return 0;
    }

    @Override
    public int warmUp(Long courseId) {
        return 0;
    }
}
