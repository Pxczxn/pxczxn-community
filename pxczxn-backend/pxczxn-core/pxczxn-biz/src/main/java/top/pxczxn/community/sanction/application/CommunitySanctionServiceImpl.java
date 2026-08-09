package top.pxczxn.community.sanction.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.sanction.model.CommunitySanction;
import top.pxczxn.community.sanction.model.CommunitySanctionEvent;
import top.pxczxn.community.sanction.persistence.CommunitySanctionEventMapper;
import top.pxczxn.community.sanction.persistence.CommunitySanctionMapper;
import top.pxczxn.community.sanction.persistence.CommunitySanctionRateLimitMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommunitySanctionServiceImpl implements CommunitySanctionService {

    private static final int RATE_LIMIT_SECONDS = 30;

    private final CommunitySanctionMapper mapper;
    private final CommunitySanctionEventMapper events;
    private final CommunitySanctionRateLimitMapper rateLimits;
    private final CommunityUserMapper users;

    @Override
    @Transactional
    public SanctionView issue(
            Long adminId,
            Long targetUserId,
            String sanctionType,
            String reasonCode,
            String reasonNote,
            Long sourceReportId,
            LocalDateTime expiresAt
    ) {
        SanctionType type = parse(sanctionType);
        requireUser(targetUserId);
        if (type.requiresExpiry() && (expiresAt == null || !expiresAt.isAfter(now()))) {
            throw new BusinessException(400, "定时处罚必须设置未来的到期时间");
        }
        if (!type.requiresExpiry() && expiresAt != null) {
            throw new BusinessException(400, "该处罚类型不支持设置到期时间");
        }

        CommunitySanction sanction = new CommunitySanction();
        sanction.setId(IdWorker.getId());
        sanction.setTargetUserId(targetUserId);
        sanction.setSanctionType(type.name());
        sanction.setReasonCode(required(reasonCode, "reason code"));
        sanction.setReasonNote(normalizeNote(reasonNote));
        sanction.setSourceReportId(sourceReportId);
        sanction.setIssuedByAdminId(requiredId(adminId, "admin"));
        sanction.setStartsAt(now());
        sanction.setExpiresAt(expiresAt);
        sanction.setStatus("ACTIVE");
        if (mapper.insert(sanction) != 1) {
            throw new BusinessException(500, "处罚下发失败");
        }
        event(sanction, "ADMIN", adminId, "ISSUED");
        sync(targetUserId);
        return SanctionView.from(sanction);
    }

    @Override
    @Transactional
    public SanctionView revoke(Long adminId, Long sanctionId, String note) {
        CommunitySanction sanction = mapper.selectById(requiredId(sanctionId, "sanction"));
        if (sanction == null) {
            throw new BusinessException(404, "处罚记录不存在");
        }
        expireDueFor(sanction.getTargetUserId());
        sanction = mapper.selectById(sanctionId);
        if (!"ACTIVE".equals(sanction.getStatus())) {
            return SanctionView.from(sanction);
        }
        sanction.setStatus("REVOKED");
        sanction.setRevokedByAdminId(requiredId(adminId, "admin"));
        sanction.setRevokedAt(now());
        sanction.setRevokeNote(normalizeNote(note));
        if (mapper.updateById(sanction) != 1) {
            throw new BusinessException(409, "处罚状态已变更，请刷新后重试");
        }
        event(sanction, "ADMIN", adminId, "REVOKED");
        sync(sanction.getTargetUserId());
        return SanctionView.from(sanction);
    }

    @Override
    @Transactional
    public void revokeActiveType(Long adminId, Long targetUserId, String sanctionType, String note) {
        SanctionType type = parse(sanctionType);
        List<CommunitySanction> active = mapper.selectList(Wrappers.<CommunitySanction>lambdaQuery()
                .eq(CommunitySanction::getTargetUserId, targetUserId)
                .eq(CommunitySanction::getSanctionType, type.name())
                .eq(CommunitySanction::getStatus, "ACTIVE"));
        if (active.isEmpty()) throw new BusinessException(409, "该账号不存在可恢复的登录限制");
        active.forEach(item -> revoke(adminId, item.getId(), note));
    }

    @Override
    @Transactional
    public List<SanctionView> mine(Long userId) {
        requireUser(userId);
        expireDueFor(userId);
        return mapper.selectList(Wrappers.<CommunitySanction>lambdaQuery()
                        .eq(CommunitySanction::getTargetUserId, userId)
                        .orderByDesc(CommunitySanction::getCreatedAt))
                .stream()
                .map(SanctionView::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SanctionView> activeFor(Long userId) {
        return activeRecords(userId).stream().map(SanctionView::from).toList();
    }

    @Override
    @Transactional
    public void requireActionAllowed(Long userId, SanctionAction action) {
        requireUser(userId);
        expireDueFor(userId);
        List<CommunitySanction> active = activeRecords(userId);
        if (hasBlockingSanction(active, action)) {
            throw new BusinessException(403, "账号当前被限制此操作");
        }
        if (action != SanctionAction.LOGIN && hasType(active, SanctionType.RATE_LIMIT)) {
            LocalDateTime now = now();
            boolean acquired = rateLimits.refreshIfWindowElapsed(
                    userId,
                    action.name(),
                    now,
                    now.minusSeconds(RATE_LIMIT_SECONDS)
            ) == 1;
            if (!acquired) {
                try {
                    rateLimits.create(userId, action.name(), now);
                    acquired = true;
                } catch (DuplicateKeyException exception) {
                    throw new BusinessException(429, "操作过于频繁，请稍后再试");
                }
            }
            if (!acquired) {
                throw new BusinessException(429, "操作过于频繁，请稍后再试");
            }
        }
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${pxczxn.community.sanction.expiry-check-delay-ms:60000}")
    public void expireDueSanctions() {
        mapper.selectList(Wrappers.<CommunitySanction>lambdaQuery()
                        .eq(CommunitySanction::getStatus, "ACTIVE")
                        .isNotNull(CommunitySanction::getExpiresAt)
                        .le(CommunitySanction::getExpiresAt, now()))
                .forEach(sanction -> expireIfDue(sanction));
    }

    private void expireDueFor(Long userId) {
        mapper.selectList(Wrappers.<CommunitySanction>lambdaQuery()
                        .eq(CommunitySanction::getTargetUserId, userId)
                        .eq(CommunitySanction::getStatus, "ACTIVE")
                        .isNotNull(CommunitySanction::getExpiresAt)
                        .le(CommunitySanction::getExpiresAt, now()))
                .forEach(this::expireIfDue);
    }

    private void expireIfDue(CommunitySanction sanction) {
        int updated = mapper.update(
                null,
                Wrappers.<CommunitySanction>update()
                        .eq("id", sanction.getId())
                        .eq("status", "ACTIVE")
                        .le("expires_at", now())
                        .set("status", "EXPIRED")
        );
        if (updated == 1) {
            sanction.setStatus("EXPIRED");
            event(sanction, "SYSTEM", null, "EXPIRED");
            sync(sanction.getTargetUserId());
        }
    }

    private void sync(Long userId) {
        CommunityUser user = users.selectById(userId);
        if (user == null) {
            return;
        }
        List<CommunitySanction> active = activeRecords(userId);
        LocalDateTime commentRestrictedUntil = latestExpiry(active, SanctionType.COMMENT_BAN);
        LocalDateTime publishRestrictedUntil = latestExpiry(
                active, SanctionType.MOMENT_BAN, SanctionType.PUBLISH_SUSPEND
        );
        LocalDateTime submissionRestrictedUntil = latestExpiry(active, SanctionType.SUBMISSION_BAN);
        String nextStatus = user.getStatus();
        String originalStatus = user.getSanctionOriginalStatus();
        boolean permanentBan = hasType(active, SanctionType.PERMANENT_BAN);
        boolean loginSuspend = hasType(active, SanctionType.LOGIN_SUSPEND);
        if (permanentBan || loginSuspend) {
            if (originalStatus == null) {
                originalStatus = nextStatus;
            }
            nextStatus = permanentBan ? "BANNED" : "FROZEN";
        } else if (originalStatus != null) {
            nextStatus = originalStatus;
            originalStatus = null;
        }
        users.update(
                null,
                Wrappers.<CommunityUser>update()
                        .eq("id", userId)
                        .set("comment_restricted_until", commentRestrictedUntil)
                        .set("publish_restricted_until", publishRestrictedUntil)
                        .set("submission_restricted_until", submissionRestrictedUntil)
                        .set("status", nextStatus)
                        .set("sanction_original_status", originalStatus)
        );
    }

    private List<CommunitySanction> activeRecords(Long userId) {
        return mapper.selectList(Wrappers.<CommunitySanction>lambdaQuery()
                .eq(CommunitySanction::getTargetUserId, userId)
                .eq(CommunitySanction::getStatus, "ACTIVE")
                .and(query -> query.isNull(CommunitySanction::getExpiresAt)
                        .or().gt(CommunitySanction::getExpiresAt, now())));
    }

    private boolean hasBlockingSanction(List<CommunitySanction> active, SanctionAction action) {
        return switch (action) {
            case COMMENT -> hasType(active, SanctionType.COMMENT_BAN);
            case PUBLISH -> hasType(active, SanctionType.MOMENT_BAN, SanctionType.PUBLISH_SUSPEND);
            case SUBMIT -> hasType(active, SanctionType.SUBMISSION_BAN, SanctionType.PUBLISH_SUSPEND);
            case MESSAGE -> hasType(active, SanctionType.MESSAGE_BAN);
            case LOGIN -> hasType(active, SanctionType.LOGIN_SUSPEND, SanctionType.PERMANENT_BAN);
        };
    }

    private static boolean hasType(List<CommunitySanction> sanctions, SanctionType... types) {
        return sanctions.stream().anyMatch(sanction -> {
            for (SanctionType type : types) {
                if (type.name().equals(sanction.getSanctionType())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static LocalDateTime latestExpiry(List<CommunitySanction> sanctions, SanctionType... types) {
        return sanctions.stream()
                .filter(sanction -> hasType(List.of(sanction), types))
                .map(CommunitySanction::getExpiresAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private void event(CommunitySanction sanction, String actorType, Long actorId, String eventType) {
        CommunitySanctionEvent event = new CommunitySanctionEvent();
        event.setSanctionId(sanction.getId());
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setEventType(eventType);
        event.setSnapshot("{\"status\":\"" + sanction.getStatus()
                + "\",\"type\":\"" + sanction.getSanctionType() + "\"}");
        event.setOccurredAt(now());
        events.insert(event);
    }

    private CommunityUser requireUser(Long userId) {
        CommunityUser user = users.selectById(requiredId(userId, "user"));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private static SanctionType parse(String rawType) {
        try {
            return SanctionType.valueOf(required(rawType, "sanction type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "无效的处罚类型");
        }
    }

    private static Long requiredId(Long value, String label) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, "缺少必填参数");
        }
        return value;
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "缺少必填参数");
        }
        return value.trim();
    }

    private static String normalizeNote(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String note = value.trim();
        if (note.length() > 1000) {
            throw new BusinessException(400, "说明文字过长");
        }
        return note;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
