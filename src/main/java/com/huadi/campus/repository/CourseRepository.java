package com.huadi.campus.repository;

import com.huadi.campus.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    List<Course> findByTeacherId(Long teacherId);

    /**
     * 原子占用一个名额：仅当 current_students < max_students 时 +1。
     * 返回受影响行数，0 表示已满。用于避免「先查后改」的并发超选。
     */
    @Modifying(flushAutomatically = true)
    @Query("update Course c set c.currentStudents = c.currentStudents + 1 "
            + "where c.id = :id and c.currentStudents < c.maxStudents")
    int occupySeat(@Param("id") Long id);

    /** 原子释放一个名额（不低于 0） */
    @Modifying(flushAutomatically = true)
    @Query("update Course c set c.currentStudents = c.currentStudents - 1 "
            + "where c.id = :id and c.currentStudents > 0")
    int releaseSeat(@Param("id") Long id);
}
