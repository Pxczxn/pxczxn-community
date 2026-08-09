-- 星语社区管理端品牌收口。
-- 可重复执行，用于升级已经应用过 V006 的环境。

UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
        COALESCE(`config_value`, JSON_OBJECT()),
        '$.siteName', '星语社区运营中心',
        '$.siteDescription', '星语社区运营与治理中心',
        '$.copyright', '版权所有 © 星语社区 2026'
    )
WHERE `group_code` = 'system'
  AND `status` = 1;

UPDATE `sys_menu`
SET `name` = CASE `id`
        WHEN 9070 THEN '评论治理'
        WHEN 9071 THEN '查询评论'
        WHEN 9072 THEN '查看评论治理事件'
        WHEN 9073 THEN '通过评论审核'
        WHEN 9074 THEN '驳回评论审核'
        WHEN 9075 THEN '下架评论'
        WHEN 9076 THEN '恢复评论'
        WHEN 9077 THEN '批量治理评论'
        WHEN 9080 THEN '动态治理'
        WHEN 9081 THEN '查询动态'
        WHEN 9082 THEN '查看动态治理事件'
        WHEN 9083 THEN '通过动态审核'
        WHEN 9084 THEN '驳回动态审核'
        WHEN 9085 THEN '下架动态'
        WHEN 9086 THEN '恢复动态'
        WHEN 9087 THEN '批量治理动态'
        WHEN 9090 THEN '互动查询'
        WHEN 9091 THEN '查询互动关系'
        ELSE `name`
    END,
    `update_by` = 1
WHERE `id` BETWEEN 9070 AND 9091
  AND `deleted` = 0;
