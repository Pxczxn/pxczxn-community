package top.pxczxn.platform.admin.controller.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.platform.admin.websocket.MessageWebSocketHandler;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.PageResult;
import top.pxczxn.platform.common.result.Result;
import top.pxczxn.platform.message.entity.ChatGroup;
import top.pxczxn.platform.message.entity.ChatGroupMember;
import top.pxczxn.platform.message.entity.ChatGroupMessage;
import top.pxczxn.platform.message.service.ChatGroupService;

import java.util.List;

/**
 * 群聊 API。
 */
@RestController
@RequestMapping("/chat/group")
@RequiredArgsConstructor
@SaCheckPermission("sys:chat:list")
public class ChatGroupController {

    private final ChatGroupService groupService;
    private final MessageWebSocketHandler webSocketHandler;

    @PostMapping("/create")
    public Result<ChatGroup> create(@RequestBody CreateGroupRequest request) {
        if (request == null) {
            throw new BusinessException(400, "创建群请求不能为空");
        }
        return Result.ok(groupService.createGroup(
                request.getName(),
                currentUserId(),
                request.getMemberIds()));
    }

    @GetMapping("/list")
    public Result<List<ChatGroup>> list() {
        return Result.ok(groupService.getUserGroups(currentUserId()));
    }

    @GetMapping("/{groupId}")
    public Result<ChatGroup> detail(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupDetail(groupId, currentUserId()));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody UpdateGroupRequest request) {
        if (request == null) {
            throw new BusinessException(400, "群信息请求不能为空");
        }
        groupService.updateGroup(
                request.getId(),
                currentUserId(),
                request.getName(),
                request.getAnnouncement());
        return Result.ok();
    }

    @DeleteMapping("/{groupId}")
    public Result<Void> dissolve(@PathVariable Long groupId) {
        groupService.dissolveGroup(groupId, currentUserId());
        return Result.ok();
    }

    @PostMapping("/{groupId}/quit")
    public Result<Void> quit(@PathVariable Long groupId) {
        groupService.quitGroup(groupId, currentUserId());
        return Result.ok();
    }

    @GetMapping("/{groupId}/members")
    public Result<List<ChatGroupMember>> members(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupMembers(groupId, currentUserId()));
    }

    @PostMapping("/{groupId}/members")
    public Result<Void> addMembers(@PathVariable Long groupId,
                                   @RequestBody MemberIdsRequest request) {
        if (request == null) {
            throw new BusinessException(400, "成员请求不能为空");
        }
        groupService.addMembers(
                groupId,
                request.getUserIds(),
                currentUserId());
        return Result.ok();
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public Result<Void> removeMember(@PathVariable Long groupId,
                                     @PathVariable Long memberId) {
        groupService.removeMember(groupId, memberId, currentUserId());
        return Result.ok();
    }

    @PostMapping("/{groupId}/admin/{memberId}")
    public Result<Void> setAdmin(@PathVariable Long groupId,
                                 @PathVariable Long memberId,
                                 @RequestParam boolean isAdmin) {
        groupService.setAdmin(
                groupId,
                memberId,
                isAdmin,
                currentUserId());
        return Result.ok();
    }

    @PostMapping("/{groupId}/mute/{memberId}")
    public Result<Void> setMuted(@PathVariable Long groupId,
                                 @PathVariable Long memberId,
                                 @RequestParam boolean muted) {
        groupService.setMuted(
                groupId,
                memberId,
                muted,
                currentUserId());
        return Result.ok();
    }

    @PostMapping("/{groupId}/transfer/{newOwnerId}")
    public Result<Void> transferOwner(@PathVariable Long groupId,
                                      @PathVariable Long newOwnerId) {
        groupService.transferOwner(
                groupId,
                newOwnerId,
                currentUserId());
        return Result.ok();
    }

    @PostMapping("/{groupId}/message")
    public Result<ChatGroupMessage> sendMessage(
            @PathVariable Long groupId,
            @RequestBody SendMessageRequest request) {
        if (request == null) {
            throw new BusinessException(400, "消息请求不能为空");
        }
        Long senderId = currentUserId();
        ChatGroupMessage message = groupService.sendMessage(
                groupId,
                senderId,
                request.getContent(),
                request.getMsgType());
        for (Long memberId : groupService.getMemberIds(groupId)) {
            if (!memberId.equals(senderId)) {
                webSocketHandler.sendEvent(memberId, "groupChat", message);
            }
        }
        return Result.ok(message);
    }

    @GetMapping("/{groupId}/messages")
    public Result<PageResult<ChatGroupMessage>> messages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return Result.ok(PageResult.of(groupService.getMessageHistory(
                groupId,
                currentUserId(),
                page,
                pageSize)));
    }

    @PostMapping("/{groupId}/read")
    public Result<Void> markAsRead(@PathVariable Long groupId) {
        groupService.markAsRead(groupId, currentUserId());
        return Result.ok();
    }

    @GetMapping("/{groupId}/unread-count")
    public Result<Integer> unreadCount(@PathVariable Long groupId) {
        return Result.ok(groupService.getUnreadCount(
                groupId,
                currentUserId()));
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Data
    public static class CreateGroupRequest {
        private String name;
        private List<Long> memberIds;
    }

    @Data
    public static class UpdateGroupRequest {
        private Long id;
        private String name;
        private String announcement;
    }

    @Data
    public static class MemberIdsRequest {
        private List<Long> userIds;
    }

    @Data
    public static class SendMessageRequest {
        private String content;
        private Integer msgType;
    }
}
