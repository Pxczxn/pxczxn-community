package top.pxczxn.community.moderation.application;

import java.util.List;

public interface ContentRuleService {
    List<ContentRuleView> list(String status);
    ContentRuleView create(Long adminId, RuleCommand command);
    ContentRuleView update(Long ruleId, RuleCommand command);
    void delete(Long ruleId);
    record RuleCommand(String keyword, String contentScopes, String riskLevel, String hitAction, String description, Integer sortOrder, String status) { }
}
