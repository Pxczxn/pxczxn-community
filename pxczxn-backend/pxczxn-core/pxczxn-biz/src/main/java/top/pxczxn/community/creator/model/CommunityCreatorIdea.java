package top.pxczxn.community.creator.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_creator_idea")
public class CommunityCreatorIdea {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long ownerUserId;
    private String title;
    private String content;
    private String tagsText;
    private String sourceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
