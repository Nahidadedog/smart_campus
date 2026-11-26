package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huadi.campus.entity.Student;
import com.huadi.campus.vo.GroupCount;
import com.huadi.campus.vo.StudentVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface StudentMapper extends BaseMapper<Student> {

    default Optional<Student> selectByUserId(Long userId) {
        return Optional.ofNullable(selectOne(Wrappers.<Student>lambdaQuery().eq(Student::getUserId, userId)));
    }

    default boolean existsByStudentNo(String studentNo) {
        return selectCount(Wrappers.<Student>lambdaQuery().eq(Student::getStudentNo, studentNo)) > 0;
    }

    // ---- 以下为 XML / 注解 SQL：带 sys_user 的联表查询与分组统计 ----

    List<StudentVO> selectStudentVOList();

    StudentVO selectStudentVOByUserId(Long userId);

    @Select("select department as name, count(*) as total from student "
            + "where department is not null and department <> '' group by department")
    List<GroupCount> countGroupByDepartment();

    @Select("select status as name, count(*) as total from student "
            + "where status is not null and status <> '' group by status")
    List<GroupCount> countGroupByStatus();
}
