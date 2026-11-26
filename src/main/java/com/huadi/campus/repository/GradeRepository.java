package com.huadi.campus.repository;

import com.huadi.campus.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    /** 同一学生、同一门课、同一学期只保留一条成绩（录成绩时按此 upsert） */
    Optional<Grade> findByStudentIdAndCourseIdAndSemester(Long studentId, Long courseId, String semester);

    @Query("""
            select g from Grade g
            join fetch g.course c
            left join fetch c.teacher
            where g.student.id = :studentId
            order by g.semester desc, g.id
            """)
    List<Grade> findByStudentIdWithCourse(@Param("studentId") Long studentId);

    @Query("""
            select g from Grade g
            join fetch g.student s
            join fetch s.user
            join fetch g.course
            where g.course.id = :courseId
            order by g.semester desc, s.studentNo
            """)
    List<Grade> findByCourseIdWithStudent(@Param("courseId") Long courseId);
}
