package com.huadi.campus.service;

import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.entity.Course;
import com.huadi.campus.entity.CourseSelection;
import com.huadi.campus.entity.Student;
import com.huadi.campus.entity.User;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.exception.ConflictException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.CourseSelectionMapper;
import com.huadi.campus.mapper.GradeMapper;
import com.huadi.campus.mapper.StudentMapper;
import com.huadi.campus.mapper.UserMapper;
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

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final GradeMapper gradeMapper;

    public StudentVO getProfile(Long userId) {
        return requireStudentVO(userId);
    }

    /** 联系方式存放在登录账号表上，修改后重新联表读取 */
    @Transactional
    public StudentVO updateContact(Long userId, ContactUpdateRequest request) {
        Student student = findByUserId(userId);
        User user = userMapper.selectById(student.getUserId());
        if (StringUtils.hasText(request.phone())) {
            user.setPhone(request.phone());
        }
        if (StringUtils.hasText(request.email())) {
            user.setEmail(request.email());
        }
        userMapper.updateById(user);
        return requireStudentVO(userId);
    }

    public List<CourseSelectionVO> getMyCourses(Long userId) {
        return courseSelectionMapper.selectVOByStudent(findByUserId(userId).getId(), SelectionStatus.ENROLLED);
    }

    public List<GradeVO> getMyGrades(Long userId, String semester) {
        return gradeMapper.selectVOByStudent(findByUserId(userId).getId(),
                StringUtils.hasText(semester) ? semester : null);
    }

    /**
     * 选课。名额通过一条原子 UPDATE 占用，避免并发超选；
     * 退选过的课再次选择时复用原记录（唯一约束 student_id + course_id）。
     */
    @Transactional
    public CourseSelectionVO selectCourse(Long userId, Long courseId) {
        Student student = findByUserId(userId);
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("课程不存在");
        }

        CourseSelection selection = courseSelectionMapper
                .selectByStudentAndCourse(student.getId(), courseId)
                .orElseGet(CourseSelection::new);
        // 注意：必须用 id 区分"已有记录"和"首次选课"
        if (selection.getId() != null && selection.getStatus() == SelectionStatus.ENROLLED) {
            throw new ConflictException("已选择该课程");
        }
        if (courseMapper.occupySeat(courseId) == 0) {
            throw new ConflictException("课程名额已满");
        }

        selection.setStudentId(student.getId());
        selection.setCourseId(courseId);
        selection.setStatus(SelectionStatus.ENROLLED);
        if (selection.getId() == null) {
            courseSelectionMapper.insert(selection);
        } else {
            courseSelectionMapper.updateById(selection);
        }

        User user = userMapper.selectById(student.getUserId());
        CourseSelectionVO vo = new CourseSelectionVO();
        vo.setId(selection.getId());
        vo.setStatus(SelectionStatus.ENROLLED);
        vo.setCreateTime(selection.getCreateTime());
        vo.setStudentId(student.getId());
        vo.setStudentNo(student.getStudentNo());
        vo.setStudentName(user.getRealName());
        // 重新联表读取，拿到数据库原子更新后的 current_students
        vo.setCourse(courseMapper.selectCourseVOById(courseId));
        return vo;
    }

    @Transactional
    public void dropCourse(Long userId, Long courseId) {
        Student student = findByUserId(userId);
        CourseSelection selection = courseSelectionMapper
                .selectByStudentAndCourse(student.getId(), courseId)
                .orElseThrow(() -> new ResourceNotFoundException("未选择该课程"));
        if (selection.getStatus() != SelectionStatus.ENROLLED) {
            throw new ConflictException("该课程已退选");
        }
        selection.setStatus(SelectionStatus.DROPPED);
        courseSelectionMapper.updateById(selection);
        courseMapper.releaseSeat(courseId);
    }

    private Student findByUserId(Long userId) {
        return studentMapper.selectByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("学生信息不存在"));
    }

    private StudentVO requireStudentVO(Long userId) {
        StudentVO vo = studentMapper.selectStudentVOByUserId(userId);
        if (vo == null) {
            throw new ResourceNotFoundException("学生信息不存在");
        }
        return vo;
    }
}
