package top.pxczxn.community.moderation.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.moderation.model.ContentKeywordRule;
import top.pxczxn.community.moderation.persistence.ContentKeywordRuleMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentRuleServiceImpl implements ContentRuleService {
    private static final Set<String> SCOPES = Set.of("ARTICLE", "COMMENT", "MOMENT");
    private static final Set<String> RISKS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> ACTIONS = Set.of("BLOCK", "MANUAL_REVIEW", "WARN");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private final ContentKeywordRuleMapper mapper;

    @Override @Transactional(readOnly = true)
    public List<ContentRuleView> list(String status) {
        var query = Wrappers.<ContentKeywordRule>lambdaQuery().isNull(ContentKeywordRule::getDeletedAt);
        if (status != null && !status.isBlank()) query.eq(ContentKeywordRule::getStatus, enumValue(status, STATUSES, "status"));
        return mapper.selectList(query.orderByAsc(ContentKeywordRule::getSortOrder).orderByAsc(ContentKeywordRule::getId)).stream().map(ContentRuleView::from).toList();
    }

    @Override @Transactional
    public ContentRuleView create(Long adminId, RuleCommand command) {
        if (adminId == null || adminId <= 0) throw new BusinessException(401, "需要管理员登录");
        RuleFields fields = fields(command, false);
        ContentKeywordRule rule = new ContentKeywordRule();
        rule.setId(IdWorker.getId()); rule.setKeyword(fields.keyword); rule.setNormalizedKeyword(ArticleKeywordReviewEngine.normalizeForMatching(fields.keyword));
        rule.setSeverity(fields.action.equals("BLOCK") ? "BLOCK" : fields.action.equals("MANUAL_REVIEW") ? "REVIEW" : "WARN");
        rule.setContentScopes(fields.scopes); rule.setRiskLevel(fields.risk); rule.setHitAction(fields.action); rule.setDescription(fields.description); rule.setSortOrder(fields.sortOrder); rule.setStatus(fields.status); rule.setCreatedByAdminId(adminId); rule.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try { if (mapper.insert(rule) != 1) throw new BusinessException(500, "内容规则创建失败"); } catch (DuplicateKeyException exception) { throw new BusinessException(409, "该关键词已存在生效的规则"); }
        return ContentRuleView.from(rule);
    }

    @Override @Transactional
    public ContentRuleView update(Long ruleId, RuleCommand command) {
        ContentKeywordRule rule = require(ruleId); RuleFields fields = fields(command, true);
        var update = Wrappers.<ContentKeywordRule>lambdaUpdate().eq(ContentKeywordRule::getId, rule.getId()).isNull(ContentKeywordRule::getDeletedAt);
        if (fields.keyword != null) { update.set(ContentKeywordRule::getKeyword, fields.keyword).set(ContentKeywordRule::getNormalizedKeyword, ArticleKeywordReviewEngine.normalizeForMatching(fields.keyword)); }
        if (fields.scopes != null) update.set(ContentKeywordRule::getContentScopes, fields.scopes);
        if (fields.risk != null) update.set(ContentKeywordRule::getRiskLevel, fields.risk);
        if (fields.action != null) update.set(ContentKeywordRule::getHitAction, fields.action).set(ContentKeywordRule::getSeverity, fields.action.equals("BLOCK") ? "BLOCK" : fields.action.equals("MANUAL_REVIEW") ? "REVIEW" : "WARN");
        if (fields.description != null) update.set(ContentKeywordRule::getDescription, fields.description);
        if (fields.sortOrder != null) update.set(ContentKeywordRule::getSortOrder, fields.sortOrder);
        if (fields.status != null) update.set(ContentKeywordRule::getStatus, fields.status);
        try { mapper.update(null, update); } catch (DuplicateKeyException exception) { throw new BusinessException(409, "该关键词已存在生效的规则"); }
        return ContentRuleView.from(require(ruleId));
    }

    @Override @Transactional
    public void delete(Long ruleId) {
        ContentKeywordRule rule = require(ruleId);
        mapper.update(null, Wrappers.<ContentKeywordRule>lambdaUpdate().eq(ContentKeywordRule::getId, rule.getId()).set(ContentKeywordRule::getStatus, "DELETED").set(ContentKeywordRule::getDeletedAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    private ContentKeywordRule require(Long id) { if (id == null || id <= 0) throw new BusinessException(400, "规则编号无效"); ContentKeywordRule rule = mapper.selectById(id); if (rule == null || rule.getDeletedAt() != null) throw new BusinessException(404, "内容规则不存在"); return rule; }
    private static RuleFields fields(RuleCommand command, boolean partial) {
        if (command == null) throw new BusinessException(400, "规则内容不能为空");
        String keyword = command.keyword() == null ? null : text(command.keyword(), 200, "keyword");
        if (!partial && keyword == null) throw new BusinessException(400, "关键词不能为空");
        String scopes = command.contentScopes() == null ? (partial ? null : "ARTICLE,COMMENT,MOMENT") : scopes(command.contentScopes());
        String risk = command.riskLevel() == null ? (partial ? null : "MEDIUM") : enumValue(command.riskLevel(), RISKS, "risk level");
        String action = command.hitAction() == null ? (partial ? null : "WARN") : enumValue(command.hitAction(), ACTIONS, "hit action");
        String description = command.description() == null ? null : optional(command.description(), 500, "description");
        Integer sort = command.sortOrder(); if (sort != null && (sort < 0 || sort > 100000)) throw new BusinessException(400, "排序值无效");
        String status = command.status() == null ? (partial ? null : "ACTIVE") : enumValue(command.status(), STATUSES, "status");
        Integer resolvedSort = sort;
        if (!partial && resolvedSort == null) {
            resolvedSort = 0;
        }
        return new RuleFields(keyword, scopes, risk, action, description, resolvedSort, status);
    }
    private static String scopes(String raw) { String value = raw.trim().toUpperCase(Locale.ROOT); if (value.equals("ALL")) return "ARTICLE,COMMENT,MOMENT"; LinkedHashSet<String> result = new LinkedHashSet<>(); Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(scope -> { if (!SCOPES.contains(scope)) throw new BusinessException(400, "无效的内容范围"); result.add(scope); }); if (result.isEmpty()) throw new BusinessException(400, "至少需要选择一个内容范围"); return String.join(",", result); }
    private static String enumValue(String raw, Set<String> allowed, String label) { String value = raw.trim().toUpperCase(Locale.ROOT); if (!allowed.contains(value)) throw new BusinessException(400, label + "无效"); return value; }
    private static String text(String raw, int max, String label) { String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC); if (value.isBlank() || value.length() > max) throw new BusinessException(400, label + "无效"); return value; }
    private static String optional(String raw, int max, String label) { String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC); if (value.length() > max) throw new BusinessException(400, label + "无效"); return value.isBlank() ? null : value; }
    private record RuleFields(String keyword, String scopes, String risk, String action, String description, Integer sortOrder, String status) { }
}
