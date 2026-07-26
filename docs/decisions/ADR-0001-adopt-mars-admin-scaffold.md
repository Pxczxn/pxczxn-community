# ADR-0001：复用 Mars Admin 脚手架并渐进迁移命名

状态：已接受  
日期：2026-07-25

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
