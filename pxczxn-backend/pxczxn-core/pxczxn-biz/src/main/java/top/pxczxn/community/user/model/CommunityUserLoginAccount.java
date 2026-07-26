package top.pxczxn.community.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_user_login_account")
public class CommunityUserLoginAccount {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String loginType;

    private String normalizedIdentifier;

    private String passwordHash;

    private LocalDateTime verifiedAt;

    private Integer failedLoginCount;

    private LocalDateTime lockedUntil;

    private LocalDateTime lastLoginAt;
}
