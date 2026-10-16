-- 用户角色
--
-- 改造前 users 表连角色字段都没有，系统里根本不存在「管理员」这个概念：
-- /api/banner/admin/** 只校验「是否登录」，任何注册用户都能改/删首页轮播图。
-- 这里补上角色字段，默认 USER（最小权限），管理员需要显式提权：
--   UPDATE users SET role = 'ADMIN' WHERE username = '你的账号';
ALTER TABLE `users`
  ADD COLUMN `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'USER'
  COMMENT '角色：USER-普通用户 ADMIN-管理员' AFTER `status`;
