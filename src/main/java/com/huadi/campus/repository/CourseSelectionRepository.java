package com.huadi.campus.repository;

import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.enums.SelectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseSelectionRepository extends JpaRepository<CourseSelection, Long> {

    Optional<CourseSelection> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByStatus(SelectionStatus status);

    /** 学生的有效选课，预抓取课程 -> 教师 -> 用户，避免 N+1 */
    @Query("""
            select s from CourseSelection s
            join fetch s.course c
            join fetch c.teacher t
            join fetch t.user
            where s.student.id = :studentId and s.status = :status
            order by s.createTime desc
            """)
    List<CourseSelection> findActiveByStudent(@Param("studentId") Long studentId,
                                              @Param("status") SelectionStatus status);

    /** 某门课的有效选课学生，预抓取学生 -> 用户 */
    @Query("""
            select s from CourseSelection s
            join fetch s.student st
            join fetch st.user
            where s.course.id = :courseId and s.status = :status
            order by st.studentNo
            """)
    List<CourseSelection> findActiveByCourse(@Param("courseId") Long courseId,
                                             @Param("status") SelectionStatus status);
}
