package com.huadi.campus.service;

import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.dto.request.GradeRequest;
import com.huadi.campus.entity.Course;
import com.huadi.campus.entity.Grade;
import com.huadi.campus.entity.Student;
import com.huadi.campus.entity.Teacher;
import com.huadi.campus.entity.User;
import com.huadi.campus.enums.GradeLevel;
import com.huadi.campus.enums.GradeStatus;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ForbiddenException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.CourseSelectionMapper;
import com.huadi.campus.mapper.GradeMapper;
import com.huadi.campus.mapper.StudentMapper;
import com.huadi.campus.mapper.TeacherMapper;
import com.huadi.campus.mapper.UserMapper;
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

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final GradeMapper gradeMapper;
    private final StudentMapper studentMapper;

    public TeacherVO getProfile(Long userId) {
        TeacherVO vo = teacherMapper.selectTeacherVOByUserId(userId);
        if (vo == null) {
            throw new ResourceNotFoundException("教师信息不存在");
        }
        return vo;
    }

    @Transactional
    public TeacherVO updateContact(Long userId, ContactUpdateRequest request) {
        Teacher teacher = findByUserId(userId);
        User user = userMapper.selectById(teacher.getUserId());
        if (StringUtils.hasText(request.phone())) {
            user.setPhone(request.phone());
        }
        if (StringUtils.hasText(request.email())) {
            user.setEmail(request.email());
        }
        userMapper.updateById(user);
        return getProfile(userId);
    }

    public List<CourseVO> getMyCourses(Long userId) {
        return courseMapper.selectCourseVOList(findByUserId(userId).getId(), null, null);
    }

    public List<CourseSelectionVO> getCourseStudents(Long userId, Long courseId) {
        Course course = requireOwnedCourse(userId, courseId);
        return courseSelectionMapper.selectVOByCourse(course.getId(), SelectionStatus.ENROLLED);
    }

    public List<GradeVO> getCourseGrades(Long userId, Long courseId) {
        Course course = requireOwnedCourse(userId, courseId);
        return gradeMapper.selectVOByCourse(course.getId());
    }

    /**
     * 录入 / 更新成绩：同一学生同一课程同一学期只保留一条记录。
     * 校验：课程归属当前教师；学生确实选过这门课；分数 0-100。
     */
    @Transactional
    public GradeVO saveGrade(Long userId, Long courseId, GradeRequest request) {
        Course course = requireOwnedCourse(userId, courseId);

        Student student = studentMapper.selectById(request.studentId());
        if (student == null) {
            throw new ResourceNotFoundException("学生不存在");
        }
        if (!courseSelectionMapper.existsByStudentAndCourse(student.getId(), course.getId())) {
            throw new BadRequestException("该学生未选此课程，无法录入成绩");
        }

        Grade grade = gradeMapper
                .selectByStudentCourseSemester(student.getId(), course.getId(), request.semester())
                .orElseGet(Grade::new);
        grade.setStudentId(student.getId());
        grade.setCourseId(course.getId());
        grade.setSemester(request.semester());
        grade.setScore(request.score());
        grade.setGradeLevel(GradeLevel.fromScore(request.score()));
        grade.setCredits(course.getCredits());
        grade.setStatus(GradeStatus.NORMAL);
        if (grade.getId() == null) {
            gradeMapper.insert(grade);
        } else {
            gradeMapper.updateById(grade);
        }
        return gradeMapper.selectVOById(grade.getId());
    }

    /** 只能操作本人授课的课程 */
    private Course requireOwnedCourse(Long userId, Long courseId) {
        Teacher teacher = findByUserId(userId);
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("课程不存在");
        }
        if (course.getTeacherId() == null || !teacher.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("只能操作本人授课的课程");
        }
        return course;
    }

    private Teacher findByUserId(Long userId) {
        return teacherMapper.selectByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("教师信息不存在"));
    }
}
