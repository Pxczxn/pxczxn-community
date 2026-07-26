package top.pxczxn.community.social.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_moment")
public class CommunityMoment {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long actorUserId;

    private Long blogId;

    private String momentType;

    private String textContent;

    private String renderedHtml;

    private String linkUrl;

    private Long articleId;

    private Long repostMomentId;

    private String visibility;

    private String status;

    private Long likeCount;

    private Long favoriteCount;

    private Long commentCount;

    private Long repostCount;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
