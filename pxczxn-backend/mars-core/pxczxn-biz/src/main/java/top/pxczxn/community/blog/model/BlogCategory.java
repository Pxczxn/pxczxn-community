package top.pxczxn.community.blog.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("blog_category")
public class BlogCategory {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long blogId;

    private String name;

    private String slug;

    private String description;

    private Integer sortOrder;

    private Integer isDefault;

    private Long articleCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Long defaultBlogId;
}
