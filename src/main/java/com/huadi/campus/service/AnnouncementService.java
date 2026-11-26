package com.huadi.campus.service;

import com.huadi.campus.dto.request.AnnouncementRequest;
import com.huadi.campus.entity.Announcement;
import com.huadi.campus.entity.User;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.repository.AnnouncementRepository;
import com.huadi.campus.repository.UserRepository;
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

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    /** 门户展示：按类型筛选，否则返回置顶优先的最近 10 条 */
    public List<AnnouncementVO> listForPortal(String type) {
        List<Announcement> list = StringUtils.hasText(type)
                ? announcementRepository.findByTypeOrderByTopDescPublishTimeDesc(type)
                : announcementRepository.findTop10ByOrderByTopDescPublishTimeDesc();
        return list.stream().map(AnnouncementVO::from).toList();
    }

    public AnnouncementVO getById(Long id) {
        return AnnouncementVO.from(find(id));
    }

    /** 管理后台：全量列表 */
    public List<AnnouncementVO> listAll() {
        return announcementRepository.findAll(AnnouncementRepository.DEFAULT_SORT)
                .stream().map(AnnouncementVO::from).toList();
    }

    @Transactional
    public AnnouncementVO create(Long publisherId, AnnouncementRequest request) {
        if (!StringUtils.hasText(request.type())) {
            throw new BadRequestException("公告类型不能为空");
        }
        User publisher = userRepository.findById(publisherId)
                .orElseThrow(() -> new ResourceNotFoundException("发布人不存在"));
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setType(request.type());
        announcement.setTop(request.top());
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setPublisher(publisher);
        return AnnouncementVO.from(announcementRepository.save(announcement));
    }

    @Transactional
    public void delete(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("公告不存在");
        }
        announcementRepository.deleteById(id);
    }

    private Announcement find(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告不存在"));
        // 在事务内触发加载发布人，VO 拍平时才不会触发 LazyInitializationException
        a.getPublisher().getRealName();
        return a;
    }
}
