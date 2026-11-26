package com.huadi.campus.service;

import com.huadi.campus.dto.request.NewsRequest;
import com.huadi.campus.entity.News;
import com.huadi.campus.entity.User;
import com.huadi.campus.exception.BadRequestException;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.repository.NewsRepository;
import com.huadi.campus.repository.UserRepository;
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

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;

    /** 门户展示：按分类筛选，否则返回最近 10 条 */
    public List<NewsVO> listForPortal(String category) {
        List<News> list = StringUtils.hasText(category)
                ? newsRepository.findByCategoryOrderByPublishTimeDesc(category)
                : newsRepository.findTop10ByOrderByPublishTimeDesc();
        return list.stream().map(NewsVO::from).toList();
    }

    /** 详情：浏览量原子 +1 */
    @Transactional
    public NewsVO getDetailAndCountView(Long id) {
        newsRepository.incrementViewCount(id);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("新闻不存在"));
        return NewsVO.from(news);
    }

    /** 管理后台：全量列表 */
    public List<NewsVO> listAll() {
        return newsRepository.findAll(NewsRepository.DEFAULT_SORT)
                .stream().map(NewsVO::from).toList();
    }

    @Transactional
    public NewsVO create(Long authorId, NewsRequest request) {
        if (!StringUtils.hasText(request.category())) {
            throw new BadRequestException("新闻分类不能为空");
        }
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("作者不存在"));
        News news = new News();
        news.setTitle(request.title());
        news.setContent(request.content());
        news.setCategory(request.category());
        news.setCoverImage(request.coverImage());
        news.setPublishTime(LocalDateTime.now());
        news.setAuthor(author);
        return NewsVO.from(newsRepository.save(news));
    }

    @Transactional
    public void delete(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new ResourceNotFoundException("新闻不存在");
        }
        newsRepository.deleteById(id);
    }
}
