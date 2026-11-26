package com.huadi.campus.service;

import com.huadi.campus.entity.Student;
import com.huadi.campus.entity.Teacher;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.repository.CourseRepository;
import com.huadi.campus.repository.CourseSelectionRepository;
import com.huadi.campus.repository.StudentRepository;
import com.huadi.campus.repository.TeacherRepository;
import com.huadi.campus.vo.CourseStatsVO;
import com.huadi.campus.vo.StudentStatsVO;
import com.huadi.campus.vo.StudentVO;
import com.huadi.campus.vo.TeacherStatsVO;
import com.huadi.campus.vo.TeacherVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 面向院系领导 / 管理员的统计与名册 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final CourseSelectionRepository courseSelectionRepository;

    public StudentStatsVO getStudentStatistics() {
        List<Student> students = studentRepository.findAll();
        return new StudentStatsVO(
                students.size(),
                countBy(students, Student::getDepartment),
                countBy(students, Student::getStatus));
    }

    public TeacherStatsVO getTeacherStatistics() {
        List<Teacher> teachers = teacherRepository.findAll();
        return new TeacherStatsVO(
                teachers.size(),
                countBy(teachers, Teacher::getTeacherType),
                countBy(teachers, Teacher::getDepartment));
    }

    public CourseStatsVO getCourseStatistics() {
        return new CourseStatsVO(
                courseRepository.count(),
                courseSelectionRepository.countByStatus(SelectionStatus.ENROLLED));
    }

    public List<StudentVO> getAllStudents() {
        return studentRepository.findAll().stream().map(StudentVO::from).toList();
    }

    public List<TeacherVO> getAllTeachers() {
        return teacherRepository.findAll().stream().map(TeacherVO::from).toList();
    }

    /** 按分类字段计数，结果按 key 排序，忽略空值 */
    private static <T> Map<String, Long> countBy(List<T> items, Function<T, String> classifier) {
        return items.stream()
                .map(classifier)
                .filter(key -> key != null && !key.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
    }
}
