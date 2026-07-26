package top.pxczxn.community.article.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article_version")
public class ArticleVersion {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long articleId;

    private Integer versionNo;

    private String contentMode;

    private String richTextJson;

    private String markdownContent;

    private String renderedHtml;

    private String plainText;

    private String tocJson;

    private String contentHash;

    private Integer wordCount;

    private Integer readingTimeMinutes;

    private Long createdByUserId;

    private String creationType;

    private LocalDateTime createdAt;
}
