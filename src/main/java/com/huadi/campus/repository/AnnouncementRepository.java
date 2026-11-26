package com.huadi.campus.repository;

import com.huadi.campus.entity.Announcement;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("top"), Sort.Order.desc("publishTime"));

    /** 门户首页：置顶优先的最近 10 条；EntityGraph 预抓取发布人避免 N+1 */
    @EntityGraph(attributePaths = "publisher")
    List<Announcement> findTop10ByOrderByTopDescPublishTimeDesc();

    @EntityGraph(attributePaths = "publisher")
    List<Announcement> findByTypeOrderByTopDescPublishTimeDesc(String type);
}
