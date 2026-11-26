package com.huadi.campus.vo;

import com.huadi.campus.entity.Announcement;

import java.time.LocalDateTime;

/** 公告（发布人只暴露 id 与姓名） */
public record AnnouncementVO(
        Long id,
        String title,
        String content,
        String type,
        Long publisherId,
        String publisherName,
        Boolean top,
        LocalDateTime publishTime,
        LocalDateTime createTime
) {
    public static AnnouncementVO from(Announcement a) {
        var publisher = a.getPublisher();
        return new AnnouncementVO(
                a.getId(), a.getTitle(), a.getContent(), a.getType(),
                publisher != null ? publisher.getId() : null,
                publisher != null ? publisher.getRealName() : null,
                a.getTop(), a.getPublishTime(), a.getCreateTime()
        );
    }
}
