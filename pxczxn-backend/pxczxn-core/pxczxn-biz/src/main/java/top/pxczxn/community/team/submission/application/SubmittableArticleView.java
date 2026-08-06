package top.pxczxn.community.team.submission.application;

import top.pxczxn.community.article.model.Article;

import java.time.LocalDateTime;

/**
 * 投稿页"选择我的文章"下拉里的一条候选。
 *
 * <p>ID 保持 {@code Long}，由全局 Jackson 配置序列化为字符串，前端一律按 string 处理；
 * 时间字段保留原始类型，交给前端格式化。
 */
public record SubmittableArticleView(
        Long articleId,
        String title,
        String publishStatus,
        String visibility,
        LocalDateTime updatedAt
) {
    public static SubmittableArticleView from(Article article) {
        return new SubmittableArticleView(
                article.getId(),
                article.getTitle(),
                article.getPublishStatus(),
                article.getVisibility(),
                article.getUpdatedAt() == null ? article.getCreatedAt() : article.getUpdatedAt());
    }
}
