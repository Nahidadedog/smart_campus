package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.dto.request.ContactUpdateRequest;
import com.huadi.campus.ratelimit.RateLimit;
import com.huadi.campus.security.UserPrincipal;
import com.huadi.campus.service.StudentService;
import com.huadi.campus.vo.CourseSelectionVO;
import com.huadi.campus.vo.GradeVO;
import com.huadi.campus.vo.SelectionResultVO;
import com.huadi.campus.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "学生端", description = "个人信息 / 选课 / 成绩")
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "我的个人信息")
    @GetMapping("/profile")
    public Result<StudentVO> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(studentService.getProfile(principal.id()));
    }

    @Operation(summary = "修改联系方式")
    @PutMapping("/profile/contact")
    public Result<StudentVO> updateContact(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody ContactUpdateRequest request) {
        return Result.success(studentService.updateContact(principal.id(), request));
    }

    @Operation(summary = "我的选课")
    @GetMapping("/courses")
    public Result<List<CourseSelectionVO>> getMyCourses(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(studentService.getMyCourses(principal.id()));
    }

    /**
     * 选课接口是唯一的写热点：每个用户一个令牌桶，挡住脚本连点。
     * 桶参数取 app.selection.rate-limit.capacity / refill-per-second（默认容量 5、每秒补 1），
     * 限流只在 app.selection.rate-limit.enabled=true 时生效，超限返回 429。
     */
    @Operation(summary = "选课", description = "名额预占成功即返回；异步模式下 data.status=PENDING，稍后刷新我的选课")
    @RateLimit(key = "'select:' + #principal.id()")
    @PostMapping("/courses/{courseId}")
    public Result<SelectionResultVO> selectCourse(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long courseId) {
        return Result.success(studentService.selectCourse(principal.id(), courseId));
    }

    @Operation(summary = "退选")
    @DeleteMapping("/courses/{courseId}")
    public Result<Void> dropCourse(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long courseId) {
        studentService.dropCourse(principal.id(), courseId);
        return Result.success();
    }

    @Operation(summary = "我的成绩", description = "可按学期筛选，不传则返回全部")
    @GetMapping("/grades")
    public Result<List<GradeVO>> getMyGrades(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam(required = false) String semester) {
        return Result.success(studentService.getMyGrades(principal.id(), semester));
    }
}
