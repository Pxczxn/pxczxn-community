package top.pxczxn.community.social.application;

import java.util.List;

public record MomentPageView(
        List<MomentView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
