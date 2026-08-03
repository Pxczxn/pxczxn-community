SELECT CASE WHEN COUNT(*) = 3 THEN 'PASS' ELSE 'FAIL' END AS result
FROM `sys_menu`
WHERE `deleted` = 0
  AND (
       (`id` = 9194 AND `parent_id` = 0 AND `name` = '申诉中心' AND `icon` = 'ChatbubblesOutline')
    OR (`id` = 9191 AND `parent_id` = 0 AND `name` = '账号处置' AND `icon` = 'HammerOutline')
    OR (`id` = 9199 AND `parent_id` = 0 AND `name` = '账号操作中心' AND `icon` = 'KeyOutline')
  );
