package top.pxczxn.community.moderation.application;

import top.pxczxn.community.moderation.model.ContentKeywordRule;

public record ContentRuleView(Long id, String keyword, String severity, String contentScopes, String riskLevel, String hitAction, String status, String description, Integer sortOrder) {
    static ContentRuleView from(ContentKeywordRule value) {
        return new ContentRuleView(value.getId(), value.getKeyword(), value.getSeverity(), value.getContentScopes(), value.getRiskLevel(), value.getHitAction(), value.getStatus(), value.getDescription(), value.getSortOrder());
    }
}
