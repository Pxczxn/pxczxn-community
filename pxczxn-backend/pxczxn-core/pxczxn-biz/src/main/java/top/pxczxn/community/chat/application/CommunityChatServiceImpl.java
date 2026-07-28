package top.pxczxn.community.chat.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.block.application.CommunityBlockService;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.chat.model.CommunityChatMessage;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityChatServiceImpl implements CommunityChatService {

    private static final Set<String> ACTIVE_USER_STATUSES = Set.of("NORMAL", "LIMITED");

    private final CommunityChatMessageMapper mapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityBlockService blockService;
    private final ApplicationEventPublisher eventPublisher;
    private final CommunityAbuseGuard abuseGuard;

    @Override
    @Transactional
    public CommunityChatMessageView send(Long actor, Long recipient, String content) {
        authorize(actor, recipient);
        abuseGuard.check("USER:" + actor, "CHAT_SEND", 20, 60);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() || text.length() > 2000) {
            throw new BusinessException(400, "Message content is invalid");
        }

        CommunityChatMessage message = new CommunityChatMessage();
        message.setId(IdWorker.getId());
        message.setSenderUserId(actor);
        message.setRecipientUserId(recipient);
        message.setContentText(text);
        message.setStatus("SENT");
        message.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (mapper.insert(message) != 1) {
            throw new BusinessException(500, "Unable to send message");
        }

        CommunityChatMessageView view = CommunityChatMessageView.from(message);
        eventPublisher.publishEvent(new CommunityChatMessageSentEvent(view));
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityChatMessageView> history(Long actor, Long peer, Integer limit) {
        authorize(actor, peer);
        int size = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
        return mapper.history(actor, peer, size).stream()
                .sorted(Comparator.comparing(CommunityChatMessage::getId))
                .map(CommunityChatMessageView::from)
                .toList();
    }

    @Override
    @Transactional
    public void markRead(Long actor, Long peer) {
        authorize(actor, peer);
        mapper.markRead(peer, actor, LocalDateTime.now(ZoneOffset.UTC));
    }

    private void authorize(Long actor, Long peer) {
        if (actor == null || peer == null || actor.equals(peer)) {
            throw new BusinessException(403, "Not authorized to access this conversation");
        }
        if (blockService.isChatRestricted(actor, peer)) {
            throw new BusinessException(403, "This conversation is blocked");
        }
        CommunityUser actorUser = userMapper.selectById(actor);
        CommunityUser peerUser = userMapper.selectById(peer);
        if (actorUser == null
                || peerUser == null
                || !ACTIVE_USER_STATUSES.contains(actorUser.getStatus())
                || !ACTIVE_USER_STATUSES.contains(peerUser.getStatus())
                || actorUser.getPersonalBlogId() == null
                || peerUser.getPersonalBlogId() == null
                || followMapper.countMutualBlogPair(
                        actor,
                        actorUser.getPersonalBlogId(),
                        peer,
                        peerUser.getPersonalBlogId()) != 1) {
            throw new BusinessException(403, "Only mutual followers can use direct chat");
        }
    }
}
