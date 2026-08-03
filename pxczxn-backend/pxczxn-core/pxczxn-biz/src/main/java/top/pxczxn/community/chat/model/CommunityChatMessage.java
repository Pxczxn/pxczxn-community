package top.pxczxn.community.chat.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_chat_message")
public class CommunityChatMessage {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long senderUserId;
    private Long recipientUserId;
    private String contentText;
    private String status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime senderDeletedAt;
    private LocalDateTime recipientDeletedAt;
}
