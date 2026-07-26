package top.pxczxn.community.article.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article")
public class Article {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long blogId;

    private Long authorUserId;

    private Long categoryId;

    private String title;

    private String slug;

    private String summary;

    private Long coverFileId;

    private String contentMode;

    private String visibility;

    private String publishMethod;

    private String publishStatus;

    private String reviewStatus;

    private Long currentVersionId;

    private Long publishedVersionId;

    private Long reviewVersionId;

    private LocalDateTime scheduledPublishAt;

    private LocalDateTime publishedAt;

    private String canonicalPath;

    private Long viewCount;

    private Long likeCount;

    private Long favoriteCount;

    private Long commentCount;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
