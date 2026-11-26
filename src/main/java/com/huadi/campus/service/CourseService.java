package com.huadi.campus.service;

import com.huadi.campus.entity.Course;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.repository.CourseRepository;
import com.huadi.campus.vo.CourseVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** 课程目录查询（门户公开）与课程实体读取 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    /** 动态筛选：学期精确匹配，关键字对课程名 / 课程代码模糊匹配（忽略大小写） */
    public List<CourseVO> search(String semester, String keyword) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(semester)) {
                predicates.add(cb.equal(root.get("semester"), semester.trim()));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return courseRepository.findAll(spec, Sort.by("id")).stream()
                .map(course -> CourseVO.from(loadTeacher(course)))
                .toList();
    }

    public CourseVO getById(Long id) {
        return CourseVO.from(loadTeacher(find(id)));
    }

    public Course find(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("课程不存在"));
    }

    /** 在事务内初始化 教师 -> 用户，供 VO 拍平使用 */
    private Course loadTeacher(Course course) {
        if (course.getTeacher() != null) {
            course.getTeacher().getUser().getRealName();
        }
        return course;
    }
}
