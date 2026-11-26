package com.huadi.campus.service;

import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.entity.Course;
import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.entity.Grade;
import com.huadi.campus.entity.Student;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.exception.ConflictException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.repository.CourseRepository;
import com.huadi.campus.repository.CourseSelectionRepository;
import com.huadi.campus.repository.GradeRepository;
import com.huadi.campus.repository.StudentRepository;
import com.huadi.campus.vo.CourseSelectionVO;
import com.huadi.campus.vo.GradeVO;
import com.huadi.campus.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseSelectionRepository courseSelectionRepository;
    private final GradeRepository gradeRepository;

    public StudentVO getProfile(Long userId) {
        Student student = findByUserId(userId);
        student.getUser().getRealName(); // 事务内初始化
        return StudentVO.from(student);
    }

    @Transactional
    public StudentVO updateContact(Long userId, ContactUpdateRequest request) {
        Student student = findByUserId(userId);
        if (StringUtils.hasText(request.phone())) {
            student.getUser().setPhone(request.phone());
        }
        if (StringUtils.hasText(request.email())) {
            student.getUser().setEmail(request.email());
        }
        return StudentVO.from(studentRepository.save(student));
    }

    public List<CourseSelectionVO> getMyCourses(Long userId) {
        return courseSelectionRepository
                .findActiveByStudent(findByUserId(userId).getId(), SelectionStatus.ENROLLED)
                .stream().map(CourseSelectionVO::from).toList();
    }

    public List<GradeVO> getMyGrades(Long userId, String semester) {
        List<Grade> grades = gradeRepository.findByStudentIdWithCourse(findByUserId(userId).getId());
        return grades.stream()
                .filter(g -> !StringUtils.hasText(semester) || semester.equals(g.getSemester()))
                .map(GradeVO::from)
                .toList();
    }

    /**
     * 选课。名额通过一条原子 UPDATE 占用，避免并发超选；
     * 退选过的课再次选择时复用原记录（唯一约束 student_id + course_id）。
     */
    @Transactional
    public CourseSelectionVO selectCourse(Long userId, Long courseId) {
        Student student = findByUserId(userId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("课程不存在"));

        CourseSelection selection = courseSelectionRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseGet(CourseSelection::new);
        // 注意：新 new 出来的实体默认状态就是 ENROLLED，必须用 id 区分"已有记录"和"首次选课"
        if (selection.getId() != null && selection.getStatus() == SelectionStatus.ENROLLED) {
            throw new ConflictException("已选择该课程");
        }
        if (courseRepository.occupySeat(courseId) == 0) {
            throw new ConflictException("课程名额已满");
        }
        // 同步内存态与原子更新后的数据库值，保证返回给前端的 currentStudents 正确
        course.setCurrentStudents(course.getCurrentStudents() + 1);

        selection.setStudent(student);
        selection.setCourse(course);
        selection.setStatus(SelectionStatus.ENROLLED);
        return CourseSelectionVO.from(courseSelectionRepository.save(selection));
    }

    @Transactional
    public void dropCourse(Long userId, Long courseId) {
        Student student = findByUserId(userId);
        CourseSelection selection = courseSelectionRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("未选择该课程"));
        if (selection.getStatus() != SelectionStatus.ENROLLED) {
            throw new ConflictException("该课程已退选");
        }
        selection.setStatus(SelectionStatus.DROPPED);
        courseRepository.releaseSeat(courseId);
    }

    private Student findByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("学生信息不存在"));
    }
}
