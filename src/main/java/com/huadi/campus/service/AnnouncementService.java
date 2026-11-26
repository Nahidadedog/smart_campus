package com.huadi.campus.service;

import com.huadi.campus.dto.request.AnnouncementRequest;
import com.huadi.campus.entity.Announcement;
import com.huadi.campus.entity.User;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.AnnouncementMapper;
import com.huadi.campus.mapper.UserMapper;
import com.huadi.campus.vo.AnnouncementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private static final int PORTAL_LIMIT = 10;

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;

    /** 门户展示：按类型筛选，否则返回置顶优先的最近 10 条 */
    public List<AnnouncementVO> listForPortal(String type) {
        return announcementMapper.selectVOList(StringUtils.hasText(type) ? type : null, PORTAL_LIMIT);
    }

    public AnnouncementVO getById(Long id) {
        AnnouncementVO vo = announcementMapper.selectVOById(id);
        if (vo == null) {
            throw new ResourceNotFoundException("公告不存在");
        }
        return vo;
    }

    /** 管理后台：全量列表 */
    public List<AnnouncementVO> listAll() {
        return announcementMapper.selectVOList(null, null);
    }

    @Transactional
    public AnnouncementVO create(Long publisherId, AnnouncementRequest request) {
        if (!StringUtils.hasText(request.type())) {
            throw new BadRequestException("公告类型不能为空");
        }
        User publisher = userMapper.selectById(publisherId);
        if (publisher == null) {
            throw new ResourceNotFoundException("发布人不存在");
        }
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setType(request.type());
        announcement.setTop(request.top());
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setPublisherId(publisherId);
        announcementMapper.insert(announcement);
        return announcementMapper.selectVOById(announcement.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (announcementMapper.selectById(id) == null) {
            throw new ResourceNotFoundException("公告不存在");
        }
        announcementMapper.deleteById(id);
    }
}
