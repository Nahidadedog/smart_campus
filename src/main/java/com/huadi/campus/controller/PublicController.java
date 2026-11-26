package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.service.AnnouncementService;
import com.huadi.campus.service.CourseService;
import com.huadi.campus.service.NewsService;
import com.huadi.campus.vo.AnnouncementVO;
import com.huadi.campus.vo.CourseVO;
import com.huadi.campus.vo.NewsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 匿名可读的门户内容：公告、新闻、课程目录。写操作在 AdminContentController。 */
@Tag(name = "门户公开接口", description = "无需登录")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final AnnouncementService announcementService;
    private final NewsService newsService;
    private final CourseService courseService;

    @Operation(summary = "公告列表（最近 10 条，置顶优先）")
    @GetMapping("/announcements")
    public Result<List<AnnouncementVO>> listAnnouncements(@RequestParam(required = false) String type) {
        return Result.success(announcementService.listForPortal(type));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/announcements/{id}")
    public Result<AnnouncementVO> getAnnouncement(@PathVariable Long id) {
        return Result.success(announcementService.getById(id));
    }

    @Operation(summary = "新闻列表（最近 10 条）")
    @GetMapping("/news")
    public Result<List<NewsVO>> listNews(@RequestParam(required = false) String category) {
        return Result.success(newsService.listForPortal(category));
    }

    @Operation(summary = "新闻详情（浏览量 +1）")
    @GetMapping("/news/{id}")
    public Result<NewsVO> getNews(@PathVariable Long id) {
        return Result.success(newsService.getDetailAndCountView(id));
    }

    @Operation(summary = "课程目录", description = "支持按学期、关键字（课程名 / 课程代码）筛选")
    @GetMapping("/courses")
    public Result<List<CourseVO>> listCourses(@RequestParam(required = false) String semester,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(courseService.search(semester, keyword));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/courses/{id}")
    public Result<CourseVO> getCourse(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }
}
