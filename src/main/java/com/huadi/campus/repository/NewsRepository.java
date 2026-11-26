package com.huadi.campus.repository;

import com.huadi.campus.entity.News;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("publishTime"));

    /** 门户首页：最近 10 条；EntityGraph 预抓取作者避免 N+1 */
    @EntityGraph(attributePaths = "author")
    List<News> findTop10ByOrderByPublishTimeDesc();

    @EntityGraph(attributePaths = "author")
    List<News> findByCategoryOrderByPublishTimeDesc(String category);

    /** 浏览量 +1，原子更新 */
    @Modifying(flushAutomatically = true)
    @Query("update News n set n.viewCount = n.viewCount + 1 where n.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
