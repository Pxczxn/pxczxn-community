package top.pxczxn.community.social.application;

import java.util.List;

public record SocialProfilePageView(
        List<SocialProfileView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
