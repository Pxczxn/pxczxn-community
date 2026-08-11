package top.pxczxn.community.creator.application;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCreatorIdeaView(
        Long id,
        String title,
        String content,
        List<String> tags,
        String sourceType,
        LocalDateTime createdAt
) {
}
