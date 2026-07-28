/* Localize table comments and replace historical placeholder text. */
SET NAMES utf8mb4;

ALTER TABLE `article_collaboration_audit_event` COMMENT = '文章协作审计事件日志';
ALTER TABLE `article_collaboration_invitation` COMMENT = '文章范围协作邀请记录';
ALTER TABLE `article_collaborator` COMMENT = '已接受的文章协作者与公开署名';
ALTER TABLE `community_appeal` COMMENT = '社区举报处理决定申诉记录';
ALTER TABLE `community_appeal_event` COMMENT = '社区申诉事件日志（仅追加）';
ALTER TABLE `community_block` COMMENT = '社区用户拉黑关系';
ALTER TABLE `community_chat_message` COMMENT = '社区用户私聊消息';
ALTER TABLE `community_moment_moderation_event` COMMENT = '社区动态审核事件记录';
ALTER TABLE `community_report` COMMENT = '社区举报记录';
ALTER TABLE `community_report_event` COMMENT = '社区举报事件日志（仅追加）';
ALTER TABLE `content_keyword_rule` COMMENT = '内容审核可配置关键词规则';
ALTER TABLE `team` COMMENT = '团队博客元数据';
ALTER TABLE `team_application` COMMENT = '团队博客创建申请记录';
ALTER TABLE `team_audit_event` COMMENT = '团队审核事件日志（仅追加）';
ALTER TABLE `team_invitation` COMMENT = '团队成员邀请记录';
ALTER TABLE `team_member` COMMENT = '团队成员关系';
ALTER TABLE `team_permission` COMMENT = '团队角色权限';
ALTER TABLE `team_role` COMMENT = '团队业务角色';
ALTER TABLE `team_series` COMMENT = '团队文章系列及连载状态';
ALTER TABLE `team_series_article` COMMENT = '团队系列文章排序关系';
ALTER TABLE `team_submission` COMMENT = '个人文章投递团队发布记录';
