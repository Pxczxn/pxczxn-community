package top.pxczxn.community.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("community_user_preference")
public class CommunityUserPreference {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String likesVisibility;
}
