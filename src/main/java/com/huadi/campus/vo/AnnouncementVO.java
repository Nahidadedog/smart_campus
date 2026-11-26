package com.huadi.campus.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 公告（发布人只暴露 id 与姓名） */
@Data
public class AnnouncementVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long publisherId;
    private String publisherName;
    private Boolean top;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
