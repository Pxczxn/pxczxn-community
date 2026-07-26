/*
 * Version: V005
 * Purpose: Durable scheduled article publication tasks and Quartz registration.
 * Depends on: V001 article table and Mars Admin sys_job.
 * Repeatable: Yes. The task table uses IF NOT EXISTS and the Quartz row is guarded.
 */

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `article_publish_task` (
    `id` BIGINT UNSIGNED NOT NULL,
    `article_id` BIGINT UNSIGNED NOT NULL,
    `article_version_id` BIGINT UNSIGNED NOT NULL,
    `scheduled_publish_at` DATETIME(3) NOT NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'WAITING',
    `attempt_count` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `max_attempts` TINYINT UNSIGNED NOT NULL DEFAULT 3,
    `next_attempt_at` DATETIME(3) NOT NULL,
    `last_error_code` VARCHAR(64) NULL,
    `last_error_message` VARCHAR(500) NULL,
    `lock_version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `completed_at` DATETIME(3) NULL,
    `active_article_id` BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE
            WHEN `status` IN ('WAITING', 'RUNNING', 'RETRY_WAIT')
            THEN `article_id`
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_publish_task_active` (`active_article_id`),
    KEY `idx_article_publish_task_due`
        (`status`, `next_attempt_at`, `id`),
    KEY `idx_article_publish_task_article`
        (`article_id`, `created_at`, `id`),
    CONSTRAINT `fk_article_publish_task_article`
        FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
        ON DELETE RESTRICT,
    CONSTRAINT `fk_article_publish_task_version`
        FOREIGN KEY (`article_version_id`) REFERENCES `article_version` (`id`)
        ON DELETE RESTRICT,
    CONSTRAINT `chk_article_publish_task_status`
        CHECK (`status` IN (
            'WAITING', 'RUNNING', 'RETRY_WAIT',
            'SUCCEEDED', 'FAILED', 'CANCELLED'
        )),
    CONSTRAINT `chk_article_publish_task_attempts`
        CHECK (
            `attempt_count` <= `max_attempts`
            AND `max_attempts` BETWEEN 1 AND 10
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文章定时发布持久任务';

INSERT INTO `sys_job` (
    `job_name`,
    `job_group`,
    `invoke_target`,
    `cron_expression`,
    `misfire_policy`,
    `concurrent`,
    `status`,
    `remark`,
    `create_by`,
    `update_by`,
    `deleted`
)
SELECT
    '文章定时发布',
    'COMMUNITY',
    'articleScheduledPublishTask.runDueBatch',
    '0 * * * * ?',
    2,
    1,
    1,
    '每分钟扫描到期文章；业务层负责乐观锁、幂等和有限重试',
    1,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_job`
    WHERE `invoke_target` = 'articleScheduledPublishTask.runDueBatch'
      AND `deleted` = 0
);
