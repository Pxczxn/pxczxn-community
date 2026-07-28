package top.pxczxn.community.appeal.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_appeal_event")
public class CommunityAppealEvent {
    @TableId(type = IdType.AUTO) private Long id;
    private Long appealId;
    private String actorType;
    private Long actorId;
    private String eventType;
    private String beforeSnapshot;
    private String afterSnapshot;
    private LocalDateTime occurredAt;
}
