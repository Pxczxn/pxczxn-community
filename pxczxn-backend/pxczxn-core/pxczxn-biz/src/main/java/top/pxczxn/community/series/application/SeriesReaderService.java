package top.pxczxn.community.series.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.series.model.Series;
import top.pxczxn.community.series.model.SeriesArticle;
import top.pxczxn.community.series.model.SeriesReadingProgress;
import top.pxczxn.community.series.persistence.SeriesArticleMapper;
import top.pxczxn.community.series.persistence.SeriesMapper;
import top.pxczxn.community.series.persistence.SeriesReadingProgressMapper;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Reader-side operations on a published series: following it for updates, and remembering
 * how far the reader got. Deliberately separate from {@link SeriesService}, which owns
 * authoring and review. Both personal and team series are handled identically here —
 * a reader never cares which kind of blog a series belongs to.
 */
@Service
@RequiredArgsConstructor
public class SeriesReaderService {

    private static final String TARGET_SERIES = "SERIES";

    private final SeriesMapper seriesMapper;
    private final SeriesArticleMapper chapterMapper;
    private final SeriesReadingProgressMapper progressMapper;
    private final CommunityFollowMapper followMapper;
    private final ArticleMapper articleMapper;

    /** Idempotent: following an already-followed series returns the current state untouched. */
    @Transactional
    public SeriesReaderStateView follow(Long actorUserId, Long seriesId) {
        Long reader = requireReader(actorUserId);
        Series series = requirePublicSeries(seriesId);
        if (followMapper.findRelation(reader, TARGET_SERIES, series.getId()) == null) {
            CommunityFollow relation = new CommunityFollow();
            relation.setId(IdWorker.getId());
            relation.setFollowerUserId(reader);
            relation.setTargetType(TARGET_SERIES);
            relation.setTargetId(series.getId());
            relation.setNotificationLevel("ALL");
            relation.setSpecialFollow(0);
            relation.setCreatedAt(now());
            try {
                if (followMapper.insert(relation) != 1) throw new BusinessException(500, "关注系列失败");
            } catch (DuplicateKeyException ignored) {
                // A concurrent follow already created the relation; the desired state is reached either way.
            }
        }
        return stateOf(reader, series);
    }

    /** Idempotent: unfollowing a series that is not followed is a no-op. */
    @Transactional
    public SeriesReaderStateView unfollow(Long actorUserId, Long seriesId) {
        Long reader = requireReader(actorUserId);
        Series series = requirePublicSeries(seriesId);
        followMapper.deleteRelation(reader, TARGET_SERIES, series.getId());
        return stateOf(reader, series);
    }

    @Transactional(readOnly = true)
    public SeriesReaderStateView state(Long actorUserId, Long seriesId) {
        Long reader = requireReader(actorUserId);
        return stateOf(reader, requirePublicSeries(seriesId));
    }

    /**
     * Records that the reader opened one chapter. The "last read" pointer follows the reader
     * anywhere, while the furthest chapter only grows, so re-reading chapter 1 of a 20 chapter
     * series never resets the progress bar.
     */
    @Transactional
    public SeriesReaderStateView recordProgress(Long actorUserId, Long seriesId, Long articleId) {
        Long reader = requireReader(actorUserId);
        Series series = requirePublicSeries(seriesId);
        if (articleId == null) throw new BusinessException(400, "章节文章不能为空");
        SeriesArticle chapter = chapterMapper.findByArticle(articleId);
        if (chapter == null || !Objects.equals(chapter.getSeriesId(), series.getId())) {
            throw new BusinessException(404, "该文章不属于此系列");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getDeletedAt() != null || !"PUBLISHED".equals(article.getPublishStatus())) {
            throw new BusinessException(404, "章节尚未发布");
        }
        LocalDateTime now = now();
        if (progressMapper.advance(reader, series.getId(), articleId, chapter.getChapterOrder(), now) == 0) {
            SeriesReadingProgress progress = new SeriesReadingProgress();
            progress.setId(IdWorker.getId());
            progress.setUserId(reader);
            progress.setSeriesId(series.getId());
            progress.setLastArticleId(articleId);
            progress.setLastChapterOrder(chapter.getChapterOrder());
            progress.setMaxChapterOrder(chapter.getChapterOrder());
            progress.setCreatedAt(now);
            progress.setUpdatedAt(now);
            try {
                progressMapper.insert(progress);
            } catch (DuplicateKeyException ignored) {
                // Another request created the row first; replay the update onto it.
                progressMapper.advance(reader, series.getId(), articleId, chapter.getChapterOrder(), now);
            }
        }
        return stateOf(reader, series);
    }

    private SeriesReaderStateView stateOf(Long reader, Series series) {
        SeriesReadingProgress progress = progressMapper.findByUserAndSeries(reader, series.getId());
        List<SeriesArticle> chapters = chapterMapper.findBySeries(series.getId());
        return new SeriesReaderStateView(
                series.getId(),
                followMapper.findRelation(reader, TARGET_SERIES, series.getId()) != null,
                followMapper.countFollowers(TARGET_SERIES, series.getId()),
                progress == null ? null : progress.getLastArticleId(),
                progress == null || progress.getMaxChapterOrder() == null ? 0 : progress.getMaxChapterOrder(),
                chapters.size()
        );
    }

    private static Long requireReader(Long actorUserId) {
        if (actorUserId == null) throw new BusinessException(401, "请先登录");
        return actorUserId;
    }

    private Series requirePublicSeries(Long seriesId) {
        if (seriesId == null) throw new BusinessException(400, "系列 ID 无效");
        Series series = seriesMapper.findPublicById(seriesId);
        if (series == null) throw new BusinessException(404, "系列不存在或尚未公开");
        return series;
    }

    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
}
