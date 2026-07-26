package top.pxczxn.community.social.application;

import java.util.List;

public record CommentThreadView(
        CommentView root,
        List<CommentView> replyPreview,
        long replyCount
) {
}
