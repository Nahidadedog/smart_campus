package com.huadi.campus.service;

import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.dto.request.GradeRequest;
import com.huadi.campus.entity.Course;
import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.entity.Grade;
import com.huadi.campus.entity.Teacher;
import com.huadi.campus.enums.GradeLevel;
import com.huadi.campus.enums.GradeStatus;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ForbiddenException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.repository.CourseRepository;
import com.huadi.campus.repository.CourseSelectionRepository;
import com.huadi.campus.repository.GradeRepository;
import com.huadi.campus.repository.StudentRepository;
import com.huadi.campus.repository.TeacherRepository;
import com.huadi.campus.vo.CourseSelectionVO;
import com.huadi.campus.vo.CourseVO;
import com.huadi.campus.vo.GradeVO;
import com.huadi.campus.vo.TeacherVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final CourseSelectionRepository courseSelectionRepository;
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;

    public TeacherVO getProfile(Long userId) {
        Teacher teacher = findByUserId(userId);
        teacher.getUser().getRealName(); // 事务内初始化
        return TeacherVO.from(teacher);
    }

    @Transactional
    public TeacherVO updateContact(Long userId, ContactUpdateRequest request) {
        Teacher teacher = findByUserId(userId);
        if (StringUtils.hasText(request.phone())) {
            teacher.getUser().setPhone(request.phone());
        }
        if (StringUtils.hasText(request.email())) {
            teacher.getUser().setEmail(request.email());
        }
        return TeacherVO.from(teacherRepository.save(teacher));
    }

    public List<CourseVO> getMyCourses(Long userId) {
        return courseRepository.findByTeacherId(findByUserId(userId).getId())
                .stream().map(CourseVO::from).toList();
    }

    public List<CourseSelectionVO> getCourseStudents(Long userId, Long courseId) {
        Course course = requireOwnedCourse(userId, courseId);
        return courseSelectionRepository.findActiveByCourse(course.getId(), SelectionStatus.ENROLLED)
                .stream().map(CourseSelectionVO::from).toList();
    }

    public List<GradeVO> getCourseGrades(Long userId, Long courseId) {
        Course course = requireOwnedCourse(userId, courseId);
        return gradeRepository.findByCourseIdWithStudent(course.getId())
                .stream().map(GradeVO::from).toList();
    }

    /**
     * 录入 / 更新成绩：同一学生同一课程同一学期只保留一条记录。
     * 校验：课程归属当前教师；学生确实选过这门课；分数 0-100。
     */
    @Transactional
    public GradeVO saveGrade(Long userId, Long courseId, GradeRequest request) {
        Course course = requireOwnedCourse(userId, courseId);

        var student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("学生不存在"));
        if (!courseSelectionRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new BadRequestException("该学生未选此课程，无法录入成绩");
        }

        Grade grade = gradeRepository
                .findByStudentIdAndCourseIdAndSemester(student.getId(), course.getId(), request.semester())
                .orElseGet(Grade::new);
        grade.setStudent(student);
        grade.setCourse(course);
        grade.setSemester(request.semester());
        grade.setScore(request.score());
        grade.setGradeLevel(GradeLevel.fromScore(request.score()));
        grade.setCredits(course.getCredits());
        grade.setStatus(GradeStatus.NORMAL);
        return GradeVO.from(gradeRepository.save(grade));
    }

    /** 只能操作本人授课的课程 */
    private Course requireOwnedCourse(Long userId, Long courseId) {
        Teacher teacher = findByUserId(userId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("课程不存在"));
        if (course.getTeacher() == null || !teacher.getId().equals(course.getTeacher().getId())) {
            throw new ForbiddenException("只能操作本人授课的课程");
        }
        return course;
    }

    private Teacher findByUserId(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("教师信息不存在"));
    }
}
