package com.huadi.campus.service;

import com.huadi.campus.dto.request.NewsRequest;
import com.huadi.campus.entity.News;
import com.huadi.campus.entity.User;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.NewsMapper;
import com.huadi.campus.mapper.UserMapper;
import com.huadi.campus.vo.NewsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private static final int PORTAL_LIMIT = 10;

    private final NewsMapper newsMapper;
    private final UserMapper userMapper;

    /** 门户展示：按分类筛选，否则返回最近 10 条 */
    public List<NewsVO> listForPortal(String category) {
        return newsMapper.selectVOList(StringUtils.hasText(category) ? category : null, PORTAL_LIMIT);
    }

    /** 详情：浏览量原子 +1 */
    @Transactional
    public NewsVO getDetailAndCountView(Long id) {
        newsMapper.incrementViewCount(id);
        NewsVO vo = newsMapper.selectVOById(id);
        if (vo == null) {
            throw new ResourceNotFoundException("新闻不存在");
        }
        return vo;
    }

    /** 管理后台：全量列表 */
    public List<NewsVO> listAll() {
        return newsMapper.selectVOList(null, null);
    }

    @Transactional
    public NewsVO create(Long authorId, NewsRequest request) {
        if (!StringUtils.hasText(request.category())) {
            throw new BadRequestException("新闻分类不能为空");
        }
        User author = userMapper.selectById(authorId);
        if (author == null) {
            throw new ResourceNotFoundException("作者不存在");
        }
        News news = new News();
        news.setTitle(request.title());
        news.setContent(request.content());
        news.setCategory(request.category());
        news.setCoverImage(request.coverImage());
        news.setPublishTime(LocalDateTime.now());
        news.setAuthorId(authorId);
        newsMapper.insert(news);
        return newsMapper.selectVOById(news.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (newsMapper.selectById(id) == null) {
            throw new ResourceNotFoundException("新闻不存在");
        }
        newsMapper.deleteById(id);
    }
}
