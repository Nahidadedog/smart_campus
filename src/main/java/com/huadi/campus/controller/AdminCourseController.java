package com.huadi.campus.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huadi.campus.common.Result;
import com.huadi.campus.dto.request.CourseRequest;
import com.huadi.campus.entity.Course;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ConflictException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.selection.SelectionLocalCache;
import com.huadi.campus.selection.StockService;
import com.huadi.campus.service.CourseService;
import com.huadi.campus.vo.CourseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程维护：目前只有创建与编辑，供管理员开课 / 压测脚本准备数据。
 * 改完课程要同步两层缓存：本地课程快照（选课窗口）与 Redis 名额（名额上限）。
 */
@Tag(name = "管理员端", description = "课程维护")
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseMapper courseMapper;
    private final CourseService courseService;
    private final SelectionLocalCache localCache;
    private final StockService stockService;

    @Operation(summary = "创建课程")
    @PostMapping
    @Transactional
    public Result<CourseVO> create(@Valid @RequestBody CourseRequest request) {
        if (courseMapper.existsByCode(request.code())) {
            throw new ConflictException("课程代码已存在");
        }
        Course course = new Course();
        apply(course, request);
        course.setCurrentStudents(0);
        courseMapper.insert(course);
        refreshCachesAfterCommit(course.getId());
        return Result.success(courseService.getById(course.getId()));
    }

    @Operation(summary = "编辑课程", description = "可调整名额上限与选课时间窗口；已有选课记录时名额不能低于已选人数。应在选课低峰执行")
    @PutMapping("/{id}")
    @Transactional
    public Result<CourseVO> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new ResourceNotFoundException("课程不存在");
        }
        if (course.getCurrentStudents() != null && request.maxStudents() < course.getCurrentStudents()) {
            throw new BadRequestException("名额上限不能低于已选人数");
        }
        if (!course.getCode().equals(request.code()) && courseMapper.existsByCode(request.code())) {
            throw new ConflictException("课程代码已存在");
        }
        apply(course, request);
        // updateById 默认跳过 null 字段，"把窗口清空 = 不限制"会写不进去；这两列必须显式 SET（含 NULL）
        courseMapper.update(course, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .set(Course::getEnrollStartTime, request.enrollStartTime())
                .set(Course::getEnrollEndTime, request.enrollEndTime()));
        refreshCachesAfterCommit(id);
        return Result.success(courseService.getById(id));
    }

    /**
     * 等数据库事务提交后再动缓存：提交前失效，并发请求会把旧值重新填回去；
     * 提交前重建 Redis 名额，一旦回滚 Redis 里就是错的。
     */
    private void refreshCachesAfterCommit(Long courseId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 已经在 afterCommit 回调里，直接失效即可；再注册一次同步不会被触发
                localCache.evictCourse(courseId);
                stockService.warmUp(courseId);
            }
        });
    }

    private static void apply(Course course, CourseRequest request) {
        course.setName(request.name());
        course.setCode(request.code());
        course.setCredits(request.credits());
        course.setHours(request.hours());
        course.setType(request.type());
        course.setDescription(request.description());
        course.setTeacherId(request.teacherId());
        course.setSemester(request.semester());
        course.setSchedule(request.schedule());
        course.setClassroom(request.classroom());
        course.setMaxStudents(request.maxStudents());
        course.setEnrollStartTime(request.enrollStartTime());
        course.setEnrollEndTime(request.enrollEndTime());
    }
}
