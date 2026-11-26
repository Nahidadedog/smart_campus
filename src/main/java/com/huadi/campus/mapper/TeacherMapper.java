package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huadi.campus.entity.Teacher;
import com.huadi.campus.vo.GroupCount;
import com.huadi.campus.vo.TeacherVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface TeacherMapper extends BaseMapper<Teacher> {

    default Optional<Teacher> selectByUserId(Long userId) {
        return Optional.ofNullable(selectOne(Wrappers.<Teacher>lambdaQuery().eq(Teacher::getUserId, userId)));
    }

    default boolean existsByTeacherNo(String teacherNo) {
        return selectCount(Wrappers.<Teacher>lambdaQuery().eq(Teacher::getTeacherNo, teacherNo)) > 0;
    }

    // ---- 以下为 XML / 注解 SQL：带 sys_user 的联表查询与分组统计 ----

    List<TeacherVO> selectTeacherVOList();

    TeacherVO selectTeacherVOByUserId(Long userId);

    @Select("select teacher_type as name, count(*) as total from teacher "
            + "where teacher_type is not null and teacher_type <> '' group by teacher_type")
    List<GroupCount> countGroupByType();

    @Select("select department as name, count(*) as total from teacher "
            + "where department is not null and department <> '' group by department")
    List<GroupCount> countGroupByDepartment();
}
