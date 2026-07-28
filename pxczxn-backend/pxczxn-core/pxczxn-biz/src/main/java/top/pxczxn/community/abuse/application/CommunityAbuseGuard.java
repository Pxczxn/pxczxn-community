package top.pxczxn.community.abuse.application;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.abuse.model.CommunityAbuseEvent;
import top.pxczxn.community.abuse.model.CommunityAbuseWindow;
import top.pxczxn.community.abuse.persistence.CommunityAbuseEventMapper;
import top.pxczxn.community.abuse.persistence.CommunityAbuseWindowMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class CommunityAbuseGuard {

    private final CommunityAbuseWindowMapper windows;
    private final CommunityAbuseEventMapper events;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessException.class)
    public void check(String actorKey, String action, int threshold, int seconds) {
        if (actorKey == null || actorKey.isBlank()) {
            throw new BusinessException(400, "Missing abuse actor");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        windows.recordAttempt(actorKey, action, now, seconds);
        CommunityAbuseWindow row = windows.selectOne(new QueryWrapper<CommunityAbuseWindow>()
                .eq("actor_key", actorKey)
                .eq("action_type", action)
                .last("FOR UPDATE"));
        if (row == null || row.getAttemptCount() == null) {
            throw new IllegalStateException("Abuse window was not persisted");
        }

        int attempts = row.getAttemptCount();
        boolean allowed = attempts <= threshold;
        if (!allowed) {
            windows.update(null, new UpdateWrapper<CommunityAbuseWindow>()
                    .eq("actor_key", actorKey)
                    .eq("action_type", action)
                    .set("rejected_count", (row.getRejectedCount() == null ? 0 : row.getRejectedCount()) + 1));
        }

        CommunityAbuseEvent event = new CommunityAbuseEvent();
        event.setActorKey(actorKey);
        event.setActionType(action);
        event.setDecision(allowed ? "ALLOW" : "REJECT");
        event.setThreshold(threshold);
        event.setWindowSeconds(seconds);
        event.setAttemptCount(attempts);
        event.setOccurredAt(now);
        events.insert(event);

        if (!allowed) {
            throw new BusinessException(429, "Too many " + action + " actions; retry after the policy window");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessException.class)
    public void rejectDuplicateContent(Long userId, String scope, String content) {
        if (userId == null || scope == null || scope.isBlank()) {
            throw new BusinessException(400, "Missing duplicate-content subject");
        }
        String normalized = Normalizer.normalize(content == null ? "" : content, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .strip()
                .toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        check("CONTENT:" + userId + ":" + sha256(normalized), scope + "_DUPLICATE", 1, 600);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte valueByte : bytes) {
                result.append(String.format("%02x", valueByte));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
