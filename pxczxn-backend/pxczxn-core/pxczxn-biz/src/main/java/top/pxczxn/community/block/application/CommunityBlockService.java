package top.pxczxn.community.block.application;

import java.util.List;

public interface CommunityBlockService {

    CommunityBlockView block(Long blockerUserId, String targetType, Long targetId);

    void unblock(Long blockerUserId, String targetType, Long targetId);

    List<CommunityBlockView> mine(Long blockerUserId);

    boolean isContentBlocked(Long viewerUserId, Long authorUserId, Long blogId, String contentType, Long contentId);

    boolean isNotificationBlocked(Long viewerUserId, Long senderUserId, String targetType, Long targetId);

    boolean isChatRestricted(Long firstUserId, Long secondUserId);

    boolean isUserBlocked(Long viewerUserId, Long otherUserId);

    boolean isBlogBlocked(Long viewerUserId, Long blogId);
}
