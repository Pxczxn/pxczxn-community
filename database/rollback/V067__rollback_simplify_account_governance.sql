/* Protected rollback: do not drop two-stage appeal columns automatically because they contain audit evidence.
   Restore only the menu label when an operator has separately archived the new audit data. */
UPDATE `sys_menu` SET `name`='账号操作中心', `update_time`=CURRENT_TIMESTAMP
WHERE `permission`='community:account:list' AND `deleted`=0;
