package com.huadi.campus.controller;

import com.huadi.campus.common.Result;
import com.huadi.campus.dto.request.AnnouncementRequest;
import com.huadi.campus.dto.request.NewsRequest;
import com.huadi.campus.security.UserPrincipal;
import com.huadi.campus.service.AnnouncementService;
import com.huadi.campus.service.NewsService;
import com.huadi.campus.vo.AnnouncementVO;
import com.huadi.campus.vo.NewsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 内容管理：仅 ADMIN。与门户只读接口分离，避免匿名用户可写内容。 */
@Tag(name = "内容管理（管理员）")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final AnnouncementService announcementService;
    private final NewsService newsService;

    @Operation(summary = "公告全量列表")
    @GetMapping("/announcements")
    public Result<List<AnnouncementVO>> listAnnouncements() {
        return Result.success(announcementService.listAll());
    }

    @Operation(summary = "发布公告")
    @PostMapping("/announcements")
    public Result<AnnouncementVO> createAnnouncement(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody AnnouncementRequest request) {
        return Result.success(announcementService.create(principal.id(), request));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }

    @Operation(summary = "新闻全量列表")
    @GetMapping("/news")
    public Result<List<NewsVO>> listNews() {
        return Result.success(newsService.listAll());
    }

    @Operation(summary = "发布新闻")
    @PostMapping("/news")
    public Result<NewsVO> createNews(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody NewsRequest request) {
        return Result.success(newsService.create(principal.id(), request));
    }

    @Operation(summary = "删除新闻")
    @DeleteMapping("/news/{id}")
    public Result<Void> deleteNews(@PathVariable Long id) {
        newsService.delete(id);
        return Result.success();
    }
}
