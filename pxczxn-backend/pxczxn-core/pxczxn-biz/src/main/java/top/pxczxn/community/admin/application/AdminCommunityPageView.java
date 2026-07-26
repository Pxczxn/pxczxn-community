package top.pxczxn.community.admin.application;

import java.util.List;

public record AdminCommunityPageView<T>(
        List<T> list,
        long total,
        long pageNum,
        long pageSize
) {
}
