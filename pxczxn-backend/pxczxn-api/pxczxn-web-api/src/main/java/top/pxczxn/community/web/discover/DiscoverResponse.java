package top.pxczxn.community.web.discover;

import top.pxczxn.community.editorial.application.EditorialCollectionView;
import top.pxczxn.community.article.application.PublicArticleSummaryView;
import top.pxczxn.community.blog.persistence.BlogPopularRow;
import top.pxczxn.community.discover.application.DiscoverView;
import top.pxczxn.community.series.persistence.SeriesPopularRow;
import top.pxczxn.community.social.persistence.MomentLatestRow;
import top.pxczxn.community.taxonomy.application.PlatformTagView;

import java.time.LocalDateTime;
import java.util.List;

public record DiscoverResponse(
        List<EditorialCollectionView> editorials,
        List<ArticleSummary> hotArticles,
        List<SeriesSummary> popularSeries,
        List<BlogSummary> popularCreators,
        List<BlogSummary> popularTeams,
        List<MomentSummary> latestMoments,
        List<TagSummary> trendingTags
) {
    static DiscoverResponse from(DiscoverView view) {
        return new DiscoverResponse(
                view.editorials(),
                view.hotArticles().stream().map(ArticleSummary::from).toList(),
                view.popularSeries().stream().map(SeriesSummary::from).toList(),
                view.popularCreators().stream().map(BlogSummary::from).toList(),
                view.popularTeams().stream().map(BlogSummary::from).toList(),
                view.latestMoments().stream().map(MomentSummary::from).toList(),
                view.trendingTags().stream().map(t -> new TagSummary(t.tagId(), t.name(), t.slug(), t.usageCount())).toList()
        );
    }

    public record ArticleSummary(Long articleId, String title, String slug, String summary, Long coverFileId,
                                  String canonicalPath, LocalDateTime publishedAt) {
        static ArticleSummary from(PublicArticleSummaryView v) {
            return new ArticleSummary(v.articleId(), v.title(), v.slug(), v.summary(), v.coverFileId(),
                    v.canonicalPath(), v.publishedAt());
        }
    }

    public record SeriesSummary(Long id, String title, String slug, String summary, Long coverFileId,
                                 String blogName, String blogSlug, String creatorName, Long followCount,
                                 LocalDateTime publishedAt) {
        static SeriesSummary from(SeriesPopularRow r) {
            return new SeriesSummary(r.getId(), r.getTitle(), r.getSlug(), r.getSummary(), r.getCoverFileId(),
                    r.getBlogName(), r.getBlogSlug(), r.getCreatorName(), r.getFollowCount(), r.getPublishedAt());
        }
    }

    public record BlogSummary(Long id, String name, String slug, String summary, Long avatarFileId,
                               Long followerCount, Long articleCount, String ownerName) {
        static BlogSummary from(BlogPopularRow r) {
            return new BlogSummary(r.getId(), r.getName(), r.getSlug(), r.getSummary(), r.getAvatarFileId(),
                    r.getFollowerCount(), r.getArticleCount(), r.getOwnerName());
        }
    }

    public record MomentSummary(Long id, String momentType, String textContent, Long likeCount,
                                 Long commentCount, String authorName, String blogName, LocalDateTime createdAt) {
        static MomentSummary from(MomentLatestRow r) {
            return new MomentSummary(r.getId(), r.getMomentType(), r.getTextContent(),
                    r.getLikeCount(), r.getCommentCount(), r.getAuthorName(), r.getBlogName(), r.getCreatedAt());
        }
    }

    public record TagSummary(Long tagId, String name, String slug, long usageCount) {}
}