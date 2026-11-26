package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.vo.CourseSelectionVO;
import org.apache.ibatis.annotations.Param;

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
