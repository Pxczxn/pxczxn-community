/* Complete the data dictionary: every base table must carry a table comment. */
SET NAMES utf8mb4;

ALTER TABLE `coder_banner` COMMENT = '首页轮播横幅配置';
ALTER TABLE `community_abuse_event` COMMENT = '社区反滥用请求事件日志';
ALTER TABLE `community_abuse_window` COMMENT = '社区反滥用限流时间窗口';
ALTER TABLE `community_sanction` COMMENT = '社区用户处罚记录';
ALTER TABLE `community_sanction_event` COMMENT = '社区处罚处理事件记录';
ALTER TABLE `community_sanction_rate_limit` COMMENT = '社区处罚请求限流记录';
ALTER TABLE `creator_analytics_event` COMMENT = '创作者数据统计事件';
ALTER TABLE `editorial_collection` COMMENT = '平台精选内容集合';
ALTER TABLE `editorial_collection_item` COMMENT = '平台精选集合内容关联';
ALTER TABLE `pxczxn_schema_version` COMMENT = '数据库迁移版本记录';
