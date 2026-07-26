package top.pxczxn.community.blog.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("blog_setting")
public class BlogSetting {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long blogId;

    private String commentScope;

    private String defaultVisibility;

    private String allowRepost;

    private String themeKey;

    private String themeConfigJson;

    private String seoTitle;

    private String seoDescription;
}
