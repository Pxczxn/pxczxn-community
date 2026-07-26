package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;

import java.util.Locale;

public enum LikeTargetType {
    ARTICLE,
    MOMENT,
    COMMENT;

    public static LikeTargetType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "点赞目标类型不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "点赞目标类型无效");
        }
    }
}
