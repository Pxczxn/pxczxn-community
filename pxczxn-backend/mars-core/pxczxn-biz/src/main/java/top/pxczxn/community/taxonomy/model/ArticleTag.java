package top.pxczxn.community.taxonomy.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article_tag")
public class ArticleTag {

    private Long articleId;

    private Long tagId;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
