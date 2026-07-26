package top.pxczxn.community.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_user")
public class CommunityUser {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String username;

    private String displayName;

    private String bio;

    private Long avatarFileId;

    private String status;

    private Long personalBlogId;

    private String verificationStatus;

    private LocalDateTime publishRestrictedUntil;

    private LocalDateTime commentRestrictedUntil;

    private Integer lockVersion;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
