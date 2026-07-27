# ADR-0001：复用 Mars Admin 脚手架并渐进迁移命名

状态：已完成  
日期：2026-07-25  
更新：2026-07-27（M2.5.1 Mars 运行时命名清理完成）

## 背景

工程文档要求复用 `pxczxn-admin` 的管理员登录、RBAC、文件、日志和定时任务
能力。当前工作目录最初只有工程文档，但相邻目录存在与文档描述一致、可独立
编译的 Mars Admin 源码和已初始化后台数据库。

## 决策

将该脚手架作为 `pxczxn-backend` 与 `pxczxn-admin` 的初始来源：

- 新增模块、Maven 坐标和社区 Java 包统一使用 `pxczxn`。
- 旧后台 `com.mars` 包暂时保留，避免一次性重命名 200 多个类引入回归。
- 旧 App、UniApp 和示例业务不进入当前后端 Reactor。
- 社区用户不复用 `sys_user`、管理员 `StpUtil` 或管理员 Token。
- 每个 M1 任务完成后继续收敛遗留命名和不必要依赖。

## 结果

可以立即复用后台能力并保持真实可运行基线，同时把新社区业务限制在
`top.pxczxn.community` 中。代价是短期内仓库存在 `pxczxn` 与 `mars`
两套内部命名，需通过后续任务渐进清理。

## 迁移历史

- **阶段 4（2026-07-26）**：完成当前 Reactor 模块命名迁移，`com.mars` 包改为 `top.pxczxn.platform`，配置前缀改为 `pxczxn.*`。未进入 Reactor 的 `mars-app-api`、`mars-web-api`、`mars-biz` 三个目录暂时保留。
- **阶段 5（2026-07-26）**：完成数据库名称迁移，从 `mars-system` 迁移到 `pxczxn_community`。
- **M2.5.1-T001（2026-07-27）**：删除未被引用的三个 Mars 遗留模块目录（`mars-app-api`、`mars-web-api`、`mars-biz`），运行时代码中不再存在 Mars 命名。历史备份文件和交付文档中保留了迁移来源说明。

Mars Admin 脚手架的 MIT 许可证声明保留在根目录 `THIRD_PARTY_NOTICES.md` 中。
