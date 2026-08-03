UPDATE sys_menu
SET name = '账号处置',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9191
  AND permission = 'community:sanction:list';

UPDATE sys_menu
SET name = '查看社区处置',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9192
  AND permission = 'community:sanction:list';

UPDATE sys_menu
SET name = '执行社区处置',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9193
  AND permission = 'community:sanction:handle';
