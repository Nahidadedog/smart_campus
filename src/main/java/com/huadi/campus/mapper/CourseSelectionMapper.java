package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.vo.CourseSelectionVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface CourseSelectionMapper extends BaseMapper<CourseSelection> {

    default Optional<CourseSelection> selectByStudentAndCourse(Long studentId, Long courseId) {
        return Optional.ofNullable(selectOne(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getCourseId, courseId)));
    }

    default boolean existsByStudentAndCourse(Long studentId, Long courseId) {
        return selectCount(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getCourseId, courseId)) > 0;
    }

    /** 是否处于"已选"状态（退选过的记录不算） */
    default boolean existsEnrolled(Long studentId, Long courseId) {
        return selectCount(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getCourseId, courseId)
                .eq(CourseSelection::getStatus, SelectionStatus.ENROLLED)) > 0;
    }

    /** 某门课所有"已选"学生的 id，用于重建 Redis 名额缓存（以数据库为准） */
    @Select("select student_id from course_selection where course_id = #{courseId} and status = 'ENROLLED'")
    List<Long> listEnrolledStudentIds(@Param("courseId") Long courseId);

    default long countByStatus(SelectionStatus status) {
        return selectCount(Wrappers.<CourseSelection>lambdaQuery().eq(CourseSelection::getStatus, status));
    }

    // ---- 联表查询（XML）：选课记录 + 学生 + 课程（含教师姓名） ----

    /** 某个学生处于指定状态的选课，按选课时间倒序 */
    List<CourseSelectionVO> selectVOByStudent(@Param("studentId") Long studentId,
                                              @Param("status") SelectionStatus status);

    /** 某门课处于指定状态的选课学生，按学号排序 */
    List<CourseSelectionVO> selectVOByCourse(@Param("courseId") Long courseId,
                                             @Param("status") SelectionStatus status);
}
