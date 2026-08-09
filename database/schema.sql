/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : pxczxn_community

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 09/08/2026 04:26:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for article
-- ----------------------------
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `blog_id` bigint UNSIGNED NOT NULL COMMENT '博客 ID',
  `author_user_id` bigint UNSIGNED NOT NULL COMMENT '作者用户 ID',
  `category_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '分类 ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `slug` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL 标识',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摘要',
  `cover_file_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '封面文件 ID',
  `content_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：content_mode',
  `visibility` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLIC' COMMENT '可见范围',
  `publish_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MANUAL' COMMENT '业务字段：publish_method',
  `publish_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '业务字段：publish_status',
  `review_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOT_SUBMITTED' COMMENT '业务字段：review_status',
  `current_version_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '当前编辑版本；循环关系由应用事务维护',
  `published_version_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '当前公开版本；循环关系由应用事务维护',
  `review_version_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '当前审核固定版本',
  `scheduled_publish_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：scheduled_publish_at',
  `published_at` datetime(3) NULL DEFAULT NULL COMMENT '发布时间',
  `canonical_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：canonical_path',
  `view_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `favorite_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：favorite_count',
  `comment_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论次数',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_blog_slug`(`blog_id` ASC, `slug` ASC) USING BTREE,
  INDEX `idx_article_author_status`(`author_user_id` ASC, `publish_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_article_blog_public`(`blog_id` ASC, `publish_status` ASC, `visibility` ASC, `published_at` ASC) USING BTREE,
  INDEX `idx_article_review_queue`(`review_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_article_category_public`(`category_id` ASC, `publish_status` ASC, `published_at` ASC) USING BTREE,
  INDEX `idx_article_public_search`(`visibility` ASC, `publish_status` ASC, `deleted_at` ASC, `published_at` ASC) USING BTREE,
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `blog_category` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_article_content_mode` CHECK (`content_mode` in (_utf8mb4'RICH_TEXT',_utf8mb4'MARKDOWN')),
  CONSTRAINT `chk_article_publish_method` CHECK (`publish_method` in (_utf8mb4'IMMEDIATE',_utf8mb4'SCHEDULED',_utf8mb4'MANUAL')),
  CONSTRAINT `chk_article_publish_status` CHECK (`publish_status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING_REVIEW',_utf8mb4'APPROVED',_utf8mb4'SCHEDULED',_utf8mb4'PUBLISHED',_utf8mb4'HIDDEN',_utf8mb4'TAKEN_DOWN',_utf8mb4'PUBLISH_FAILED',_utf8mb4'DELETED')),
  CONSTRAINT `chk_article_review_status` CHECK (`review_status` in (_utf8mb4'NOT_SUBMITTED',_utf8mb4'QUEUED',_utf8mb4'AUTO_REVIEWING',_utf8mb4'MANUAL_REVIEWING',_utf8mb4'APPROVED',_utf8mb4'REVISION_REQUIRED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED',_utf8mb4'EXPIRED')),
  CONSTRAINT `chk_article_visibility` CHECK (`visibility` in (_utf8mb4'PUBLIC',_utf8mb4'PRIVATE',_utf8mb4'FOLLOWERS_ONLY',_utf8mb4'UNLISTED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章稳定元数据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article_collaboration_audit_event
-- ----------------------------
DROP TABLE IF EXISTS `article_collaboration_audit_event`;
CREATE TABLE `article_collaboration_audit_event`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `actor_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：actor_user_id',
  `event_type` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `target_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '目标用户 ID',
  `before_snapshot` json NULL COMMENT '变更前快照',
  `after_snapshot` json NULL COMMENT '变更后快照',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_article_collab_audit_article`(`article_id` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_article_collab_audit_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章协作审计事件日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article_collaboration_invitation
-- ----------------------------
DROP TABLE IF EXISTS `article_collaboration_invitation`;
CREATE TABLE `article_collaboration_invitation`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `invitee_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：invitee_user_id',
  `invited_by_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：invited_by_user_id',
  `contribution_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：contribution_type',
  `can_edit` tinyint NOT NULL DEFAULT 0 COMMENT '是否可编辑',
  `attribution_order` smallint UNSIGNED NOT NULL COMMENT '业务字段：attribution_order',
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '消息内容',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '业务状态',
  `idempotency_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等键',
  `expires_at` datetime(3) NOT NULL COMMENT '到期时间',
  `responded_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：responded_at',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `active_invitee_user_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'PENDING') then `invitee_user_id` else NULL end)) STORED COMMENT '业务字段：active_invitee_user_id' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_collab_invitation_idempotency`(`idempotency_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_article_collab_active_invitee`(`article_id` ASC, `active_invitee_user_id` ASC) USING BTREE,
  INDEX `idx_article_collab_invitee`(`invitee_user_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_article_collab_article`(`article_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_article_collab_invitation_inviter`(`invited_by_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_article_collab_invitation_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_collab_invitation_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_collab_invitation_inviter` FOREIGN KEY (`invited_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_article_collab_invitation_edit` CHECK (`can_edit` in (0,1)),
  CONSTRAINT `chk_article_collab_invitation_status` CHECK (`status` in (_utf8mb4'PENDING',_utf8mb4'ACCEPTED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED',_utf8mb4'EXPIRED')),
  CONSTRAINT `chk_article_collab_invitation_type` CHECK (`contribution_type` in (_utf8mb4'CO_AUTHOR',_utf8mb4'RESEARCH',_utf8mb4'REVIEW',_utf8mb4'ILLUSTRATION'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章范围协作邀请记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article_collaborator
-- ----------------------------
DROP TABLE IF EXISTS `article_collaborator`;
CREATE TABLE `article_collaborator`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户 ID',
  `invitation_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：invitation_id',
  `contribution_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：contribution_type',
  `can_edit` tinyint NOT NULL DEFAULT 0 COMMENT '是否可编辑',
  `attribution_order` smallint UNSIGNED NOT NULL COMMENT '业务字段：attribution_order',
  `accepted_at` datetime(3) NOT NULL COMMENT '业务字段：accepted_at',
  `revoked_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：revoked_at',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `active_user_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when (`revoked_at` is null) then `user_id` else NULL end)) STORED COMMENT '业务字段：active_user_id' NULL,
  `active_attribution_order` smallint UNSIGNED GENERATED ALWAYS AS ((case when (`revoked_at` is null) then `attribution_order` else NULL end)) STORED COMMENT '业务字段：active_attribution_order' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_collaborator_invitation`(`invitation_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_article_collaborator_active_user`(`article_id` ASC, `active_user_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_article_collaborator_active_order`(`article_id` ASC, `active_attribution_order` ASC) USING BTREE,
  INDEX `idx_article_collaborator_article`(`article_id` ASC, `attribution_order` ASC) USING BTREE,
  INDEX `idx_article_collaborator_user`(`user_id` ASC, `revoked_at` ASC) USING BTREE,
  CONSTRAINT `fk_article_collaborator_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_collaborator_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `article_collaboration_invitation` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_collaborator_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_article_collaborator_edit` CHECK (`can_edit` in (0,1)),
  CONSTRAINT `chk_article_collaborator_type` CHECK (`contribution_type` in (_utf8mb4'CO_AUTHOR',_utf8mb4'RESEARCH',_utf8mb4'REVIEW',_utf8mb4'ILLUSTRATION'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '已接受的文章协作者与公开署名' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article_publish_task
-- ----------------------------
DROP TABLE IF EXISTS `article_publish_task`;
CREATE TABLE `article_publish_task`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `article_version_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：article_version_id',
  `scheduled_publish_at` datetime(3) NOT NULL COMMENT '业务字段：scheduled_publish_at',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WAITING' COMMENT '业务状态',
  `attempt_count` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '尝试次数',
  `max_attempts` tinyint UNSIGNED NOT NULL DEFAULT 3 COMMENT '业务字段：max_attempts',
  `next_attempt_at` datetime(3) NOT NULL COMMENT '业务字段：next_attempt_at',
  `last_error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：last_error_code',
  `last_error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：last_error_message',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `completed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：completed_at',
  `active_article_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'WAITING',_utf8mb4'RUNNING',_utf8mb4'RETRY_WAIT')) then `article_id` else NULL end)) STORED COMMENT '业务字段：active_article_id' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_publish_task_active`(`active_article_id` ASC) USING BTREE,
  INDEX `idx_article_publish_task_due`(`status` ASC, `next_attempt_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_article_publish_task_article`(`article_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `fk_article_publish_task_version`(`article_version_id` ASC) USING BTREE,
  CONSTRAINT `fk_article_publish_task_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_publish_task_version` FOREIGN KEY (`article_version_id`) REFERENCES `article_version` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_article_publish_task_attempts` CHECK ((`attempt_count` <= `max_attempts`) and (`max_attempts` between 1 and 10)),
  CONSTRAINT `chk_article_publish_task_status` CHECK (`status` in (_utf8mb4'WAITING',_utf8mb4'RUNNING',_utf8mb4'RETRY_WAIT',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'CANCELLED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章定时发布持久任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article_tag
-- ----------------------------
DROP TABLE IF EXISTS `article_tag`;
CREATE TABLE `article_tag`  (
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `tag_id` bigint UNSIGNED NOT NULL COMMENT '标签 ID',
  `sort_order` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`article_id`, `tag_id`) USING BTREE,
  INDEX `idx_article_tag_tag`(`tag_id` ASC, `article_id` ASC) USING BTREE,
  CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `platform_tag` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_article_tag_sort_order` CHECK (`sort_order` between 0 and 4)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章与平台标签关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article_version
-- ----------------------------
DROP TABLE IF EXISTS `article_version`;
CREATE TABLE `article_version`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `version_no` int UNSIGNED NOT NULL COMMENT '业务字段：version_no',
  `content_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：content_mode',
  `rich_text_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Tiptap/ProseMirror JSON',
  `markdown_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '业务字段：markdown_content',
  `rendered_html` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务端安全渲染缓存',
  `plain_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：plain_text',
  `toc_json` json NULL COMMENT '业务字段：toc_json',
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：content_hash',
  `word_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：word_count',
  `reading_time_minutes` int UNSIGNED NOT NULL DEFAULT 1 COMMENT '业务字段：reading_time_minutes',
  `created_by_user_id` bigint UNSIGNED NOT NULL COMMENT '创建用户 ID',
  `creation_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MANUAL_SAVE' COMMENT '业务字段：creation_type',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_version_number`(`article_id` ASC, `version_no` ASC) USING BTREE,
  INDEX `idx_article_version_created`(`article_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_article_version_creator`(`created_by_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_article_version_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_article_version_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_article_version_content_mode` CHECK (`content_mode` in (_utf8mb4'RICH_TEXT',_utf8mb4'MARKDOWN')),
  CONSTRAINT `chk_article_version_single_source` CHECK (((`content_mode` = _utf8mb4'RICH_TEXT') and (`rich_text_json` is not null) and (`markdown_content` is null)) or ((`content_mode` = _utf8mb4'MARKDOWN') and (`markdown_content` is not null) and (`rich_text_json` is null)))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章完整正文快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog
-- ----------------------------
DROP TABLE IF EXISTS `blog`;
CREATE TABLE `blog`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `blog_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'PERSONAL 或 TEAM',
  `owner_user_id` bigint UNSIGNED NOT NULL COMMENT '个人博主或团队发起人',
  `name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `slug` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL 标识',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摘要',
  `avatar_file_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：avatar_file_id',
  `background_file_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：background_file_id',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态',
  `article_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '文章数量',
  `follower_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：follower_count',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  `personal_owner_user_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when (`blog_type` = _utf8mb4'PERSONAL') then `owner_user_id` else NULL end)) STORED COMMENT '业务字段：personal_owner_user_id' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_slug`(`slug` ASC) USING BTREE,
  UNIQUE INDEX `uk_blog_personal_owner`(`personal_owner_user_id` ASC) USING BTREE,
  INDEX `idx_blog_owner_type`(`owner_user_id` ASC, `blog_type` ASC) USING BTREE,
  INDEX `idx_blog_status_created`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_blog_public_search`(`status` ASC, `deleted_at` ASC, `updated_at` ASC) USING BTREE,
  CONSTRAINT `fk_blog_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_blog_status` CHECK (`status` in (_utf8mb4'ACTIVE',_utf8mb4'HIDDEN',_utf8mb4'FROZEN',_utf8mb4'CLOSED',_utf8mb4'DELETED')),
  CONSTRAINT `chk_blog_type` CHECK (`blog_type` in (_utf8mb4'PERSONAL',_utf8mb4'TEAM'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '个人博客与团队博客' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_category
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `blog_id` bigint UNSIGNED NOT NULL COMMENT '博客 ID',
  `name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `slug` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL 标识',
  `description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认',
  `article_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '文章数量',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  `default_blog_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when ((`is_default` = 1) and (`deleted_at` is null)) then `blog_id` else NULL end)) STORED COMMENT '业务字段：default_blog_id' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_category_slug`(`blog_id` ASC, `slug` ASC) USING BTREE,
  UNIQUE INDEX `uk_blog_category_default`(`default_blog_id` ASC) USING BTREE,
  INDEX `idx_blog_category_sort`(`blog_id` ASC, `sort_order` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_blog_category_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_blog_category_default` CHECK (`is_default` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '博客一级分类' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_setting
-- ----------------------------
DROP TABLE IF EXISTS `blog_setting`;
CREATE TABLE `blog_setting`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `blog_id` bigint UNSIGNED NOT NULL COMMENT '博客 ID',
  `comment_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALL_LOGGED_IN' COMMENT '业务字段：comment_scope',
  `default_visibility` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLIC' COMMENT '业务字段：default_visibility',
  `allow_repost` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALLOW' COMMENT '业务字段：allow_repost',
  `theme_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'default' COMMENT '业务字段：theme_key',
  `theme_config_json` json NULL COMMENT '安全主题配置，不允许任意 CSS/JS',
  `seo_title` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：seo_title',
  `seo_description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：seo_description',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_blog_setting_blog`(`blog_id` ASC) USING BTREE,
  CONSTRAINT `fk_blog_setting_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_blog_setting_comment_scope` CHECK (`comment_scope` in (_utf8mb4'ALL_LOGGED_IN',_utf8mb4'FOLLOWERS_ONLY',_utf8mb4'MUTUAL_ONLY',_utf8mb4'BLOGGER_FOLLOWING',_utf8mb4'TEAM_FOLLOWERS',_utf8mb4'TEAM_MEMBERS',_utf8mb4'DISABLED')),
  CONSTRAINT `chk_blog_setting_repost` CHECK (`allow_repost` in (_utf8mb4'ALLOW',_utf8mb4'APPROVAL_REQUIRED',_utf8mb4'DISALLOW')),
  CONSTRAINT `chk_blog_setting_visibility` CHECK (`default_visibility` in (_utf8mb4'PUBLIC',_utf8mb4'PRIVATE',_utf8mb4'FOLLOWERS_ONLY',_utf8mb4'UNLISTED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '博客设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for coder_banner
-- ----------------------------
DROP TABLE IF EXISTS `coder_banner`;
CREATE TABLE `coder_banner`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：image',
  `link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：link',
  `tag` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：tag',
  `btn_text` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：btn_text',
  `sort` int NULL DEFAULT 0 COMMENT '业务字段：sort',
  `status` tinyint NULL DEFAULT 1 COMMENT '业务状态',
  `position` tinyint NULL DEFAULT 1 COMMENT '业务字段：position',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '首页轮播横幅配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_abuse_event
-- ----------------------------
DROP TABLE IF EXISTS `community_abuse_event`;
CREATE TABLE `community_abuse_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `actor_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：actor_key',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `decision` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处理决定',
  `threshold` int UNSIGNED NOT NULL COMMENT '业务字段：threshold',
  `window_seconds` int UNSIGNED NOT NULL COMMENT '业务字段：window_seconds',
  `attempt_count` int UNSIGNED NOT NULL COMMENT '尝试次数',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_abuse_event_actor_time`(`actor_key` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `chk_abuse_event_decision` CHECK (`decision` in (_utf8mb4'ALLOW',_utf8mb4'REJECT'))
) ENGINE = InnoDB AUTO_INCREMENT = 1017 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区反滥用请求事件日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_abuse_window
-- ----------------------------
DROP TABLE IF EXISTS `community_abuse_window`;
CREATE TABLE `community_abuse_window`  (
  `actor_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：actor_key',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `window_started_at` datetime(3) NOT NULL COMMENT '业务字段：window_started_at',
  `attempt_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '尝试次数',
  `rejected_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：rejected_count',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`actor_key`, `action_type`) USING BTREE,
  INDEX `idx_abuse_window_updated`(`updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区反滥用限流时间窗口' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_account_enforcement_appeal
-- ----------------------------
DROP TABLE IF EXISTS `community_account_enforcement_appeal`;
CREATE TABLE `community_account_enforcement_appeal`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `case_id` bigint UNSIGNED NOT NULL COMMENT '处置申请 ID',
  `appellant_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：appellant_user_id',
  `statement` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申诉陈述',
  `evidence_snapshot` json NULL COMMENT '证据快照',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED,UNDER_REVIEW,UPHELD,MODIFIED,REVOKED,FINAL',
  `reviewed_by_admin_id` bigint NULL DEFAULT NULL COMMENT '审核管理员 ID',
  `review_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核意见',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '审核时间',
  `primary_reviewed_by_admin_id` bigint NULL DEFAULT NULL COMMENT '业务字段：primary_reviewed_by_admin_id',
  `primary_decision` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：primary_decision',
  `primary_review_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：primary_review_note',
  `primary_reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：primary_reviewed_at',
  `final_reviewed_by_admin_id` bigint NULL DEFAULT NULL COMMENT '终审管理员 ID',
  `final_decision` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：final_decision',
  `final_review_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：final_review_note',
  `final_reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：final_reviewed_at',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account_enforcement_appeal_queue`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_account_enforcement_appeal_case`(`case_id` ASC) USING BTREE,
  INDEX `fk_account_enforcement_appeal_user`(`appellant_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_account_enforcement_appeal_case` FOREIGN KEY (`case_id`) REFERENCES `community_account_enforcement_case` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_account_enforcement_appeal_user` FOREIGN KEY (`appellant_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_account_enforcement_appeal_status` CHECK (`status` in (_utf8mb4'SUBMITTED',_utf8mb4'UNDER_REVIEW',_utf8mb4'UPHELD',_utf8mb4'MODIFIED',_utf8mb4'REVOKED',_utf8mb4'FINAL',_utf8mb4'PRIMARY_REVIEWED',_utf8mb4'FINALIZED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账号强制措施申诉' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_account_enforcement_case
-- ----------------------------
DROP TABLE IF EXISTS `community_account_enforcement_case`;
CREATE TABLE `community_account_enforcement_case`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `target_user_id` bigint UNSIGNED NOT NULL COMMENT '目标用户 ID',
  `measure_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'TEMP_FREEZE,LONG_FREEZE,DATA_CLEANUP,ACCOUNT_DELETE',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'DRAFT,SUBMITTED,UNDER_REVIEW,APPROVED,PENDING_EXECUTION,ACTIVE,APPEAL_WINDOW,APPEALED,FINALIZED,REJECTED,REVOKED,EXECUTION_FAILED',
  `reason_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准原因代码',
  `user_visible_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '面向用户的处理说明',
  `internal_reason` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内部处理说明',
  `evidence_snapshot` json NOT NULL COMMENT '证据快照',
  `cleanup_scope` json NULL COMMENT '数据清理范围',
  `source_report_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '来源举报 ID',
  `requested_by_admin_id` bigint NULL DEFAULT NULL COMMENT '申请提交管理员 ID',
  `requested_by_team_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：requested_by_team_id',
  `requested_at` datetime(3) NOT NULL COMMENT '申请提交时间',
  `starts_at` datetime(3) NULL DEFAULT NULL COMMENT '开始时间',
  `expires_at` datetime(3) NULL DEFAULT NULL COMMENT '到期时间',
  `appeal_allowed` tinyint(1) NOT NULL DEFAULT 1 COMMENT '业务字段：appeal_allowed',
  `appeal_deadline_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：appeal_deadline_at',
  `execute_after` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：execute_after',
  `finalized_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：finalized_at',
  `lock_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account_enforcement_queue`(`status` ASC, `measure_type` ASC, `requested_at` ASC) USING BTREE,
  INDEX `idx_account_enforcement_target`(`target_user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_account_enforcement_target` FOREIGN KEY (`target_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_account_enforcement_measure` CHECK (`measure_type` in (_utf8mb4'TEMP_FREEZE',_utf8mb4'LONG_FREEZE',_utf8mb4'DATA_CLEANUP',_utf8mb4'ACCOUNT_DELETE',_utf8mb4'PASSWORD_RESET',_utf8mb4'SECURITY_LOGOUT',_utf8mb4'ACCOUNT_LOCK',_utf8mb4'ACCOUNT_UNLOCK',_utf8mb4'FREEZE_EXTEND',_utf8mb4'FREEZE_RELEASE',_utf8mb4'LOGIN_BLOCK',_utf8mb4'LOGIN_RESTORE',_utf8mb4'ACCOUNT_DEACTIVATE',_utf8mb4'ACCOUNT_RESTORE')),
  CONSTRAINT `chk_account_enforcement_requester` CHECK ((`requested_by_admin_id` is not null) or (`requested_by_team_id` is not null)),
  CONSTRAINT `chk_account_enforcement_status` CHECK (`status` in (_utf8mb4'DRAFT',_utf8mb4'SUBMITTED',_utf8mb4'UNDER_REVIEW',_utf8mb4'APPROVED',_utf8mb4'PENDING_EXECUTION',_utf8mb4'ACTIVE',_utf8mb4'APPEAL_WINDOW',_utf8mb4'APPEALED',_utf8mb4'FINALIZED',_utf8mb4'REJECTED',_utf8mb4'REVOKED',_utf8mb4'EXECUTION_FAILED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账号强制措施申请与延迟删除工作流' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_account_enforcement_event
-- ----------------------------
DROP TABLE IF EXISTS `community_account_enforcement_event`;
CREATE TABLE `community_account_enforcement_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `case_id` bigint UNSIGNED NOT NULL COMMENT '处置申请 ID',
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人类型',
  `actor_id` bigint NULL DEFAULT NULL COMMENT '操作人 ID',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `snapshot` json NULL COMMENT '数据快照',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account_enforcement_event_case`(`case_id` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_account_enforcement_event_case` FOREIGN KEY (`case_id`) REFERENCES `community_account_enforcement_case` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账号强制措施审计日志（仅追加）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_account_enforcement_review
-- ----------------------------
DROP TABLE IF EXISTS `community_account_enforcement_review`;
CREATE TABLE `community_account_enforcement_review`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `case_id` bigint UNSIGNED NOT NULL COMMENT '处置申请 ID',
  `stage` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'INITIAL,SECONDARY,FINAL',
  `reviewer_admin_id` bigint NOT NULL COMMENT '审核管理员 ID',
  `decision` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'APPROVE,REJECT,RETURN_FOR_EVIDENCE,REVOKE,UPHOLD',
  `review_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '审核意见',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account_enforcement_reviewer_stage`(`case_id` ASC, `stage` ASC, `reviewer_admin_id` ASC) USING BTREE,
  INDEX `idx_account_enforcement_review_case`(`case_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_account_enforcement_review_case` FOREIGN KEY (`case_id`) REFERENCES `community_account_enforcement_case` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账号强制措施审核记录（仅追加）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_appeal
-- ----------------------------
DROP TABLE IF EXISTS `community_appeal`;
CREATE TABLE `community_appeal`  (
  `id` bigint UNSIGNED NOT NULL COMMENT 'Appeal ID',
  `report_id` bigint UNSIGNED NOT NULL COMMENT 'Resolved report being appealed',
  `appellant_user_id` bigint UNSIGNED NOT NULL COMMENT 'Owner of the reported target',
  `appeal_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：appeal_reason',
  `evidence_json` json NULL COMMENT '业务字段：evidence_json',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '业务状态',
  `reviewer_admin_id` bigint NULL DEFAULT NULL COMMENT '审核管理员 ID',
  `review_note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '审核时间',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_community_appeal_report_appellant`(`report_id` ASC, `appellant_user_id` ASC) USING BTREE,
  INDEX `idx_community_appeal_queue`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_community_appeal_appellant`(`appellant_user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_community_appeal_appellant` FOREIGN KEY (`appellant_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_appeal_report` FOREIGN KEY (`report_id`) REFERENCES `community_report` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_community_appeal_status` CHECK (`status` in (_utf8mb4'PENDING',_utf8mb4'UPHELD',_utf8mb4'REVOKED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区举报处理决定申诉记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_appeal_event
-- ----------------------------
DROP TABLE IF EXISTS `community_appeal_event`;
CREATE TABLE `community_appeal_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `appeal_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：appeal_id',
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'USER or ADMIN',
  `actor_id` bigint NULL DEFAULT NULL COMMENT '操作人 ID',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'CREATED, UPHELD, REVOKED',
  `before_snapshot` json NULL COMMENT '变更前快照',
  `after_snapshot` json NULL COMMENT '变更后快照',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_community_appeal_event_appeal`(`appeal_id` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_community_appeal_event_appeal` FOREIGN KEY (`appeal_id`) REFERENCES `community_appeal` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区申诉事件日志（仅追加）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_block
-- ----------------------------
DROP TABLE IF EXISTS `community_block`;
CREATE TABLE `community_block`  (
  `id` bigint UNSIGNED NOT NULL COMMENT 'Block ID',
  `blocker_user_id` bigint UNSIGNED NOT NULL COMMENT 'Community user who created the block',
  `target_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'USER, BLOG, TAG, CHAT',
  `target_id` bigint UNSIGNED NOT NULL COMMENT 'Blocked resource or chat peer user ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_community_block_target`(`blocker_user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_community_block_blocker`(`blocker_user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_community_block_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  CONSTRAINT `fk_community_block_blocker` FOREIGN KEY (`blocker_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_community_block_type` CHECK (`target_type` in (_utf8mb4'USER',_utf8mb4'BLOG',_utf8mb4'TAG',_utf8mb4'CHAT'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区用户拉黑关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `community_chat_message`;
CREATE TABLE `community_chat_message`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `sender_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：sender_user_id',
  `recipient_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：recipient_user_id',
  `content_text` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文本内容',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SENT' COMMENT '业务状态',
  `read_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：read_at',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  `sender_deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：sender_deleted_at',
  `recipient_deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：recipient_deleted_at',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_community_chat_recipient`(`recipient_user_id` ASC, `status` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_chat_pair`(`sender_user_id` ASC, `recipient_user_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_chat_sender_visibility`(`sender_user_id` ASC, `sender_deleted_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_chat_recipient_visibility`(`recipient_user_id` ASC, `recipient_deleted_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_community_chat_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_chat_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_community_chat_distinct_users` CHECK (`sender_user_id` <> `recipient_user_id`),
  CONSTRAINT `chk_community_chat_status` CHECK (`status` in (_utf8mb4'SENT',_utf8mb4'READ',_utf8mb4'DELETED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区用户私聊消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_comment
-- ----------------------------
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `author_user_id` bigint UNSIGNED NOT NULL COMMENT '作者用户 ID',
  `target_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ARTICLE/MOMENT',
  `target_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：target_id',
  `root_comment_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：root_comment_id',
  `parent_comment_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：parent_comment_id',
  `reply_to_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：reply_to_user_id',
  `content_text` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文本内容',
  `rendered_html` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：rendered_html',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLISHED' COMMENT '业务状态',
  `like_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_community_comment_target`(`target_type` ASC, `target_id` ASC, `status` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_comment_root`(`root_comment_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_comment_author`(`author_user_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `fk_community_comment_parent`(`parent_comment_id` ASC) USING BTREE,
  INDEX `fk_community_comment_reply_user`(`reply_to_user_id` ASC) USING BTREE,
  INDEX `idx_comment_admin_status_created`(`status` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_community_comment_author` FOREIGN KEY (`author_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `community_comment` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_comment_reply_user` FOREIGN KEY (`reply_to_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_comment_root` FOREIGN KEY (`root_comment_id`) REFERENCES `community_comment` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_community_comment_status` CHECK (`status` in (_utf8mb4'PENDING_REVIEW',_utf8mb4'PUBLISHED',_utf8mb4'HIDDEN_BY_AUTHOR',_utf8mb4'HIDDEN_BY_BLOG',_utf8mb4'DELETED_BY_USER',_utf8mb4'TAKEN_DOWN',_utf8mb4'SPAM')),
  CONSTRAINT `chk_community_comment_target` CHECK (`target_type` in (_utf8mb4'ARTICLE',_utf8mb4'MOMENT'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章与动态的两层评论和回复' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_comment_moderation_event
-- ----------------------------
DROP TABLE IF EXISTS `community_comment_moderation_event`;
CREATE TABLE `community_comment_moderation_event`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `comment_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：comment_id',
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作动作',
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人类型',
  `actor_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：actor_user_id',
  `actor_admin_id` bigint NULL DEFAULT NULL COMMENT '业务字段：actor_admin_id',
  `previous_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：previous_status',
  `new_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：new_status',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：reason',
  `metadata_json` json NULL COMMENT '元数据 JSON',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_comment_moderation_comment_created`(`comment_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_comment_moderation_action_created`(`action` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `fk_comment_moderation_actor_user`(`actor_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_comment_moderation_actor_user` FOREIGN KEY (`actor_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_comment_moderation_comment` FOREIGN KEY (`comment_id`) REFERENCES `community_comment` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_comment_moderation_action_v2` CHECK (`action` in (_utf8mb4'AUTO_PUBLISHED',_utf8mb4'AUTO_REVIEW_QUEUED',_utf8mb4'USER_DELETED',_utf8mb4'AUTHOR_HIDDEN',_utf8mb4'BLOG_HIDDEN',_utf8mb4'PLATFORM_APPROVED',_utf8mb4'PLATFORM_REJECTED',_utf8mb4'PLATFORM_TAKEN_DOWN',_utf8mb4'PLATFORM_RESTORED')),
  CONSTRAINT `chk_comment_moderation_actor_type` CHECK (`actor_type` in (_utf8mb4'USER',_utf8mb4'ADMIN',_utf8mb4'SYSTEM'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论提交、删除、隐藏和平台治理的不可变事件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_content_like
-- ----------------------------
DROP TABLE IF EXISTS `community_content_like`;
CREATE TABLE `community_content_like`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户 ID',
  `target_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ARTICLE/MOMENT/COMMENT',
  `target_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：target_id',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_content_like_user_target`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_content_like_target_created`(`target_type` ASC, `target_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_content_like_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_content_like_target` CHECK (`target_type` in (_utf8mb4'ARTICLE',_utf8mb4'MOMENT',_utf8mb4'COMMENT'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章、动态、评论与回复点赞关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_file_reference
-- ----------------------------
DROP TABLE IF EXISTS `community_file_reference`;
CREATE TABLE `community_file_reference`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `file_id` bigint UNSIGNED NOT NULL COMMENT '文件 ID',
  `owner_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '所属用户 ID',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标类型',
  `target_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：target_id',
  `usage_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：usage_type',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_reference_target`(`file_id` ASC, `target_type` ASC, `target_id` ASC, `usage_type` ASC) USING BTREE,
  INDEX `idx_file_reference_target`(`target_type` ASC, `target_id` ASC, `deleted_at` ASC) USING BTREE,
  INDEX `idx_file_reference_owner`(`owner_user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_file_reference_file` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_file_reference_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区文件业务引用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_follow
-- ----------------------------
DROP TABLE IF EXISTS `community_follow`;
CREATE TABLE `community_follow`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `follower_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：follower_user_id',
  `target_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BLOG/PLATFORM_TAG/SERIES',
  `target_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：target_id',
  `notification_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALL' COMMENT '业务字段：notification_level',
  `special_follow` tinyint(1) NOT NULL DEFAULT 0 COMMENT '仅个人博客允许特别关注',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_community_follow_target`(`follower_user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_community_follow_target_created`(`target_type` ASC, `target_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_follow_user_created`(`follower_user_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_community_follow_user` FOREIGN KEY (`follower_user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_community_follow_notification` CHECK (`notification_level` in (_utf8mb4'ALL',_utf8mb4'IMPORTANT',_utf8mb4'MUTED')),
  CONSTRAINT `chk_community_follow_special` CHECK (`special_follow` in (0,1)),
  CONSTRAINT `chk_community_follow_target_type` CHECK (`target_type` in (_utf8mb4'BLOG',_utf8mb4'PLATFORM_TAG',_utf8mb4'SERIES'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户对博客、平台标签和系列的关注关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_moment
-- ----------------------------
DROP TABLE IF EXISTS `community_moment`;
CREATE TABLE `community_moment`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `actor_user_id` bigint UNSIGNED NOT NULL COMMENT '真实操作者',
  `blog_id` bigint UNSIGNED NOT NULL COMMENT '发布身份',
  `moment_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TEXT' COMMENT '业务字段：moment_type',
  `text_content` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：text_content',
  `rendered_html` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '业务字段：rendered_html',
  `link_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：link_url',
  `article_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '文章 ID',
  `repost_moment_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：repost_moment_id',
  `visibility` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLIC' COMMENT '可见范围',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLISHED' COMMENT '业务状态',
  `like_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `favorite_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：favorite_count',
  `comment_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论次数',
  `repost_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：repost_count',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_community_moment_blog_created`(`blog_id` ASC, `status` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_moment_actor_created`(`actor_user_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_community_moment_article`(`article_id` ASC) USING BTREE,
  INDEX `fk_community_moment_repost`(`repost_moment_id` ASC) USING BTREE,
  INDEX `idx_community_moment_public_feed`(`status` ASC, `visibility` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_moment_public_search`(`visibility` ASC, `status` ASC, `deleted_at` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_community_moment_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_moment_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_moment_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_community_moment_repost` FOREIGN KEY (`repost_moment_id`) REFERENCES `community_moment` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_community_moment_link_required` CHECK ((`moment_type` not in (_utf8mb4'LINK',_utf8mb4'VIDEO_LINK')) or ((`link_url` is not null) and (char_length(trim(`link_url`)) > 0))),
  CONSTRAINT `chk_community_moment_quote_text` CHECK ((`moment_type` <> _utf8mb4'QUOTE') or ((`text_content` is not null) and (char_length(trim(`text_content`)) > 0))),
  CONSTRAINT `chk_community_moment_repost_text` CHECK ((`moment_type` <> _utf8mb4'REPOST') or (`text_content` is null) or (char_length(trim(`text_content`)) = 0)),
  CONSTRAINT `chk_community_moment_status` CHECK (`status` in (_utf8mb4'PENDING_REVIEW',_utf8mb4'PUBLISHED',_utf8mb4'HIDDEN',_utf8mb4'TAKEN_DOWN',_utf8mb4'DELETED')),
  CONSTRAINT `chk_community_moment_type` CHECK (`moment_type` in (_utf8mb4'TEXT',_utf8mb4'IMAGE',_utf8mb4'LINK',_utf8mb4'ARTICLE_SHARE',_utf8mb4'PROJECT_UPDATE',_utf8mb4'CODE',_utf8mb4'POLL',_utf8mb4'TEAM_NOTICE',_utf8mb4'REPOST',_utf8mb4'QUOTE',_utf8mb4'VIDEO_LINK')),
  CONSTRAINT `chk_community_moment_visibility` CHECK (`visibility` in (_utf8mb4'PUBLIC',_utf8mb4'FOLLOWERS_ONLY',_utf8mb4'PRIVATE',_utf8mb4'UNLISTED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '个人或团队博客身份发布的社区动态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_moment_moderation_event
-- ----------------------------
DROP TABLE IF EXISTS `community_moment_moderation_event`;
CREATE TABLE `community_moment_moderation_event`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `moment_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：moment_id',
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作动作',
  `actor_admin_id` bigint NOT NULL COMMENT '业务字段：actor_admin_id',
  `previous_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：previous_status',
  `new_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：new_status',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：reason',
  `metadata_json` json NULL COMMENT '元数据 JSON',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_moment_moderation_moment_created`(`moment_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_moment_moderation_action_created`(`action` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_moment_moderation_moment` FOREIGN KEY (`moment_id`) REFERENCES `community_moment` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_moment_moderation_action` CHECK (`action` in (_utf8mb4'PLATFORM_APPROVED',_utf8mb4'PLATFORM_REJECTED',_utf8mb4'PLATFORM_TAKEN_DOWN',_utf8mb4'PLATFORM_RESTORED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区动态审核事件记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_notification
-- ----------------------------
DROP TABLE IF EXISTS `community_notification`;
CREATE TABLE `community_notification`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `notification_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：notification_type',
  `category` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SYSTEM' COMMENT '业务字段：category',
  `importance` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL' COMMENT '业务字段：importance',
  `sender_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：sender_user_id',
  `title` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标类型',
  `target_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：target_id',
  `deduplication_key` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：deduplication_key',
  `payload_json` json NULL COMMENT '载荷 JSON',
  `aggregate_count` int UNSIGNED NOT NULL DEFAULT 1 COMMENT '业务字段：aggregate_count',
  `last_activity_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '业务字段：last_activity_at',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_deduplication`(`deduplication_key` ASC) USING BTREE,
  INDEX `idx_notification_type_created`(`notification_type` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_notification_sender`(`sender_user_id` ASC) USING BTREE,
  INDEX `idx_notification_category_activity`(`category` ASC, `last_activity_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_notification_aggregate_count` CHECK (`aggregate_count` >= 1),
  CONSTRAINT `chk_notification_category` CHECK (`category` in (_utf8mb4'INTERACTION',_utf8mb4'FOLLOW',_utf8mb4'COMMENT',_utf8mb4'COAUTHOR',_utf8mb4'SUBMISSION',_utf8mb4'TEAM',_utf8mb4'REVIEW',_utf8mb4'SYSTEM')),
  CONSTRAINT `chk_notification_importance` CHECK (`importance` in (_utf8mb4'NORMAL',_utf8mb4'HIGH'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内通知内容' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_notification_recipient
-- ----------------------------
DROP TABLE IF EXISTS `community_notification_recipient`;
CREATE TABLE `community_notification_recipient`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `notification_id` bigint UNSIGNED NOT NULL COMMENT '通知 ID',
  `recipient_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：recipient_user_id',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNREAD' COMMENT '业务状态',
  `read_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：read_at',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_recipient`(`notification_id` ASC, `recipient_user_id` ASC) USING BTREE,
  INDEX `idx_recipient_inbox`(`recipient_user_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_notification_recipient_notification` FOREIGN KEY (`notification_id`) REFERENCES `community_notification` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notification_recipient_user` FOREIGN KEY (`recipient_user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_notification_recipient_status` CHECK (`status` in (_utf8mb4'UNREAD',_utf8mb4'READ',_utf8mb4'ARCHIVED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内通知收件箱' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_report
-- ----------------------------
DROP TABLE IF EXISTS `community_report`;
CREATE TABLE `community_report`  (
  `id` bigint UNSIGNED NOT NULL COMMENT 'Report ID',
  `reporter_user_id` bigint UNSIGNED NOT NULL COMMENT 'Community reporter',
  `target_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ARTICLE, MOMENT, COMMENT, BLOG, USER, TEAM, CHAT',
  `target_id` bigint UNSIGNED NOT NULL COMMENT 'Reported resource identifier',
  `reason_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准原因代码',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `evidence_json` json NULL COMMENT 'Reporter supplied evidence references only',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '业务状态',
  `assignee_admin_id` bigint NULL DEFAULT NULL COMMENT '业务字段：assignee_admin_id',
  `resolution_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：resolution_code',
  `resolution_note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：resolution_note',
  `resolved_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：resolved_at',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `active_dedupe_key` varchar(180) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'PENDING',_utf8mb4'ASSIGNED')) then concat(`reporter_user_id`,_utf8mb4':',`target_type`,_utf8mb4':',`target_id`,_utf8mb4':',`reason_code`) else NULL end)) STORED COMMENT '业务字段：active_dedupe_key' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_active_dedupe`(`active_dedupe_key` ASC) USING BTREE,
  INDEX `idx_report_queue`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_report_target`(`target_type` ASC, `target_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_report_reporter`(`reporter_user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_report_assignee`(`assignee_admin_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_report_status` CHECK (`status` in (_utf8mb4'PENDING',_utf8mb4'ASSIGNED',_utf8mb4'RESOLVED',_utf8mb4'DISMISSED')),
  CONSTRAINT `chk_report_target_type` CHECK (`target_type` in (_utf8mb4'ARTICLE',_utf8mb4'MOMENT',_utf8mb4'COMMENT',_utf8mb4'BLOG',_utf8mb4'USER',_utf8mb4'TEAM',_utf8mb4'CHAT'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区举报记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_report_event
-- ----------------------------
DROP TABLE IF EXISTS `community_report_event`;
CREATE TABLE `community_report_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `report_id` bigint UNSIGNED NOT NULL COMMENT '举报 ID',
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'USER or ADMIN',
  `actor_id` bigint NULL DEFAULT NULL COMMENT '操作人 ID',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'CREATED, CLAIMED, RESOLVED, DISMISSED',
  `before_snapshot` json NULL COMMENT '变更前快照',
  `after_snapshot` json NULL COMMENT '变更后快照',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_event_report`(`report_id` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_report_event_report` FOREIGN KEY (`report_id`) REFERENCES `community_report` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区举报事件日志（仅追加）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_sanction
-- ----------------------------
DROP TABLE IF EXISTS `community_sanction`;
CREATE TABLE `community_sanction`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `target_user_id` bigint UNSIGNED NOT NULL COMMENT '目标用户 ID',
  `sanction_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'WARNING,RATE_LIMIT,COMMENT_BAN,MOMENT_BAN,SUBMISSION_BAN,PUBLISH_SUSPEND,LOGIN_SUSPEND,PERMANENT_BAN',
  `reason_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标准原因代码',
  `reason_note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原因备注',
  `source_report_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '来源举报 ID',
  `issued_by_admin_id` bigint NOT NULL COMMENT '业务字段：issued_by_admin_id',
  `starts_at` datetime(3) NOT NULL COMMENT '开始时间',
  `expires_at` datetime(3) NULL DEFAULT NULL COMMENT '到期时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态',
  `revoked_by_admin_id` bigint NULL DEFAULT NULL COMMENT '业务字段：revoked_by_admin_id',
  `revoked_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：revoked_at',
  `revoke_note` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：revoke_note',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sanction_target_active`(`target_user_id` ASC, `status` ASC, `expires_at` ASC) USING BTREE,
  INDEX `idx_sanction_report`(`source_report_id` ASC) USING BTREE,
  CONSTRAINT `fk_sanction_target` FOREIGN KEY (`target_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_sanction_status` CHECK (`status` in (_utf8mb4'ACTIVE',_utf8mb4'EXPIRED',_utf8mb4'REVOKED')),
  CONSTRAINT `chk_sanction_type` CHECK (`sanction_type` in (_utf8mb4'WARNING',_utf8mb4'RATE_LIMIT',_utf8mb4'COMMENT_BAN',_utf8mb4'MOMENT_BAN',_utf8mb4'SUBMISSION_BAN',_utf8mb4'PUBLISH_SUSPEND',_utf8mb4'LOGIN_SUSPEND',_utf8mb4'PERMANENT_BAN'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区用户处罚记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_sanction_event
-- ----------------------------
DROP TABLE IF EXISTS `community_sanction_event`;
CREATE TABLE `community_sanction_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `sanction_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：sanction_id',
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人类型',
  `actor_id` bigint NULL DEFAULT NULL COMMENT '操作人 ID',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `snapshot` json NULL COMMENT '数据快照',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sanction_event`(`sanction_id` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_sanction_event` FOREIGN KEY (`sanction_id`) REFERENCES `community_sanction` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区处罚处理事件记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_sanction_rate_limit
-- ----------------------------
DROP TABLE IF EXISTS `community_sanction_rate_limit`;
CREATE TABLE `community_sanction_rate_limit`  (
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户 ID',
  `action_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `last_action_at` datetime(3) NOT NULL COMMENT '业务字段：last_action_at',
  PRIMARY KEY (`user_id`, `action_type`) USING BTREE,
  CONSTRAINT `fk_sanction_rate_limit_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区处罚请求限流记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_user
-- ----------------------------
DROP TABLE IF EXISTS `community_user`;
CREATE TABLE `community_user`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '社区用户 ID',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '可修改的公开用户名',
  `display_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公开显示名称',
  `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公开简介',
  `avatar_file_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '头像文件 ID',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL' COMMENT '账号状态',
  `personal_blog_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '唯一的个人博客 ID',
  `verification_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'UNVERIFIED' COMMENT '认证状态',
  `publish_restricted_until` datetime(3) NULL DEFAULT NULL COMMENT '禁止发布截止时间',
  `comment_restricted_until` datetime(3) NULL DEFAULT NULL COMMENT '禁止评论截止时间',
  `last_login_at` datetime(3) NULL DEFAULT NULL COMMENT '最近登录时间',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  `submission_restricted_until` datetime(3) NULL DEFAULT NULL COMMENT '处罚导致的投稿限制截止时间',
  `sanction_original_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '首次登录类处罚前的账号状态',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_community_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_community_user_personal_blog`(`personal_blog_id` ASC) USING BTREE,
  INDEX `idx_community_user_status_created`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_community_user_public_search`(`status` ASC, `updated_at` ASC) USING BTREE,
  CONSTRAINT `chk_community_user_status` CHECK (`status` in (_utf8mb4'NORMAL',_utf8mb4'LIMITED',_utf8mb4'FROZEN',_utf8mb4'BANNED',_utf8mb4'DEACTIVATED',_utf8mb4'DELETED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区用户账号主体' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_user_login_account
-- ----------------------------
DROP TABLE IF EXISTS `community_user_login_account`;
CREATE TABLE `community_user_login_account`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户 ID',
  `login_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'EMAIL' COMMENT '业务字段：login_type',
  `normalized_identifier` varchar(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规范化后的邮箱等登录标识',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'BCrypt 或 Argon2id 哈希',
  `verified_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：verified_at',
  `failed_login_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：failed_login_count',
  `locked_until` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：locked_until',
  `last_login_at` datetime(3) NULL DEFAULT NULL COMMENT '最近登录时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `force_password_change` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必须在下次登录后修改密码',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_login_account_type_identifier`(`login_type` ASC, `normalized_identifier` ASC) USING BTREE,
  UNIQUE INDEX `uk_login_account_user_type`(`user_id` ASC, `login_type` ASC) USING BTREE,
  CONSTRAINT `fk_login_account_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_login_account_type` CHECK (`login_type` in (_utf8mb4'EMAIL',_utf8mb4'PHONE',_utf8mb4'GITHUB',_utf8mb4'WECHAT',_utf8mb4'GOOGLE'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区用户登录账号' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_user_preference
-- ----------------------------
DROP TABLE IF EXISTS `community_user_preference`;
CREATE TABLE `community_user_preference`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户 ID',
  `locale` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'zh-CN' COMMENT '业务字段：locale',
  `time_zone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '业务字段：time_zone',
  `email_notification_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '业务字段：email_notification_enabled',
  `content_language` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'zh-CN' COMMENT '业务字段：content_language',
  `likes_visibility` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PRIVATE' COMMENT '业务字段：likes_visibility',
  `settings_json` json NULL COMMENT '非关系型界面偏好',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_preference_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_user_preference_likes_visibility` CHECK (`likes_visibility` in (_utf8mb4'PRIVATE',_utf8mb4'PUBLIC',_utf8mb4'FOLLOWERS_ONLY',_utf8mb4'MUTUAL_ONLY'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区用户偏好' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for content_keyword_rule
-- ----------------------------
DROP TABLE IF EXISTS `content_keyword_rule`;
CREATE TABLE `content_keyword_rule`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `keyword` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Original policy phrase',
  `normalized_keyword` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'NFKC/lowercase matching form',
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BLOCK / REVIEW / WARN',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_by_admin_id` bigint NULL DEFAULT NULL COMMENT '创建管理员 ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  `active_normalized_keyword` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when ((`status` <> _utf8mb4'DELETED') and (`deleted_at` is null)) then `normalized_keyword` else NULL end)) STORED COMMENT '业务字段：active_normalized_keyword' NULL,
  `content_scopes` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ARTICLE,COMMENT,MOMENT' COMMENT 'Comma-separated ARTICLE,COMMENT,MOMENT or ALL',
  `risk_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW,MEDIUM,HIGH,CRITICAL',
  `hit_action` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WARN' COMMENT 'BLOCK,MANUAL_REVIEW,WARN',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword_rule_active_normalized`(`active_normalized_keyword` ASC) USING BTREE,
  INDEX `idx_keyword_rule_scan`(`status` ASC, `severity` ASC, `sort_order` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `chk_keyword_rule_hit_action` CHECK (`hit_action` in (_utf8mb4'BLOCK',_utf8mb4'MANUAL_REVIEW',_utf8mb4'WARN')),
  CONSTRAINT `chk_keyword_rule_risk_level` CHECK (`risk_level` in (_utf8mb4'LOW',_utf8mb4'MEDIUM',_utf8mb4'HIGH',_utf8mb4'CRITICAL')),
  CONSTRAINT `chk_keyword_rule_severity` CHECK (`severity` in (_utf8mb4'BLOCK',_utf8mb4'REVIEW',_utf8mb4'WARN')),
  CONSTRAINT `chk_keyword_rule_status` CHECK (`status` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED',_utf8mb4'DELETED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内容审核可配置关键词规则' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for content_review_task
-- ----------------------------
DROP TABLE IF EXISTS `content_review_task`;
CREATE TABLE `content_review_task`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `subject_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ARTICLE' COMMENT '主体类型',
  `subject_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：subject_id',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `fixed_version_id` bigint UNSIGNED NOT NULL COMMENT '提交后不可变的审核版本',
  `review_stage` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PLATFORM_AUTO' COMMENT '审核阶段',
  `review_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'AUTO' COMMENT '审核类型',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'QUEUED' COMMENT '业务状态',
  `risk_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOW' COMMENT '风险等级',
  `idempotency_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等键',
  `submitted_by_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：submitted_by_user_id',
  `assignee_admin_id` bigint NULL DEFAULT NULL COMMENT '平台管理员 ID',
  `result_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：result_code',
  `result_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：result_reason',
  `submitted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '业务字段：submitted_at',
  `claimed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：claimed_at',
  `completed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：completed_at',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `active_article_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'QUEUED',_utf8mb4'AUTO_REVIEWING',_utf8mb4'MANUAL_REVIEWING')) then `article_id` else NULL end)) STORED COMMENT '业务字段：active_article_id' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_review_task_idempotency`(`idempotency_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_review_task_active_article`(`active_article_id` ASC) USING BTREE,
  INDEX `idx_review_task_queue`(`status` ASC, `risk_level` ASC, `submitted_at` ASC) USING BTREE,
  INDEX `idx_review_task_assignee`(`assignee_admin_id` ASC, `status` ASC, `claimed_at` ASC) USING BTREE,
  INDEX `idx_review_task_article_history`(`article_id` ASC, `submitted_at` ASC) USING BTREE,
  INDEX `fk_review_task_version`(`fixed_version_id` ASC) USING BTREE,
  INDEX `fk_review_task_submitter`(`submitted_by_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_review_task_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_review_task_submitter` FOREIGN KEY (`submitted_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_review_task_version` FOREIGN KEY (`fixed_version_id`) REFERENCES `article_version` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_review_task_risk` CHECK (`risk_level` in (_utf8mb4'LOW',_utf8mb4'MEDIUM',_utf8mb4'HIGH',_utf8mb4'CRITICAL')),
  CONSTRAINT `chk_review_task_stage` CHECK (`review_stage` in (_utf8mb4'TEAM_INTERNAL',_utf8mb4'PLATFORM_AUTO',_utf8mb4'PLATFORM_MANUAL',_utf8mb4'APPEAL_REVIEW')),
  CONSTRAINT `chk_review_task_status` CHECK (`status` in (_utf8mb4'QUEUED',_utf8mb4'AUTO_REVIEWING',_utf8mb4'MANUAL_REVIEWING',_utf8mb4'APPROVED',_utf8mb4'REVISION_REQUIRED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED',_utf8mb4'EXPIRED')),
  CONSTRAINT `chk_review_task_type` CHECK (`review_type` in (_utf8mb4'AUTO',_utf8mb4'MANUAL'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内容审核任务与不可删除审核记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for creator_analytics_event
-- ----------------------------
DROP TABLE IF EXISTS `creator_analytics_event`;
CREATE TABLE `creator_analytics_event`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `author_user_id` bigint UNSIGNED NOT NULL COMMENT '作者用户 ID',
  `event_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `source_type` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DIRECT' COMMENT '来源类型',
  `search_term` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：search_term',
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator_analytics_author_day`(`author_user_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_creator_analytics_article_day`(`article_id` ASC, `occurred_at` ASC) USING BTREE,
  CONSTRAINT `chk_creator_analytics_source` CHECK (`source_type` in (_utf8mb4'DIRECT',_utf8mb4'INTERNAL',_utf8mb4'SEARCH',_utf8mb4'REFERRAL')),
  CONSTRAINT `chk_creator_analytics_type` CHECK (`event_type` in (_utf8mb4'VIEW',_utf8mb4'SEARCH_CLICK'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '创作者数据统计事件' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for editorial_collection
-- ----------------------------
DROP TABLE IF EXISTS `editorial_collection`;
CREATE TABLE `editorial_collection`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `kind` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：kind',
  `title` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `slug` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL 标识',
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摘要',
  `cover_file_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '封面文件 ID',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '业务状态',
  `starts_at` datetime(3) NULL DEFAULT NULL COMMENT '开始时间',
  `ends_at` datetime(3) NULL DEFAULT NULL COMMENT '结束时间',
  `display_order` int NOT NULL DEFAULT 0 COMMENT '展示排序',
  `created_by_admin_id` bigint NULL DEFAULT NULL COMMENT '创建管理员 ID',
  `published_at` datetime(3) NULL DEFAULT NULL COMMENT '发布时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_editorial_collection_slug`(`slug` ASC) USING BTREE,
  INDEX `idx_editorial_public`(`status` ASC, `starts_at` ASC, `ends_at` ASC, `display_order` ASC, `published_at` ASC) USING BTREE,
  CONSTRAINT `chk_editorial_kind` CHECK (`kind` in (_utf8mb4'TOPIC',_utf8mb4'EVENT',_utf8mb4'ANNOUNCEMENT',_utf8mb4'FEATURED',_utf8mb4'COLLECTION')),
  CONSTRAINT `chk_editorial_status` CHECK (`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED',_utf8mb4'ARCHIVED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台精选内容集合' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for editorial_collection_item
-- ----------------------------
DROP TABLE IF EXISTS `editorial_collection_item`;
CREATE TABLE `editorial_collection_item`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `collection_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：collection_id',
  `target_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标类型',
  `target_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：target_id',
  `display_order` int NOT NULL DEFAULT 0 COMMENT '展示排序',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_editorial_item_target`(`collection_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_editorial_item_order`(`collection_id` ASC, `display_order` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_editorial_item_collection` FOREIGN KEY (`collection_id`) REFERENCES `editorial_collection` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_editorial_item_type` CHECK (`target_type` in (_utf8mb4'ARTICLE',_utf8mb4'SERIES'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台精选集合内容关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for favorite_folder
-- ----------------------------
DROP TABLE IF EXISTS `favorite_folder`;
CREATE TABLE `favorite_folder`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `owner_user_id` bigint UNSIGNED NOT NULL COMMENT '所属用户 ID',
  `name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `visibility` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PRIVATE' COMMENT '可见范围',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认',
  `item_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '项目数量',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  `active_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`deleted_at` is null) then `name` else NULL end)) STORED COMMENT '业务字段：active_name' NULL,
  `default_owner_id` bigint UNSIGNED GENERATED ALWAYS AS ((case when ((`is_default` = 1) and (`deleted_at` is null)) then `owner_user_id` else NULL end)) STORED COMMENT '业务字段：default_owner_id' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_favorite_folder_name`(`owner_user_id` ASC, `active_name` ASC) USING BTREE,
  UNIQUE INDEX `uk_favorite_folder_default`(`default_owner_id` ASC) USING BTREE,
  INDEX `idx_favorite_folder_owner_sort`(`owner_user_id` ASC, `sort_order` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_favorite_folder_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_favorite_folder_default` CHECK (`is_default` in (0,1)),
  CONSTRAINT `chk_favorite_folder_visibility` CHECK (`visibility` in (_utf8mb4'PRIVATE',_utf8mb4'PUBLIC',_utf8mb4'FOLLOWERS_ONLY',_utf8mb4'MUTUAL_ONLY'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户自定义收藏夹' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for favorite_folder_item
-- ----------------------------
DROP TABLE IF EXISTS `favorite_folder_item`;
CREATE TABLE `favorite_folder_item`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `folder_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：folder_id',
  `favorite_item_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：favorite_item_id',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_favorite_folder_item`(`folder_id` ASC, `favorite_item_id` ASC) USING BTREE,
  INDEX `idx_favorite_folder_item_content`(`favorite_item_id` ASC) USING BTREE,
  CONSTRAINT `fk_favorite_folder_item_content` FOREIGN KEY (`favorite_item_id`) REFERENCES `favorite_item` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_favorite_folder_item_folder` FOREIGN KEY (`folder_id`) REFERENCES `favorite_folder` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '同一收藏内容加入多个收藏夹的映射' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for favorite_item
-- ----------------------------
DROP TABLE IF EXISTS `favorite_item`;
CREATE TABLE `favorite_item`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `owner_user_id` bigint UNSIGNED NOT NULL COMMENT '所属用户 ID',
  `target_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ARTICLE/MOMENT',
  `target_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：target_id',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_favorite_item_owner_target`(`owner_user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_favorite_item_owner_created`(`owner_user_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_favorite_item_target`(`target_type` ASC, `target_id` ASC, `created_at` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_favorite_item_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `community_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_favorite_item_target` CHECK (`target_type` in (_utf8mb4'ARTICLE',_utf8mb4'MOMENT'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户收藏内容的唯一关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_object
-- ----------------------------
DROP TABLE IF EXISTS `file_object`;
CREATE TABLE `file_object`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `storage_provider` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOCAL' COMMENT '业务字段：storage_provider',
  `bucket_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：bucket_name',
  `object_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：object_key',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：original_name',
  `mime_type` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：mime_type',
  `size_bytes` bigint UNSIGNED NOT NULL COMMENT '业务字段：size_bytes',
  `sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务字段：sha256',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态',
  `created_by_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建用户 ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_object_location`(`storage_provider` ASC, `bucket_name` ASC, `object_key` ASC) USING BTREE,
  INDEX `idx_file_object_hash`(`sha256` ASC, `size_bytes` ASC) USING BTREE,
  INDEX `idx_file_object_status_created`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_file_object_creator`(`created_by_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_file_object_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_file_object_status` CHECK (`status` in (_utf8mb4'UPLOADING',_utf8mb4'ACTIVE',_utf8mb4'QUARANTINED',_utf8mb4'DELETED',_utf8mb4'PURGED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区文件对象元数据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表描述',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '实体类名称',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成功能名',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '生成功能作者',
  `gen_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '生成类型（crud单表 tree树表）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '生成路径',
  `front_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'naive-ui' COMMENT '前端模板类型',
  `form_layout` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'vertical' COMMENT '表单布局（vertical-从上到下 grid-一行两列）',
  `parent_menu_id` bigint NULL DEFAULT NULL COMMENT '上级菜单ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '列类型',
  `java_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'Java类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'Java字段名',
  `is_pk` tinyint NULL DEFAULT 0 COMMENT '是否主键（1是）',
  `is_increment` tinyint NULL DEFAULT 0 COMMENT '是否自增（1是）',
  `is_required` tinyint NULL DEFAULT 0 COMMENT '是否必填（1是）',
  `is_insert` tinyint NULL DEFAULT 0 COMMENT '是否为插入字段（1是）',
  `is_edit` tinyint NULL DEFAULT 0 COMMENT '是否编辑字段（1是）',
  `is_list` tinyint NULL DEFAULT 0 COMMENT '是否列表字段（1是）',
  `is_query` tinyint NULL DEFAULT 0 COMMENT '是否查询字段（1是）',
  `query_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'EQ' COMMENT '查询方式',
  `html_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '显示类型',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_id`(`table_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成字段表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for platform_tag
-- ----------------------------
DROP TABLE IF EXISTS `platform_tag`;
CREATE TABLE `platform_tag`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `slug` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL 标识',
  `description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
  `status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '业务状态',
  `merged_to_tag_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：merged_to_tag_id',
  `usage_count` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '业务字段：usage_count',
  `created_by_admin_id` bigint NULL DEFAULT NULL COMMENT '后台管理员 ID，不与社区用户混用',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_tag_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uk_platform_tag_slug`(`slug` ASC) USING BTREE,
  INDEX `idx_platform_tag_status_usage`(`status` ASC, `usage_count` ASC) USING BTREE,
  INDEX `fk_platform_tag_merged_to`(`merged_to_tag_id` ASC) USING BTREE,
  CONSTRAINT `fk_platform_tag_merged_to` FOREIGN KEY (`merged_to_tag_id`) REFERENCES `platform_tag` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_platform_tag_status` CHECK (`status` in (_utf8mb4'ACTIVE',_utf8mb4'HIDDEN',_utf8mb4'MERGED',_utf8mb4'DELETED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台统一标签' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pxczxn_schema_version
-- ----------------------------
DROP TABLE IF EXISTS `pxczxn_schema_version`;
CREATE TABLE `pxczxn_schema_version`  (
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务字段：version',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '说明',
  `checksum` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务字段：checksum',
  `executed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '业务字段：executed_at',
  `success` tinyint(1) NOT NULL COMMENT '是否成功',
  PRIMARY KEY (`version`) USING BTREE,
  CONSTRAINT `chk_pxczxn_schema_version_success` CHECK (`success` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据库迁移版本记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` blob NULL COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Blob类型的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日历名称',
  `calendar` blob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`, `calendar_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日历信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Cron类型的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`, `entry_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '已触发的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务详细信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`, `lock_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '存储的悲观锁信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`, `trigger_group`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '暂停的触发器表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`, `instance_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度器状态表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简单触发器的信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `str_prop_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `str_prop_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `str_prop_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `int_prop_1` int NULL DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `int_prop_2` int NULL DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `long_prop_1` bigint NULL DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `long_prop_2` bigint NULL DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `dec_prop_1` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `dec_prop_2` decimal(13, 4) NULL DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `bool_prop_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `bool_prop_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '同步机制的行锁表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint NULL DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint NULL DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int NULL DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint NULL DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint NULL DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` blob NULL COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`) USING BTREE,
  INDEX `sched_name`(`sched_name` ASC, `job_name` ASC, `job_group` ASC) USING BTREE,
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `qrtz_job_details` (`sched_name`, `job_name`, `job_group`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '触发器详细信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别 1男 2女',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_api_access_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_api_access_log`;
CREATE TABLE `sys_api_access_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `start_time` datetime NULL DEFAULT NULL COMMENT '请求开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '请求结束时间',
  `api_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API路径',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'HTTP方法',
  `status_code` int NULL DEFAULT NULL COMMENT 'HTTP状态码',
  `success` tinyint NULL DEFAULT 1 COMMENT '是否成功(0否 1是)',
  `cost_time` bigint NULL DEFAULT NULL COMMENT '耗时(毫秒)',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID(未登录为空)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_api_path`(`api_path`(100) ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23891 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'API访问统计日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_chat_group
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_group`;
CREATE TABLE `sys_chat_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '群ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群名称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群头像',
  `owner_id` bigint NOT NULL COMMENT '群主ID',
  `announcement` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群公告',
  `max_members` int NULL DEFAULT 200 COMMENT '最大成员数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-解散 1-正常',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_owner_id`(`owner_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群聊表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_chat_group_member
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_group_member`;
CREATE TABLE `sys_chat_group_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群内昵称',
  `role` tinyint NULL DEFAULT 0 COMMENT '角色：0-普通成员 1-管理员 2-群主',
  `muted` tinyint NULL DEFAULT 0 COMMENT '是否禁言：0-否 1-是',
  `last_read_message_id` bigint NOT NULL DEFAULT 0 COMMENT '该成员最后已读群消息ID',
  `last_read_time` datetime NULL DEFAULT NULL COMMENT '最近一次标记群消息已读时间',
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_user`(`group_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `chk_chat_group_member_muted` CHECK (`muted` in (0,1)),
  CONSTRAINT `chk_chat_group_member_role` CHECK (`role` in (0,1,2))
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群成员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_chat_group_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_group_message`;
CREATE TABLE `sys_chat_group_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者名称',
  `sender_avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者头像',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `msg_type` tinyint NULL DEFAULT 1 COMMENT '消息类型：1-文本 2-图片 3-文件 4-系统消息',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_chat_group_message_page`(`group_id` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `chk_chat_group_message_type` CHECK (`msg_type` in (1,2,3,4))
) ENGINE = InnoDB AUTO_INCREMENT = 159 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '群消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_chat_message`;
CREATE TABLE `sys_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者名称',
  `sender_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者头像',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID(0表示群发)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `msg_type` tinyint NULL DEFAULT 1 COMMENT '消息类型(1文本 2图片 3文件)',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读(0未读 1已读)',
  `sender_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '发送方是否已清除该消息：0否 1是',
  `receiver_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '接收方是否已清除该消息：0否 1是',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_receiver_id`(`receiver_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_chat_pair_time`(`sender_id` ASC, `receiver_id` ASC, `send_time` ASC, `id` ASC) USING BTREE,
  INDEX `idx_chat_receiver_unread`(`receiver_id` ASC, `is_read` ASC, `send_time` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `chk_chat_message_read` CHECK (`is_read` in (0,1)),
  CONSTRAINT `chk_chat_message_type` CHECK (`msg_type` in (1,2,3)),
  CONSTRAINT `chk_chat_message_visibility` CHECK ((`sender_deleted` in (0,1)) and (`receiver_deleted` in (0,1)))
) ENGINE = InnoDB AUTO_INCREMENT = 513 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_config_group
-- ----------------------------
DROP TABLE IF EXISTS `sys_config_group`;
CREATE TABLE `sys_config_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组编码',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `group_icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分组图标',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值(JSON格式)',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_code`(`group_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置分组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门ID',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '样式属性',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '是否默认(0-否 1-是)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '原始文件名',
  `file_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件访问URL',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件类型（MIME类型）',
  `file_suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件后缀',
  `storage_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '存储类型',
  `bucket_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '存储桶名称',
  `group_id` bigint NULL DEFAULT NULL COMMENT '分组ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_path`(`file_path`(191) ASC) USING BTREE,
  INDEX `idx_storage_type`(`storage_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_file_type`(`file_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 85 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_file_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_config`;
CREATE TABLE `sys_file_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `storage_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储类型(local/minio/aliyun)',
  `master` tinyint NULL DEFAULT 0 COMMENT '是否为主配置(0否 1是)',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问域名',
  `base_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '基础路径(本地存储)',
  `bucket_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储桶名称',
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问密钥',
  `secret_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '秘密密钥',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '端点地址',
  `region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地域',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0禁用 1启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_storage_type`(`storage_type` ASC) USING BTREE,
  INDEX `idx_master`(`master` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件存储配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_file_group
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_group`;
CREATE TABLE `sys_file_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件分组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'cron执行表达式',
  `misfire_policy` tinyint NULL DEFAULT 3 COMMENT '计划执行错误策略(1-立即执行 2-执行一次 3-放弃执行)',
  `concurrent` tinyint NULL DEFAULT 1 COMMENT '是否并发执行(0-允许 1-禁止)',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态(0-暂停 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日志信息',
  `status` tinyint NULL DEFAULT 0 COMMENT '执行状态(0-正常 1-失败)',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '异常信息',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `stop_time` datetime NULL DEFAULT NULL COMMENT '停止时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7107 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '定时任务日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NULL DEFAULT 0 COMMENT '登录状态(0-成功 1-失败)',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 485 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父级ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `type` tinyint NOT NULL COMMENT '菜单类型(1-目录 2-菜单 3-按钮)',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint NULL DEFAULT 1 COMMENT '是否可见(0-隐藏 1-显示)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `is_frame` tinyint NULL DEFAULT 0 COMMENT '是否外链(0-否 1-是)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9206 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '通知内容',
  `notice_type` tinyint NULL DEFAULT 1 COMMENT '通知类型(1通知 2公告)',
  `channels` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '[\"station\"]' COMMENT '推送渠道(JSON): station站内信,email邮件,sms短信,webhook飞书/钉钉/企业微信',
  `target_type` tinyint NULL DEFAULT 3 COMMENT '推送对象类型(1指定用户 2按部门 3全部)',
  `target_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推送对象ID(JSON): 用户ID或部门ID数组',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态(0草稿 1发布)',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者ID',
  `create_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建者名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_type`(`notice_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_notice_send_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_send_log`;
CREATE TABLE `sys_notice_send_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `channel` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推送渠道: station站内信,email邮件,dingtalk钉钉,feishu飞书,wechat_work企业微信',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1成功 2失败',
  `target_count` int NULL DEFAULT 0 COMMENT '推送目标数量',
  `success_count` int NULL DEFAULT 0 COMMENT '成功数量(邮件/站内信)',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notice_id`(`notice_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知推送记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块标题',
  `business_type` int NULL DEFAULT 0 COMMENT '业务类型(0-其它 1-新增 2-修改 3-删除)',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方式',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人员',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '主机地址',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '返回参数',
  `status` int NULL DEFAULT 0 COMMENT '操作状态(0-正常 1-异常)',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint NULL DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 942 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父岗位ID',
  `post_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-停用 1-正常)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_code`(`post_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  `data_scope` tinyint NOT NULL DEFAULT 1 COMMENT '数据范围(1全部 2自定义 3本部门 4本部门及以下 5仅本人)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_id` bigint NOT NULL COMMENT '角色 ID',
  `dept_id` bigint NOT NULL COMMENT '部门 ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_dept`(`role_id` ASC, `dept_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-部门 数据权限关联' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_server
-- ----------------------------
DROP TABLE IF EXISTS `sys_server`;
CREATE TABLE `sys_server`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务器名称',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务器地址',
  `port` int NOT NULL DEFAULT 22 COMMENT 'SSH端口',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `auth_type` tinyint NOT NULL DEFAULT 1 COMMENT '认证方式：1-密码 2-密钥',
  `password` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（加密存储）',
  `private_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '私钥内容',
  `passphrase` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '私钥密码（加密存储）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `last_connect_time` datetime NULL DEFAULT NULL COMMENT '最后连接时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '服务器管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_sms_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_log`;
CREATE TABLE `sys_sms_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '短信内容/验证码',
  `sms_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'verify_code' COMMENT '短信类型：verify_code-验证码 notice-通知 marketing-营销',
  `template_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板ID',
  `template_params` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板参数（JSON格式）',
  `provider` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务商：aliyun-阿里云 tencent-腾讯云 console-控制台',
  `status` tinyint NULL DEFAULT 0 COMMENT '发送状态：0-发送中 1-成功 2-失败',
  `result_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送结果消息',
  `biz_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务商返回的消息ID',
  `send_time` datetime NULL DEFAULT NULL COMMENT '发送时间',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `biz_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务类型：login-登录 register-注册 reset_password-重置密码 bind_phone-绑定手机',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短信发送记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门id',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `must_change_password` tinyint NOT NULL DEFAULT 0 COMMENT 'Whether a password change is required (0-no 1-yes)',
  `password_changed_at` datetime NULL DEFAULT NULL COMMENT 'Time of the latest user-selected password change',
  `temporary_password_issued_at` datetime NULL DEFAULT NULL COMMENT 'Time the current one-time password was issued',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别(0-未知 1-男 2-女)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
  `is_quit` tinyint NULL DEFAULT 0 COMMENT '是否离职(0-否 1-是)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `user_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'admin' COMMENT '用户类型(admin-后台管理员 pc-PC前台用户 app-App/小程序用户)',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信openId(微信扫码登录时使用)',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_open_id`(`open_id` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 91 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_blacklist`;
CREATE TABLE `sys_user_blacklist`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `blocked_user_id` bigint NOT NULL COMMENT '被拉黑的用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_blocked`(`user_id` ASC, `blocked_user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_blocked_user_id`(`blocked_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户黑名单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读(0未读 1已读)',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_notice`(`user_id` ASC, `notice_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_notice_id`(`notice_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户通知关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户岗位关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 114 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for team
-- ----------------------------
DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Team ID (same as blog_id)',
  `blog_id` bigint UNSIGNED NOT NULL COMMENT 'Unique foreign key to blog table',
  `owner_user_id` bigint UNSIGNED NOT NULL COMMENT 'Current team owner',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '团队分类',
  `content_direction` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容方向',
  `theme` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主页主题',
  `seo_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'SEO 标题',
  `seo_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'SEO 描述',
  `public_members` tinyint NOT NULL DEFAULT 1 COMMENT '是否公开成员列表 1=公开 0=隐藏',
  `allow_submissions` tinyint NOT NULL DEFAULT 1 COMMENT '是否开放外部投稿 1=开放 0=仅成员',
  `submission_guideline` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '投稿说明',
  `contact_info` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系方式',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISBANDED',
  `lock_version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT 'Soft delete timestamp',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_blog_id`(`blog_id` ASC) USING BTREE,
  INDEX `idx_team_owner`(`owner_user_id` ASC) USING BTREE,
  INDEX `idx_team_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_team_blog` FOREIGN KEY (`blog_id`) REFERENCES `blog` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队博客元数据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_application
-- ----------------------------
DROP TABLE IF EXISTS `team_application`;
CREATE TABLE `team_application`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Application ID',
  `applicant_user_id` bigint UNSIGNED NOT NULL COMMENT 'Applicant user ID',
  `team_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Requested team name',
  `team_slug` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Requested team slug',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Application description',
  `application_data` json NULL COMMENT 'Additional application metadata',
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Client idempotency key',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  `reviewer_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Reviewer user ID',
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Review feedback',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT 'Review timestamp',
  `lock_version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  `is_pending` tinyint GENERATED ALWAYS AS (if((`status` = _utf8mb4'PENDING'),1,NULL)) STORED COMMENT 'Generated: 1 if pending, NULL otherwise' NULL,
  `is_slug_active` tinyint GENERATED ALWAYS AS (if((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED')),1,NULL)) STORED COMMENT 'Generated: 1 for slug-reserving application statuses, NULL otherwise' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pending_application`(`applicant_user_id` ASC, `is_pending` ASC) USING BTREE,
  UNIQUE INDEX `uk_idempotency`(`idempotency_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_team_slug_active`(`team_slug` ASC, `is_slug_active` ASC) USING BTREE COMMENT 'Prevents duplicate slug across active applications',
  INDEX `idx_application_status`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_application_slug`(`team_slug` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 68 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队博客创建申请记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_audit_event
-- ----------------------------
DROP TABLE IF EXISTS `team_audit_event`;
CREATE TABLE `team_audit_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Event ID',
  `team_id` bigint UNSIGNED NOT NULL COMMENT 'Team ID',
  `actor_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Actor user ID (null for system)',
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Event type code',
  `target_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Target entity type',
  `target_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Target entity ID',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Request trace ID',
  `before_snapshot` json NULL COMMENT 'State before change',
  `after_snapshot` json NULL COMMENT 'State after change',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Event timestamp',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_team`(`team_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_audit_actor`(`actor_user_id` ASC) USING BTREE,
  INDEX `idx_audit_type`(`event_type` ASC) USING BTREE,
  CONSTRAINT `fk_audit_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 206 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队审核事件日志（仅追加）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_invitation
-- ----------------------------
DROP TABLE IF EXISTS `team_invitation`;
CREATE TABLE `team_invitation`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Invitation ID',
  `team_id` bigint UNSIGNED NOT NULL COMMENT 'Team ID',
  `invitee_user_id` bigint UNSIGNED NOT NULL COMMENT 'Invited user ID',
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Target role',
  `token_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA-256 hash of random token',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/EXPIRED',
  `invited_by_user_id` bigint UNSIGNED NOT NULL COMMENT 'Inviter user ID',
  `expires_at` datetime NOT NULL COMMENT 'Expiration time',
  `accepted_at` datetime NULL DEFAULT NULL COMMENT 'Acceptance timestamp',
  `rejected_at` datetime NULL DEFAULT NULL COMMENT 'Rejection timestamp',
  `lock_version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `is_pending` tinyint GENERATED ALWAYS AS (if((`status` = _utf8mb4'PENDING'),1,NULL)) STORED COMMENT 'Generated: 1 if pending, NULL otherwise' NULL,
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Client idempotency key',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_invitation_token`(`token_hash` ASC) USING BTREE,
  UNIQUE INDEX `uk_team_invitation_idempotency`(`idempotency_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_pending_invitation`(`team_id` ASC, `invitee_user_id` ASC, `is_pending` ASC) USING BTREE,
  INDEX `idx_invitation_user`(`invitee_user_id` ASC) USING BTREE,
  INDEX `idx_invitation_status`(`status` ASC, `expires_at` ASC) USING BTREE,
  CONSTRAINT `fk_invitation_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队成员邀请记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_member
-- ----------------------------
DROP TABLE IF EXISTS `team_member`;
CREATE TABLE `team_member`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Member record ID',
  `team_id` bigint UNSIGNED NOT NULL COMMENT 'Team ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT 'User ID',
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'OWNER/ADMIN/EDITOR/AUTHOR',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Join timestamp',
  `left_at` datetime NULL DEFAULT NULL COMMENT 'Leave timestamp (null = active)',
  `invited_by_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Inviter user ID',
  `lock_version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `is_active` tinyint GENERATED ALWAYS AS (if((`left_at` is null),1,NULL)) STORED COMMENT 'Generated: 1 if active, NULL if left' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_member_active`(`team_id` ASC, `user_id` ASC, `is_active` ASC) USING BTREE,
  INDEX `idx_team_member_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_team_member_role`(`role_code` ASC) USING BTREE,
  INDEX `idx_team_member_status`(`team_id` ASC, `left_at` ASC) USING BTREE,
  CONSTRAINT `fk_team_member_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 97 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队成员关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_permission
-- ----------------------------
DROP TABLE IF EXISTS `team_permission`;
CREATE TABLE `team_permission`  (
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Role code',
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Permission code',
  PRIMARY KEY (`role_code`, `permission_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队角色权限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_role
-- ----------------------------
DROP TABLE IF EXISTS `team_role`;
CREATE TABLE `team_role`  (
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Role code: OWNER/ADMIN/EDITOR/AUTHOR',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Display name',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Role description',
  `sort_order` int NOT NULL COMMENT 'Display order (lower = higher authority)',
  `is_system` tinyint NOT NULL DEFAULT 1 COMMENT 'System role flag (cannot delete)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`role_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '团队业务角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_series
-- ----------------------------
DROP TABLE IF EXISTS `team_series`;
CREATE TABLE `team_series`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `team_id` bigint UNSIGNED NOT NULL COMMENT '团队 ID',
  `created_by_user_id` bigint UNSIGNED NOT NULL COMMENT '创建用户 ID',
  `title` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `slug` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL 标识',
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摘要',
  `cover_file_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '封面文件 ID',
  `serialization_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ONGOING' COMMENT '业务字段：serialization_status',
  `review_status` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '业务字段：review_status',
  `reviewer_admin_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '审核管理员 ID',
  `review_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：review_comment',
  `reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '审核时间',
  `published_at` datetime(3) NULL DEFAULT NULL COMMENT '发布时间',
  `lock_version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted_at` datetime(3) NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_series_team_slug`(`team_id` ASC, `slug` ASC) USING BTREE,
  INDEX `idx_team_series_public`(`review_status` ASC, `serialization_status` ASC, `published_at` ASC) USING BTREE,
  INDEX `fk_team_series_creator`(`created_by_user_id` ASC) USING BTREE,
  INDEX `idx_team_series_public_search`(`review_status` ASC, `deleted_at` ASC, `published_at` ASC) USING BTREE,
  CONSTRAINT `fk_team_series_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_series_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_team_series_review` CHECK (`review_status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING_REVIEW',_utf8mb4'APPROVED',_utf8mb4'REJECTED')),
  CONSTRAINT `chk_team_series_serialization` CHECK (`serialization_status` in (_utf8mb4'ONGOING',_utf8mb4'COMPLETED',_utf8mb4'PAUSED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '团队文章系列及连载状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_series_article
-- ----------------------------
DROP TABLE IF EXISTS `team_series_article`;
CREATE TABLE `team_series_article`  (
  `id` bigint UNSIGNED NOT NULL COMMENT '主键 ID',
  `series_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：series_id',
  `article_id` bigint UNSIGNED NOT NULL COMMENT '文章 ID',
  `chapter_order` int NOT NULL COMMENT '业务字段：chapter_order',
  `added_by_user_id` bigint UNSIGNED NOT NULL COMMENT '业务字段：added_by_user_id',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_series_article_article`(`article_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_team_series_article_order`(`series_id` ASC, `chapter_order` ASC) USING BTREE,
  INDEX `idx_team_series_article_series`(`series_id` ASC, `chapter_order` ASC) USING BTREE,
  INDEX `fk_team_series_article_actor`(`added_by_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_team_series_article_actor` FOREIGN KEY (`added_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_series_article_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_series_article_series` FOREIGN KEY (`series_id`) REFERENCES `team_series` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_team_series_article_order` CHECK (`chapter_order` > 0)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '团队系列文章排序关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for team_submission
-- ----------------------------
DROP TABLE IF EXISTS `team_submission`;
CREATE TABLE `team_submission`  (
  `id` bigint UNSIGNED NOT NULL COMMENT 'Submission ID',
  `source_article_id` bigint UNSIGNED NOT NULL COMMENT 'Personal source article; never reassigned',
  `fixed_source_version_id` bigint UNSIGNED NOT NULL COMMENT 'Immutable source snapshot',
  `target_team_id` bigint UNSIGNED NOT NULL COMMENT 'Destination team',
  `submitted_by_user_id` bigint UNSIGNED NOT NULL COMMENT 'Source author',
  `supersedes_submission_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Previous revision/rejection submission',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TEAM_PENDING' COMMENT 'Two-stage review state',
  `team_reviewer_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '业务字段：team_reviewer_user_id',
  `team_review_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：team_review_comment',
  `team_reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：team_reviewed_at',
  `platform_reviewer_admin_id` bigint NULL DEFAULT NULL COMMENT '业务字段：platform_reviewer_admin_id',
  `platform_review_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务字段：platform_review_comment',
  `platform_reviewed_at` datetime(3) NULL DEFAULT NULL COMMENT '业务字段：platform_reviewed_at',
  `published_team_article_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Independent published team article',
  `idempotency_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等键',
  `lock_version` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `active_source_target` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'TEAM_PENDING',_utf8mb4'PLATFORM_PENDING',_utf8mb4'PLATFORM_PUBLISHING')) then concat(`source_article_id`,_utf8mb4':',`target_team_id`) else NULL end)) STORED COMMENT '业务字段：active_source_target' NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_submission_idempotency`(`idempotency_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_team_submission_active_source_target`(`active_source_target` ASC) USING BTREE,
  UNIQUE INDEX `uk_team_submission_published_article`(`published_team_article_id` ASC) USING BTREE,
  INDEX `idx_team_submission_team_queue`(`target_team_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_team_submission_author`(`submitted_by_user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_team_submission_source`(`source_article_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `fk_team_submission_fixed_version`(`fixed_source_version_id` ASC) USING BTREE,
  INDEX `fk_team_submission_supersedes`(`supersedes_submission_id` ASC) USING BTREE,
  CONSTRAINT `fk_team_submission_author` FOREIGN KEY (`submitted_by_user_id`) REFERENCES `community_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_submission_fixed_version` FOREIGN KEY (`fixed_source_version_id`) REFERENCES `article_version` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_submission_published_article` FOREIGN KEY (`published_team_article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_submission_source_article` FOREIGN KEY (`source_article_id`) REFERENCES `article` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_submission_supersedes` FOREIGN KEY (`supersedes_submission_id`) REFERENCES `team_submission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_team_submission_team` FOREIGN KEY (`target_team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_team_submission_status` CHECK (`status` in (_utf8mb4'TEAM_PENDING',_utf8mb4'TEAM_REVISION_REQUIRED',_utf8mb4'TEAM_REJECTED',_utf8mb4'PLATFORM_PENDING',_utf8mb4'PLATFORM_PUBLISHING',_utf8mb4'PLATFORM_REVISION_REQUIRED',_utf8mb4'PLATFORM_REJECTED',_utf8mb4'PUBLISHED'))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '个人文章投递团队发布记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Triggers structure for table community_abuse_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_abuse_event_prevent_update`;
delimiter ;;
CREATE TRIGGER `community_abuse_event_prevent_update` BEFORE UPDATE ON `community_abuse_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_abuse_event is append-only'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_abuse_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_abuse_event_prevent_delete`;
delimiter ;;
CREATE TRIGGER `community_abuse_event_prevent_delete` BEFORE DELETE ON `community_abuse_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_abuse_event is append-only'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_account_enforcement_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_account_enforcement_event_prevent_update`;
delimiter ;;
CREATE TRIGGER `community_account_enforcement_event_prevent_update` BEFORE UPDATE ON `community_account_enforcement_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_event is append-only: UPDATE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_account_enforcement_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_account_enforcement_event_prevent_delete`;
delimiter ;;
CREATE TRIGGER `community_account_enforcement_event_prevent_delete` BEFORE DELETE ON `community_account_enforcement_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_event is append-only: DELETE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_account_enforcement_review
-- ----------------------------
DROP TRIGGER IF EXISTS `community_account_enforcement_review_prevent_update`;
delimiter ;;
CREATE TRIGGER `community_account_enforcement_review_prevent_update` BEFORE UPDATE ON `community_account_enforcement_review` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_review is append-only: UPDATE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_account_enforcement_review
-- ----------------------------
DROP TRIGGER IF EXISTS `community_account_enforcement_review_prevent_delete`;
delimiter ;;
CREATE TRIGGER `community_account_enforcement_review_prevent_delete` BEFORE DELETE ON `community_account_enforcement_review` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_account_enforcement_review is append-only: DELETE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_appeal_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_appeal_event_prevent_update`;
delimiter ;;
CREATE TRIGGER `community_appeal_event_prevent_update` BEFORE UPDATE ON `community_appeal_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_appeal_event is append-only: UPDATE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_appeal_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_appeal_event_prevent_delete`;
delimiter ;;
CREATE TRIGGER `community_appeal_event_prevent_delete` BEFORE DELETE ON `community_appeal_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_appeal_event is append-only: DELETE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_report_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_report_event_prevent_update`;
delimiter ;;
CREATE TRIGGER `community_report_event_prevent_update` BEFORE UPDATE ON `community_report_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_report_event is append-only: UPDATE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_report_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_report_event_prevent_delete`;
delimiter ;;
CREATE TRIGGER `community_report_event_prevent_delete` BEFORE DELETE ON `community_report_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'community_report_event is append-only: DELETE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_sanction_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_sanction_event_prevent_update`;
delimiter ;;
CREATE TRIGGER `community_sanction_event_prevent_update` BEFORE UPDATE ON `community_sanction_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_sanction_event is append-only: UPDATE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table community_sanction_event
-- ----------------------------
DROP TRIGGER IF EXISTS `community_sanction_event_prevent_delete`;
delimiter ;;
CREATE TRIGGER `community_sanction_event_prevent_delete` BEFORE DELETE ON `community_sanction_event` FOR EACH ROW BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='community_sanction_event is append-only: DELETE not allowed'; END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table team_audit_event
-- ----------------------------
DROP TRIGGER IF EXISTS `team_audit_event_prevent_update`;
delimiter ;;
CREATE TRIGGER `team_audit_event_prevent_update` BEFORE UPDATE ON `team_audit_event` FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'team_audit_event is append-only: UPDATE not allowed';
END
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table team_audit_event
-- ----------------------------
DROP TRIGGER IF EXISTS `team_audit_event_prevent_delete`;
delimiter ;;
CREATE TRIGGER `team_audit_event_prevent_delete` BEFORE DELETE ON `team_audit_event` FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'team_audit_event is append-only: DELETE not allowed';
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
