package top.pxczxn.community.block.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_block")
public class CommunityBlock {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long blockerUserId;
    private String targetType;
    private Long targetId;
    private LocalDateTime createdAt;
}
