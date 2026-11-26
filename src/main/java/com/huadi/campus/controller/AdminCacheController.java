package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.selection.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 运维接口：以数据库为准重建名额缓存（stock=redis 时有意义，db 模式返回 0）。 */
@Tag(name = "管理员端", description = "缓存运维")
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class AdminCacheController {

    private final StockService stockService;

    @Operation(summary = "刷新全部课程的名额缓存", description = "请在选课低峰、MQ 无积压时调用，返回刷新的课程数")
    @PostMapping("/courses/refresh")
    public Result<Map<String, Integer>> refreshCourseStock() {
        return Result.success(Map.of("refreshed", stockService.warmUp()));
    }
}
