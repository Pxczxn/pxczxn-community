package top.pxczxn.community.block.application;

import top.pxczxn.community.block.model.CommunityBlock;

import java.time.LocalDateTime;

public record CommunityBlockView(
        Long id,
        String targetType,
        Long targetId,
        LocalDateTime createdAt
) {
    static CommunityBlockView from(CommunityBlock value) {
        return new CommunityBlockView(
                value.getId(), value.getTargetType(), value.getTargetId(), value.getCreatedAt()
        );
    }
}
