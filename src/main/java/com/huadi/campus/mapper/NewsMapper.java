package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadi.campus.entity.News;
import com.huadi.campus.vo.NewsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface NewsMapper extends BaseMapper<News> {

    /**
     * 新闻列表，发布时间倒序。
     *
     * @param category 按分类筛选，可空
     * @param limit    最多返回条数，null 表示不限制
     */
    List<NewsVO> selectVOList(@Param("category") String category, @Param("limit") Integer limit);

    NewsVO selectVOById(Long id);

    /** 浏览量 +1，原子更新 */
    @Update("update news set view_count = view_count + 1 where id = #{id}")
    int incrementViewCount(@Param("id") Long id);
}
