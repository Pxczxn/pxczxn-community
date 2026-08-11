SET NAMES utf8mb4;

SELECT CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS community_preference_settings_json_ready
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'community_user_preference'
  AND column_name = 'settings_json'
  AND data_type = 'json';
