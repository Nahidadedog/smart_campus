package com.huadi.campus.service;

import com.huadi.campus.entity.Course;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** 课程目录查询（门户公开）与课程实体读取 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseMapper courseMapper;

    /** 动态筛选：学期精确匹配，关键字对课程名 / 课程代码模糊匹配（忽略大小写），由 XML 动态 SQL 完成 */
    public List<CourseVO> search(String semester, String keyword) {
        return courseMapper.selectCourseVOList(null,
                StringUtils.hasText(semester) ? semester.trim() : null,
                StringUtils.hasText(keyword) ? keyword.trim() : null);
    }

    public CourseVO getById(Long id) {
        CourseVO vo = courseMapper.selectCourseVOById(id);
        if (vo == null) {
            throw new ResourceNotFoundException("课程不存在");
        }
        return vo;
    }

    public Course find(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new ResourceNotFoundException("课程不存在");
        }
        return course;
    }
}
