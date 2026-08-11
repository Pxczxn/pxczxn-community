SET NAMES utf8mb4;

SELECT CASE
    WHEN (
        SELECT COUNT(*)
        FROM sys_menu
        WHERE path = '/community/moments'
          AND type = 2
          AND deleted = 0
    ) = 1
    AND (
        SELECT COUNT(*)
        FROM sys_menu
        WHERE path IN ('/org/dept', '/org/post', '/system/customer', '/test/test', '/tool/gen')
          AND deleted = 0
          AND visible = 0
    ) = 5
    THEN 'PASS'
    ELSE 'FAIL'
END AS community_navigation_restructure_ready;
