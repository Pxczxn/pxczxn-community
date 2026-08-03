SELECT CASE WHEN
 (SELECT COUNT(*) FROM sys_menu WHERE id=9194 AND name='申诉中心' AND permission='community:appeal:list' AND deleted=0)=1
 AND (SELECT COUNT(*) FROM sys_menu WHERE id IN (9195,9196) AND parent_id=9194 AND permission IN ('community:appeal:list','community:appeal:handle') AND deleted=0)=2
 AND (SELECT COUNT(*) FROM sys_menu WHERE id IN (9200,9201,9202,9203,9204) AND parent_id=9199 AND permission IN ('community:account:list','community:account:freeze','community:account:apply','community:account:approve','community:account:execute') AND deleted=0)=5
THEN 'PASS' ELSE 'FAIL' END AS result;
