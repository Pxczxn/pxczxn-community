package top.pxczxn.platform.admin.controller.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.platform.admin.websocket.MessageWebSocketHandler;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.PageResult;
import top.pxczxn.platform.common.result.Result;
import top.pxczxn.platform.message.entity.SysChatMessage;
import top.pxczxn.platform.message.model.ChatContact;
import top.pxczxn.platform.message.service.ChatGroupService;
import top.pxczxn.platform.message.service.SysChatMessageService;
import top.pxczxn.platform.system.entity.SysUser;
import top.pxczxn.platform.system.entity.SysUserBlacklist;
import top.pxczxn.platform.system.service.SysUserBlacklistService;
import top.pxczxn.platform.system.service.SysUserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 即时私聊。
 */
@RestController
@RequestMapping("/sys/chat")
@RequiredArgsConstructor
@SaCheckPermission("sys:chat:list")
public class SysChatController {

    private final SysChatMessageService chatMessageService;
    private final ChatGroupService groupService;
    private final SysUserService userService;
    private final SysUserBlacklistService blacklistService;
    private final MessageWebSocketHandler webSocketHandler;

    @PostMapping("/send")
    public Result<SysChatMessage> send(@RequestBody ChatSendRequest request) {
        if (request == null) {
            throw new BusinessException(400, "消息请求不能为空");
        }
        Long senderId = StpUtil.getLoginIdAsLong();
        SysChatMessage message = chatMessageService.send(
                senderId,
                request.getReceiverId(),
                request.getContent(),
                request.getMsgType());
        webSocketHandler.sendEvent(message.getReceiverId(), "chat", message);
        return Result.ok(message);
    }

    @GetMapping("/history/{targetId}")
    public Result<PageResult<SysChatMessage>> getChatHistory(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(PageResult.of(
                chatMessageService.getChatHistory(userId, targetId, page, pageSize)));
    }

    @GetMapping("/contacts")
    public Result<List<ChatContact>> getRecentContacts() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ChatContact> contacts = chatMessageService.getRecentContacts(userId);
        contacts.forEach(contact ->
                contact.setOnline(webSocketHandler.isOnline(contact.getUserId())));
        return Result.ok(contacts);
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : userService.listAll()) {
            if (user.getId().equals(userId)
                    || !Integer.valueOf(1).equals(user.getStatus())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", displayName(user));
            item.put("avatar", user.getAvatar());

            SysChatMessage latestMessage =
                    chatMessageService.getLatestMessage(userId, user.getId());
            if (latestMessage != null) {
                item.put(
                        "lastMessage",
                        latestMessage.getMsgType() == 1
                                ? latestMessage.getContent()
                                : latestMessage.getMsgType() == 2
                                ? "[图片]"
                                : "[文件]");
                item.put("lastMessageTime", latestMessage.getSendTime());
            }
            item.put(
                    "unreadCount",
                    chatMessageService.getUnreadCountWithUser(userId, user.getId()));
            item.put(
                    "isBlocked",
                    blacklistService.isInMyBlacklist(userId, user.getId()));
            item.put("online", webSocketHandler.isOnline(user.getId()));
            result.add(item);
        }
        return Result.ok(result);
    }

    @PostMapping("/read/{senderId}")
    public Result<Void> markAsRead(@PathVariable Long senderId) {
        chatMessageService.markAsRead(StpUtil.getLoginIdAsLong(), senderId);
        return Result.ok();
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(
                chatMessageService.getUnreadCount(userId)
                        + groupService.getUnreadCount(userId));
    }

    @GetMapping("/stats")
    public Result<Map<String, Integer>> getMessageStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        int privateCount = chatMessageService.getUnreadCount(userId);
        int groupCount = groupService.getUnreadCount(userId);
        Map<String, Integer> stats = new HashMap<>();
        stats.put("privateChatCount", privateCount);
        stats.put("groupChatCount", groupCount);
        stats.put("chatCount", privateCount + groupCount);
        return Result.ok(stats);
    }

    @GetMapping("/online/{userId}")
    public Result<Boolean> isOnline(@PathVariable Long userId) {
        requireTargetUser(userId);
        return Result.ok(webSocketHandler.isOnline(userId));
    }

    @DeleteMapping("/clear/{targetId}")
    public Result<Void> clearHistory(@PathVariable Long targetId) {
        chatMessageService.clearHistory(StpUtil.getLoginIdAsLong(), targetId);
        return Result.ok();
    }

    @PostMapping("/block/{targetId}")
    public Result<Void> blockUser(@PathVariable Long targetId) {
        requireTargetUser(targetId);
        blacklistService.blockUser(StpUtil.getLoginIdAsLong(), targetId);
        return Result.ok();
    }

    @DeleteMapping("/block/{targetId}")
    public Result<Void> unblockUser(@PathVariable Long targetId) {
        requireTargetUser(targetId);
        blacklistService.unblockUser(StpUtil.getLoginIdAsLong(), targetId);
        return Result.ok();
    }

    @GetMapping("/blacklist")
    public Result<List<SysUserBlacklist>> getBlacklist() {
        return Result.ok(
                blacklistService.getBlacklist(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/blocked/{targetId}")
    public Result<Boolean> isBlocked(@PathVariable Long targetId) {
        requireTargetUser(targetId);
        return Result.ok(blacklistService.isInMyBlacklist(
                StpUtil.getLoginIdAsLong(),
                targetId));
    }

    private SysUser requireTargetUser(Long targetId) {
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }
        SysUser user = userService.getById(targetId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(404, "用户不存在或已停用");
        }
        return user;
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }

    @Data
    public static class ChatSendRequest {
        private Long receiverId;
        private String content;
        private Integer msgType;
    }
}
