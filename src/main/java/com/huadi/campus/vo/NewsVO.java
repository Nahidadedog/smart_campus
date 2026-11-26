package com.huadi.campus.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 新闻（作者只暴露 id 与姓名） */
@Data
public class NewsVO {
    private Long id;
    private String title;
    private String content;
    private String coverImage;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer viewCount;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
