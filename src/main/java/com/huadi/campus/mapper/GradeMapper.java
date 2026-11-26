package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huadi.campus.entity.Grade;
import com.huadi.campus.vo.GradeVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface GradeMapper extends BaseMapper<Grade> {

    /** 同一学生、同一门课、同一学期只保留一条成绩（录成绩时按此 upsert） */
    default Optional<Grade> selectByStudentCourseSemester(Long studentId, Long courseId, String semester) {
        return Optional.ofNullable(selectOne(Wrappers.<Grade>lambdaQuery()
                .eq(Grade::getStudentId, studentId)
                .eq(Grade::getCourseId, courseId)
                .eq(Grade::getSemester, semester)));
    }

    // ---- 联表查询（XML）：成绩 + 学生 + 课程 ----

    List<GradeVO> selectVOByStudent(@Param("studentId") Long studentId,
                                    @Param("semester") String semester);

    List<GradeVO> selectVOByCourse(@Param("courseId") Long courseId);

    GradeVO selectVOById(Long id);
}
