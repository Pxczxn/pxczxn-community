SET NAMES utf8mb4;

-- ======================================================================
-- V074: 社区管理端导航信息架构重组（安全子集）
-- 对应提示词第十五~十七节。本迁移只执行可安全幂等的改动：
--   1. 「动态治理」菜单更名为「动态管理」（产品定位调整）
--   2. 下沉/隐藏 OA 与开发工具菜单，避免干扰社区运营主导航
-- 完整的一级目录归位（用户与博客/内容管理/审核中心/社区治理/标签与系列/
-- 通知与沟通）因存在历史 path 冲突（系列审核与系列管理共用 /community/series），
-- 建议在「系统设置 - 菜单管理」页面手动拖拽调整父子层级，避免自动 SQL 误伤。
-- ======================================================================

-- 1. 动态治理 → 动态管理
UPDATE `sys_menu`
SET `name` = '动态管理', `update_time` = CURRENT_TIMESTAMP
WHERE `path` = '/community/moments'
  AND `type` = 2
  AND `deleted` = 0;

-- 2. 隐藏 OA / 脚手架遗留 / 开发工具菜单（普通社区管理员不应在主导航看到）
--    说明：平台组织/平台岗位/客户管理/测试菜单/代码生成
--    若超管或开发角色需要访问，可在「菜单管理」单独为其角色授权后保持 visible=1。
UPDATE `sys_menu`
SET `visible` = 0, `update_time` = CURRENT_TIMESTAMP
WHERE `path` IN (
  '/org/dept',
  '/org/post',
  '/system/customer',
  '/test/test',
  '/tool/gen'
)
  AND `deleted` = 0;
