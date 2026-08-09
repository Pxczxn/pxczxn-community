UPDATE sys_menu
SET name = '账号管理',
    update_time = CURRENT_TIMESTAMP
WHERE id = 9191
  AND permission = 'community:sanction:list';
