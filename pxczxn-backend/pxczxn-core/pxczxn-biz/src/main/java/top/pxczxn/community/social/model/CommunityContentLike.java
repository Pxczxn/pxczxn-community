package top.pxczxn.community.social.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_content_like")
public class CommunityContentLike {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String targetType;

    private Long targetId;

    private LocalDateTime createdAt;
}
