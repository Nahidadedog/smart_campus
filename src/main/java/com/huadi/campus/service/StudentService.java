package com.huadi.campus.service;

import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.entity.Student;
import com.huadi.campus.entity.User;
import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseSelectionMapper;
import com.huadi.campus.mapper.GradeMapper;
import com.huadi.campus.mapper.StudentMapper;
import com.huadi.campus.mapper.UserMapper;
import com.huadi.campus.selection.SelectionService;
import com.huadi.campus.vo.CourseSelectionVO;
import com.huadi.campus.vo.GradeVO;
import com.huadi.campus.vo.SelectionResultVO;
import com.huadi.campus.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 注意：这个类刻意没有类级 @Transactional(readOnly = true)。
 * <p>
 * 只读事务不是免费的：事务期间借到的数据库连接会一直绑在当前线程上，直到方法返回才归还。
 * 选课接口里真正需要连接的只有一条 UPDATE 和一条 INSERT（各几毫秒），如果整个方法包在事务里，
 * 每个请求都会在缓存、Redis、MQ 的耗时里一直占着连接，10 个连接的池子瞬间见底。
 * <p>
 * 更隐蔽的是 {@code @Transactional(propagation = NOT_SUPPORTED)}：它只是"不开真实事务"，
 * 默认仍然会开启事务同步（SYNCHRONIZATION_ALWAYS），MyBatis 借到的连接照样被绑定到方法结束，
 * 而且内部 REQUIRED 的写库方法还会再借第二个连接。压测时它和 Caffeine 的桶锁一起形成了
 * 连接池死锁——排查过程见 README「压测踩坑」。
 * <p>
 * 所以：单条查询不开事务；多步写操作（updateContact）才加 @Transactional。
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final CourseSelectionMapper courseSelectionMapper;
    private final GradeMapper gradeMapper;
    private final SelectionService selectionService;

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
     * 选课：名额预占（Redis / 数据库）与落库（同步 / 线程池 / MQ）的编排都在 SelectionService，
     * 这里只负责把登录用户映射成学生并转调。不加事务：整条链路按步骤借还连接（见类注释）。
     */
    public SelectionResultVO selectCourse(Long userId, Long courseId) {
        return selectionService.select(userId, courseId);
    }

    /** 同 selectCourse：SelectionService.drop 自己开读写事务 */
    public void dropCourse(Long userId, Long courseId) {
        selectionService.drop(userId, courseId);
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
