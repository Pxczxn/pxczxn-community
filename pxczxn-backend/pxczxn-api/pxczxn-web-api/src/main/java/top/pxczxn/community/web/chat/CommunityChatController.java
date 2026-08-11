package top.pxczxn.community.web.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.chat.application.CommunityChatMessageView;
import top.pxczxn.community.chat.application.CommunityChatConversationView;
import top.pxczxn.community.chat.application.CommunityChatService;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class CommunityChatController {

    private final CommunityChatService service;
    private final CommunityAuth auth;

    @PostMapping("/messages")
    public Result<MessageResponse> send(@Valid @RequestBody SendRequest request) {
        return Result.ok(MessageResponse.from(service.send(
                auth.getLoginUserId(),
                request.recipientUserId(),
                request.contentText()
        )));
    }

    @GetMapping("/messages/{peerId}")
    public Result<List<MessageResponse>> history(
            @PathVariable Long peerId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.ok(service.history(auth.getLoginUserId(), peerId, limit).stream()
                .map(MessageResponse::from)
                .toList());
    }

    @PostMapping("/messages/{peerId}/read")
    public Result<Void> read(@PathVariable Long peerId) {
        service.markRead(auth.getLoginUserId(), peerId);
        return Result.ok();
    }

    @GetMapping("/conversations")
    public Result<List<ConversationResponse>> conversations(
            @RequestParam(required = false) Integer limit
    ) {
        return Result.ok(service.conversations(auth.getLoginUserId(), limit).stream()
                .map(ConversationResponse::from)
                .toList());
    }

    public record SendRequest(
            @NotNull Long recipientUserId,
            @NotBlank String contentText
    ) {
    }

    public record MessageResponse(
            String id,
            String senderUserId,
            String recipientUserId,
            String contentText,
            String status,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        static MessageResponse from(CommunityChatMessageView view) {
            return new MessageResponse(
                    view.id().toString(),
                    view.senderUserId().toString(),
                    view.recipientUserId().toString(),
                    view.contentText(),
                    view.status(),
                    view.readAt(),
                    view.createdAt()
            );
        }
    }

    public record ConversationResponse(
            String peerUserId,
            String peerUsername,
            String peerDisplayName,
            String peerAvatarFileId,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount
    ) {
        static ConversationResponse from(CommunityChatConversationView view) {
            return new ConversationResponse(
                    view.peerUserId().toString(),
                    view.peerUsername(),
                    view.peerDisplayName(),
                    view.peerAvatarFileId() == null ? null : view.peerAvatarFileId().toString(),
                    view.lastMessage(),
                    view.lastMessageAt(),
                    view.unreadCount()
            );
        }
    }
}
