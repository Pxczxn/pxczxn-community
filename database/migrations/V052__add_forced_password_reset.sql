SET NAMES utf8mb4;
ALTER TABLE `community_user_login_account`
  ADD COLUMN `force_password_change` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须在下次登录后修改密码';
