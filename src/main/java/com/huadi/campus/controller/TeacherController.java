package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.dto.request.GradeRequest;
import com.huadi.campus.security.UserPrincipal;
import com.huadi.campus.service.TeacherService;
import com.huadi.campus.vo.CourseSelectionVO;
import com.huadi.campus.vo.CourseVO;
import com.huadi.campus.vo.GradeVO;
import com.huadi.campus.vo.TeacherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "教师端", description = "个人信息 / 授课 / 成绩录入")
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "我的个人信息")
    @GetMapping("/profile")
    public Result<TeacherVO> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(teacherService.getProfile(principal.id()));
    }

    @Operation(summary = "修改联系方式")
    @PutMapping("/profile/contact")
    public Result<TeacherVO> updateContact(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody ContactUpdateRequest request) {
        return Result.success(teacherService.updateContact(principal.id(), request));
    }

    @Operation(summary = "我的授课")
    @GetMapping("/courses")
    public Result<List<CourseVO>> getMyCourses(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(teacherService.getMyCourses(principal.id()));
    }

    @Operation(summary = "某门课的选课学生")
    @GetMapping("/courses/{courseId}/students")
    public Result<List<CourseSelectionVO>> getCourseStudents(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long courseId) {
        return Result.success(teacherService.getCourseStudents(principal.id(), courseId));
    }

    @Operation(summary = "某门课的成绩列表")
    @GetMapping("/courses/{courseId}/grades")
    public Result<List<GradeVO>> getCourseGrades(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable Long courseId) {
        return Result.success(teacherService.getCourseGrades(principal.id(), courseId));
    }

    @Operation(summary = "录入 / 更新成绩", description = "同一学生、课程、学期只保留一条记录")
    @PutMapping("/courses/{courseId}/grades")
    public Result<GradeVO> saveGrade(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long courseId,
                                     @Valid @RequestBody GradeRequest request) {
        return Result.success(teacherService.saveGrade(principal.id(), courseId, request));
    }
}
