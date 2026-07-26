package top.pxczxn.community.blog.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("blog")
public class Blog {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String blogType;

    private Long ownerUserId;

    private String name;

    private String slug;

    private String summary;

    private Long avatarFileId;

    private Long backgroundFileId;

    private String status;

    private Long articleCount;

    private Long followerCount;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
