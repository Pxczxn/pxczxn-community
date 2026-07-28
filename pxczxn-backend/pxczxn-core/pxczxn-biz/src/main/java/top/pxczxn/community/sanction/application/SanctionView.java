package top.pxczxn.community.sanction.application;
import top.pxczxn.community.sanction.model.CommunitySanction;
import java.time.LocalDateTime;
public record SanctionView(Long id, String sanctionType, String reasonCode, String reasonNote, String status, LocalDateTime expiresAt) { static SanctionView from(CommunitySanction value) { return new SanctionView(value.getId(),value.getSanctionType(),value.getReasonCode(),value.getReasonNote(),value.getStatus(),value.getExpiresAt()); } }
