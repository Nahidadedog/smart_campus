package com.huadi.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huadi.campus.entity.Announcement;
import com.huadi.campus.vo.AnnouncementVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 公告列表，置顶优先、发布时间倒序。
     *
     * @param type  按类型筛选，可空
     * @param limit 最多返回条数，null 表示不限制
     */
    List<AnnouncementVO> selectVOList(@Param("type") String type, @Param("limit") Integer limit);

    AnnouncementVO selectVOById(Long id);
}
