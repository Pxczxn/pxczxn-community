SET NAMES utf8mb4;

CREATE TABLE `community_creator_idea` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '创作灵感 ID',
    `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属社区用户 ID',
    `title` VARCHAR(160) NOT NULL COMMENT '灵感标题',
    `content` VARCHAR(4000) NOT NULL COMMENT '灵感内容',
    `tags_text` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '逗号分隔标签',
    `source_type` VARCHAR(24) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `deleted_at` DATETIME(3) NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_creator_idea_owner_created` (`owner_user_id`, `created_at`),
    CONSTRAINT `fk_creator_idea_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_creator_idea_source_type` CHECK (`source_type` IN ('MANUAL', 'ARTICLE', 'MOMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区作者创作灵感';
