package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.service.LeaderService;
import com.huadi.campus.vo.CourseStatsVO;
import com.huadi.campus.vo.StudentStatsVO;
import com.huadi.campus.vo.StudentVO;
import com.huadi.campus.vo.TeacherStatsVO;
import com.huadi.campus.vo.TeacherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 领导端：ADMIN 也可访问（管理后台首页复用这些统计） */
@Tag(name = "领导端", description = "统计与学生 / 教师名册")
@RestController
@RequestMapping("/api/leader")
@RequiredArgsConstructor
public class LeaderController {

    private final LeaderService leaderService;

    @Operation(summary = "学生统计")
    @GetMapping("/statistics/students")
    public Result<StudentStatsVO> getStudentStatistics() {
        return Result.success(leaderService.getStudentStatistics());
    }

    @Operation(summary = "教师统计")
    @GetMapping("/statistics/teachers")
    public Result<TeacherStatsVO> getTeacherStatistics() {
        return Result.success(leaderService.getTeacherStatistics());
    }

    @Operation(summary = "课程与选课统计")
    @GetMapping("/statistics/courses")
    public Result<CourseStatsVO> getCourseStatistics() {
        return Result.success(leaderService.getCourseStatistics());
    }

    @Operation(summary = "学生名册")
    @GetMapping("/students")
    public Result<List<StudentVO>> getAllStudents() {
        return Result.success(leaderService.getAllStudents());
    }

    @Operation(summary = "教师名册")
    @GetMapping("/teachers")
    public Result<List<TeacherVO>> getAllTeachers() {
        return Result.success(leaderService.getAllTeachers());
    }
}
