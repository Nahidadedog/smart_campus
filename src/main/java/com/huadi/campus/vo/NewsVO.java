package com.huadi.campus.vo;

import com.huadi.campus.entity.News;

import java.time.LocalDateTime;

/** 新闻（作者只暴露 id 与姓名） */
public record NewsVO(
        Long id,
        String title,
        String content,
        String coverImage,
        String category,
        Long authorId,
        String authorName,
        Integer viewCount,
        LocalDateTime publishTime,
        LocalDateTime createTime
) {
    public static NewsVO from(News n) {
        var author = n.getAuthor();
        return new NewsVO(
                n.getId(), n.getTitle(), n.getContent(), n.getCoverImage(), n.getCategory(),
                author != null ? author.getId() : null,
                author != null ? author.getRealName() : null,
                n.getViewCount(), n.getPublishTime(), n.getCreateTime()
        );
    }
}
