package top.pxczxn.community.article.application;

import top.pxczxn.platform.common.exception.BusinessException;

import java.util.Locale;

public enum PublicDiscoverySort {
    LATEST("按公开发布时间从新到旧排序"),
    HOT("按互动热度与时间衰减综合排序"),
    VIEWS("按公开阅读数从高到低排序，同分按发布时间排序"),
    LIKES("按公开点赞数从高到低排序，同分按发布时间排序"),
    FAVORITES("按公开收藏数从高到低排序，同分按发布时间排序"),
    COMMENTS("按公开评论数从高到低排序，同分按发布时间排序"),
    QUALITY("按贝叶斯先验平滑互动率、内容完整度和审核状态综合排序"),
    RISK("已审核内容优先，再按发布时间排序；不展示风险评分");

    private final String explanation;

    PublicDiscoverySort(String explanation) { this.explanation = explanation; }
    public String explanation() { return explanation; }

    public static PublicDiscoverySort parse(String raw) {
        if (raw == null || raw.isBlank()) return LATEST;
        try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BusinessException(400, "发现页排序类型无效"); }
    }
}
