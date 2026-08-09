package top.pxczxn.community.social.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.block.application.CommunityBlockService;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.persistence.FollowingFeedMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowingFeedService {
    private final FollowingFeedMapper mapper;
    private final CommunityAuth communityAuth;
    private final CommunityBlockService blockService;

    @Transactional(readOnly = true)
    public FollowingFeedPageView page(Integer rawPageNum, Integer rawPageSize) {
        int pageNum = rawPageNum == null ? 1 : rawPageNum;
        int pageSize = rawPageSize == null ? 20 : rawPageSize;
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) throw new BusinessException(400, "分页参数无效");
        Long userId = communityAuth.getLoginUserId();
        List<FollowingFeedItemView> records = mapper.selectPage(userId, (long) (pageNum - 1) * pageSize, pageSize).stream()
                .filter(row -> !blocked(userId, row.getItemType(), row.getTargetId(), row.getAuthorUserId(), row.getBlogId()))
                .map(row -> new FollowingFeedItemView(row.getItemType(), row.getTargetId(), row.getTitle(), row.getExcerpt(), row.getCanonicalPath(), row.getAuthorName(), row.getBlogName(), row.getTagName(), row.getOccurredAt(), row.getSpecialFollow())).toList();
        return new FollowingFeedPageView(records, mapper.count(userId), pageNum, pageSize);
    }

    private boolean blocked(Long userId, String type, Long targetId, Long authorUserId, Long blogId) {
        if ("SERIES".equals(type)) return blockService.isBlogBlocked(userId, blogId);
        return blockService.isContentBlocked(userId, authorUserId, blogId, "MOMENT".equals(type) ? "MOMENT" : "ARTICLE", targetId);
    }
}
