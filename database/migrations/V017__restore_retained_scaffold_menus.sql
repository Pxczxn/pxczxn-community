SET NAMES utf8mb4;

-- Stage 6 policy changed from deletion to retention. Keep inactive scaffold
-- capabilities available for later extension, while exposing only the retained
-- server monitor and the now-supported instant chat module.
INSERT INTO `sys_menu` (
    `id`, `parent_id`, `name`, `type`, `path`, `component`, `permission`,
    `icon`, `sort`, `visible`, `status`, `is_frame`,
    `create_time`, `update_time`, `create_by`, `update_by`, `deleted`
) VALUES
    (45, 36, '服务监控', 2, '/monitor/server', '/monitor/server/index', 'monitor:server:list', 'DesktopOutline', 4, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (139, 134, '即时聊天', 2, '/message/chat', '/message/chat/index', 'sys:chat:list', 'ChatbubbleOutline', 2, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (140, 0, '测试菜单', 1, '/test', NULL, NULL, 'StarOutline', 7, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (141, 140, '测试菜单', 2, '/test/test', '/test/test/index', NULL, 'SearchOutline', 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (154, 36, '服务器管理', 2, '/monitor/server-manager', '/monitor/server-manager/index', NULL, 'ServerOutline', 5, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (155, 154, '服务器列表', 3, NULL, NULL, 'monitor:server:list', NULL, 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (156, 154, '服务器详情', 3, NULL, NULL, 'monitor:server:query', NULL, 2, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (157, 154, '新增服务器', 3, NULL, NULL, 'monitor:server:add', NULL, 3, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (158, 154, '编辑服务器', 3, NULL, NULL, 'monitor:server:edit', NULL, 4, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (159, 154, '删除服务器', 3, NULL, NULL, 'monitor:server:remove', NULL, 5, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (160, 154, '测试连接', 3, NULL, NULL, 'monitor:server:test', NULL, 6, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (162, 161, '代码生成', 2, '/tool/gen', '/tool/gen/index', NULL, 'CodeSlashOutline', 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (163, 162, '查询', 3, NULL, NULL, 'tool:gen:list', NULL, 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (164, 162, '详情', 3, NULL, NULL, 'tool:gen:query', NULL, 2, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (165, 162, '导入', 3, NULL, NULL, 'tool:gen:import', NULL, 3, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (166, 162, '编辑', 3, NULL, NULL, 'tool:gen:edit', NULL, 4, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (167, 162, '删除', 3, NULL, NULL, 'tool:gen:remove', NULL, 5, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (168, 162, '预览', 3, NULL, NULL, 'tool:gen:preview', NULL, 6, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (169, 162, '生成代码', 3, NULL, NULL, 'tool:gen:code', NULL, 7, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (170, 1, '客户表', 2, '/system/customer', '/system/customer/index', NULL, 'ListOutline', 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (171, 170, '客户表查询', 3, NULL, NULL, 'system:customer:list', NULL, 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (172, 170, '客户表详情', 3, NULL, NULL, 'system:customer:query', NULL, 2, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (173, 170, '客户表新增', 3, NULL, NULL, 'system:customer:add', NULL, 3, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (174, 170, '客户表修改', 3, NULL, NULL, 'system:customer:edit', NULL, 4, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (175, 170, '客户表删除', 3, NULL, NULL, 'system:customer:remove', NULL, 5, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (327, 161, '学生表', 2, 'system/student', 'system/student/index', 'system:student:list', 'ListOutline', 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (328, 327, '学生表查询', 3, NULL, NULL, 'system:student:list', NULL, 1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (329, 327, '学生表详情', 3, NULL, NULL, 'system:student:query', NULL, 2, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (330, 327, '学生表新增', 3, NULL, NULL, 'system:student:add', NULL, 3, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (331, 327, '学生表修改', 3, NULL, NULL, 'system:student:edit', NULL, 4, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0),
    (332, 327, '学生表删除', 3, NULL, NULL, 'system:student:remove', NULL, 5, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `type` = VALUES(`type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `permission` = VALUES(`permission`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `is_frame` = VALUES(`is_frame`),
    `update_time` = CURRENT_TIMESTAMP,
    `update_by` = VALUES(`update_by`),
    `deleted` = VALUES(`deleted`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 45), (1, 139), (1, 140), (1, 141),
    (1, 154), (1, 155), (1, 156), (1, 157), (1, 158), (1, 159), (1, 160),
    (1, 162), (1, 163), (1, 164), (1, 165), (1, 166), (1, 167), (1, 168), (1, 169),
    (1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175),
    (1, 327), (1, 328), (1, 329), (1, 330), (1, 331), (1, 332),
    (2, 45), (2, 139);
