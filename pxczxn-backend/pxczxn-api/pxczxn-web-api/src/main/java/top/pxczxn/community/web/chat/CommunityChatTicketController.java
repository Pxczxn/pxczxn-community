package top.pxczxn.community.web.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;
import top.pxczxn.platform.websocket.WebSocketTicketService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class CommunityChatTicketController {

    private final CommunityAuth auth;
    private final WebSocketTicketService tickets;

    @PostMapping("/websocket-ticket")
    public Result<WebSocketTicketService.IssuedTicket> ticket() {
        return Result.ok(tickets.issue("COMMUNITY", auth.getLoginUserId()));
    }
}
