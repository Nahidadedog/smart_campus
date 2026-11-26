package com.huadi.campus.repository;

import com.huadi.campus.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByStudentNo(String studentNo);

    boolean existsByStudentNo(String studentNo);
}
