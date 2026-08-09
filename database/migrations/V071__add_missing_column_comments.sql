/* Add Chinese comments to every currently undocumented column without changing its definition. */
SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_missing_column_comments $$
CREATE PROCEDURE add_missing_column_comments()
BEGIN
    DECLARE done BOOLEAN DEFAULT FALSE;
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_column_name VARCHAR(64);
    DECLARE v_column_type TEXT;
    DECLARE v_nullable VARCHAR(3);
    DECLARE v_default TEXT;
    DECLARE v_extra TEXT;
    DECLARE v_generation TEXT;
    DECLARE v_charset VARCHAR(64);
    DECLARE v_collation VARCHAR(64);
    DECLARE v_comment TEXT;
    DECLARE v_definition LONGTEXT;
    DECLARE v_sql LONGTEXT;
    DECLARE column_cursor CURSOR FOR
        SELECT table_name, column_name, column_type, is_nullable, column_default, extra,
               generation_expression, character_set_name, collation_name
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND column_comment = ''
        ORDER BY table_name, ordinal_position;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN column_cursor;
    read_columns: LOOP
        FETCH column_cursor INTO v_table_name, v_column_name, v_column_type, v_nullable, v_default,
            v_extra, v_generation, v_charset, v_collation;
        IF done THEN LEAVE read_columns; END IF;

        SET v_comment = CASE v_column_name
            WHEN 'id' THEN '主键 ID'
            WHEN 'status' THEN '业务状态'
            WHEN 'created_at' THEN '创建时间'
            WHEN 'updated_at' THEN '更新时间'
            WHEN 'created_by_user_id' THEN '创建用户 ID'
            WHEN 'created_by_admin_id' THEN '创建管理员 ID'
            WHEN 'requested_by_admin_id' THEN '申请提交管理员 ID'
            WHEN 'reviewer_admin_id' THEN '审核管理员 ID'
            WHEN 'reviewed_by_admin_id' THEN '审核管理员 ID'
            WHEN 'final_reviewed_by_admin_id' THEN '终审管理员 ID'
            WHEN 'target_user_id' THEN '目标用户 ID'
            WHEN 'user_id' THEN '用户 ID'
            WHEN 'owner_user_id' THEN '所属用户 ID'
            WHEN 'author_user_id' THEN '作者用户 ID'
            WHEN 'actor_id' THEN '操作人 ID'
            WHEN 'actor_type' THEN '操作人类型'
            WHEN 'event_type' THEN '事件类型'
            WHEN 'reason_code' THEN '标准原因代码'
            WHEN 'reason_note' THEN '原因备注'
            WHEN 'review_note' THEN '审核意见'
            WHEN 'internal_reason' THEN '内部处理说明'
            WHEN 'user_visible_reason' THEN '面向用户的处理说明'
            WHEN 'evidence_snapshot' THEN '证据快照'
            WHEN 'lock_version' THEN '乐观锁版本号'
            WHEN 'deleted_at' THEN '删除时间'
            WHEN 'expires_at' THEN '到期时间'
            WHEN 'starts_at' THEN '开始时间'
            WHEN 'ends_at' THEN '结束时间'
            WHEN 'occurred_at' THEN '发生时间'
            WHEN 'requested_at' THEN '申请提交时间'
            WHEN 'reviewed_at' THEN '审核时间'
            WHEN 'published_at' THEN '发布时间'
            WHEN 'last_login_at' THEN '最近登录时间'
            WHEN 'description' THEN '说明'
            WHEN 'title' THEN '标题'
            WHEN 'name' THEN '名称'
            WHEN 'slug' THEN 'URL 标识'
            WHEN 'summary' THEN '摘要'
            WHEN 'content_text' THEN '文本内容'
            WHEN 'content' THEN '内容'
            WHEN 'visibility' THEN '可见范围'
            WHEN 'sort_order' THEN '排序序号'
            WHEN 'display_order' THEN '展示排序'
            WHEN 'view_count' THEN '浏览次数'
            WHEN 'like_count' THEN '点赞次数'
            WHEN 'comment_count' THEN '评论次数'
            WHEN 'article_count' THEN '文章数量'
            WHEN 'item_count' THEN '项目数量'
            WHEN 'attempt_count' THEN '尝试次数'
            WHEN 'is_default' THEN '是否默认'
            WHEN 'can_edit' THEN '是否可编辑'
            WHEN 'success' THEN '是否成功'
            WHEN 'deleted' THEN '逻辑删除标识'
            WHEN 'create_time' THEN '创建时间'
            WHEN 'update_time' THEN '更新时间'
            WHEN 'role_id' THEN '角色 ID'
            WHEN 'dept_id' THEN '部门 ID'
            WHEN 'article_id' THEN '文章 ID'
            WHEN 'blog_id' THEN '博客 ID'
            WHEN 'team_id' THEN '团队 ID'
            WHEN 'case_id' THEN '处置申请 ID'
            WHEN 'report_id' THEN '举报 ID'
            WHEN 'source_report_id' THEN '来源举报 ID'
            WHEN 'file_id' THEN '文件 ID'
            WHEN 'cover_file_id' THEN '封面文件 ID'
            WHEN 'category_id' THEN '分类 ID'
            WHEN 'tag_id' THEN '标签 ID'
            WHEN 'notification_id' THEN '通知 ID'
            WHEN 'message' THEN '消息内容'
            WHEN 'snapshot' THEN '数据快照'
            WHEN 'before_snapshot' THEN '变更前快照'
            WHEN 'after_snapshot' THEN '变更后快照'
            WHEN 'metadata_json' THEN '元数据 JSON'
            WHEN 'payload_json' THEN '载荷 JSON'
            WHEN 'cleanup_scope' THEN '数据清理范围'
            WHEN 'statement' THEN '申诉陈述'
            WHEN 'decision' THEN '处理决定'
            WHEN 'action_type' THEN '操作类型'
            WHEN 'action' THEN '操作动作'
            WHEN 'source_type' THEN '来源类型'
            WHEN 'target_type' THEN '目标类型'
            WHEN 'subject_type' THEN '主体类型'
            WHEN 'review_stage' THEN '审核阶段'
            WHEN 'review_type' THEN '审核类型'
            WHEN 'risk_level' THEN '风险等级'
            WHEN 'idempotency_key' THEN '幂等键'
            WHEN 'created_at' THEN '创建时间'
            WHEN 'updated_at' THEN '更新时间'
            ELSE CONCAT('业务字段：', v_column_name)
        END;

        SET v_definition = CONCAT(
            v_column_type,
            IF(v_charset IS NULL, '', CONCAT(' CHARACTER SET ', v_charset)),
            IF(v_collation IS NULL, '', CONCAT(' COLLATE ', v_collation)),
            IF(v_generation <> '',
                CONCAT(' GENERATED ALWAYS AS (', REPLACE(v_generation, CHAR(92), ''), ') ', IF(v_extra LIKE '%STORED GENERATED%', 'STORED', 'VIRTUAL')),
                CONCAT(
                    IF(v_nullable = 'YES', ' NULL', ' NOT NULL'),
                    IF(v_default IS NULL,
                        IF(v_nullable = 'YES', ' DEFAULT NULL', ''),
                        IF(v_default LIKE 'CURRENT_TIMESTAMP%' OR v_default LIKE 'CURRENT_DATE%' OR v_default LIKE 'CURRENT_TIME%' OR v_default LIKE '(%',
                            CONCAT(' DEFAULT ', v_default),
                            CONCAT(' DEFAULT ', QUOTE(v_default))
                        )
                    ),
                    IF(v_extra LIKE '%auto_increment%', ' AUTO_INCREMENT', ''),
                    IF(v_extra LIKE '%on update%', CONCAT(' ', REGEXP_REPLACE(v_extra, 'DEFAULT_GENERATED ?', '')), '')
                )
            ),
            ' COMMENT ', QUOTE(v_comment)
        );
        SET v_sql = CONCAT('ALTER TABLE `', v_table_name, '` MODIFY COLUMN `', v_column_name, '` ', v_definition);
        SET @column_comment_sql = v_sql;
        PREPARE statement FROM @column_comment_sql;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END LOOP;
    CLOSE column_cursor;
END $$

CALL add_missing_column_comments() $$
DROP PROCEDURE add_missing_column_comments $$

DELIMITER ;
