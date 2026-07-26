package top.pxczxn.community.social.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_comment")
public class CommunityComment {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long authorUserId;

    private String targetType;

    private Long targetId;

    private Long rootCommentId;

    private Long parentCommentId;

    private Long replyToUserId;

    private String contentText;

    private String renderedHtml;

    private String status;

    private Long likeCount;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
