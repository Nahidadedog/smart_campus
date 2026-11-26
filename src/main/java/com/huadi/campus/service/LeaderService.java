package com.huadi.campus.service;

import com.huadi.campus.enums.SelectionStatus;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.CourseSelectionMapper;
import com.huadi.campus.mapper.StudentMapper;
import com.huadi.campus.mapper.TeacherMapper;
import com.huadi.campus.vo.CourseStatsVO;
import com.huadi.campus.vo.GroupCount;
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

/** 面向院系领导 / 管理员的统计与名册。分布统计交给 SQL GROUP BY，不把整表拉到内存里数 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderService {

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final CourseSelectionMapper courseSelectionMapper;

    public StudentStatsVO getStudentStatistics() {
        return new StudentStatsVO(
                studentMapper.selectCount(null),
                toSortedMap(studentMapper.countGroupByDepartment()),
                toSortedMap(studentMapper.countGroupByStatus()));
    }

    public TeacherStatsVO getTeacherStatistics() {
        return new TeacherStatsVO(
                teacherMapper.selectCount(null),
                toSortedMap(teacherMapper.countGroupByType()),
                toSortedMap(teacherMapper.countGroupByDepartment()));
    }

    public CourseStatsVO getCourseStatistics() {
        return new CourseStatsVO(
                courseMapper.selectCount(null),
                courseSelectionMapper.countByStatus(SelectionStatus.ENROLLED));
    }

    public List<StudentVO> getAllStudents() {
        return studentMapper.selectStudentVOList();
    }

    public List<TeacherVO> getAllTeachers() {
        return teacherMapper.selectTeacherVOList();
    }

    /** GROUP BY 结果转有序 Map，方便前端稳定展示 */
    private static Map<String, Long> toSortedMap(List<GroupCount> groups) {
        Map<String, Long> result = new TreeMap<>();
        for (GroupCount group : groups) {
            result.put(group.getName(), group.getTotal());
        }
        return result;
    }
}
