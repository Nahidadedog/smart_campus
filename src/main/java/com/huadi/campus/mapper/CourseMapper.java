package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadi.campus.entity.Course;
import com.huadi.campus.vo.CourseVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {

    // ---- 联表查询（XML）：course -> teacher -> sys_user ----

    /**
     * 课程目录。三个条件均可选：按教师、按学期、关键字匹配课程名 / 课程代码（忽略大小写）
     */
    List<CourseVO> selectCourseVOList(@Param("teacherId") Long teacherId,
                                      @Param("semester") String semester,
                                      @Param("keyword") String keyword);

    CourseVO selectCourseVOById(Long id);

    // ---- 原子更新：选课名额的占用 / 释放 ----

    /**
     * 原子占用一个名额：仅当 current_students < max_students 时 +1。
     * 返回受影响行数，0 表示已满。用于避免「先查后改」的并发超选。
     */
    @Update("update course set current_students = current_students + 1 "
            + "where id = #{id} and current_students < max_students")
    int occupySeat(@Param("id") Long id);

    /** 原子释放一个名额（不低于 0） */
    @Update("update course set current_students = current_students - 1 "
            + "where id = #{id} and current_students > 0")
    int releaseSeat(@Param("id") Long id);
}
