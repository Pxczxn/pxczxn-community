package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;

import java.util.Locale;
import java.util.Set;

public enum MomentType {
    TEXT,
    LINK,
    ARTICLE_SHARE,
    PROJECT_UPDATE,
    CODE,
    REPOST,
    QUOTE,
    VIDEO_LINK;

    private static final Set<MomentType> TEXT_REQUIRED =
            Set.of(TEXT, PROJECT_UPDATE, CODE, QUOTE);
    private static final Set<MomentType> LINK_REQUIRED =
            Set.of(LINK, VIDEO_LINK);

    public static MomentType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(400, "动态类型不能为空");
        }
        try {
            return valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "当前版本不支持该动态类型");
        }
    }

    public boolean requiresText() {
        return TEXT_REQUIRED.contains(this);
    }

    public boolean requiresLink() {
        return LINK_REQUIRED.contains(this);
    }

    public boolean referencesArticle() {
        return this == ARTICLE_SHARE;
    }

    public boolean referencesMoment() {
        return this == REPOST || this == QUOTE;
    }
}
