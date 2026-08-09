SET NAMES utf8mb4;

-- 回滚 V075：删除系列轻量阅读进度表。
-- 注意：阅读进度为用户行为数据，删除后不可恢复，执行前请确认已备份。

DROP TABLE IF EXISTS `series_reading_progress`;
