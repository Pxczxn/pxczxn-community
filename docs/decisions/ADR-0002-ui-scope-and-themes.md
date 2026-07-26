# ADR-0002 前端原型范围与主题范围

## 状态

已接受，2026-07-25。

## 决策

- 用户提供的八组页面原型只约束 `pxczxn-web` 博客用户端，要求按原型
  一比一实现结构、视觉层级、间距、组件状态和响应式重组。
- `pxczxn-admin` 不复刻博客端原型。
- 管理端以 `https://gitee.com/Marsfactory/mars-admin` 为脚手架，保留
  Vue 3、TypeScript、Vite、Naive UI、Pinia 和 Vue Router。
- 用户端与管理端均实现浅色、深色、星空三套主题。
- 两端共享品牌色与语义 Token，但保持不同的信息密度和页面形态。

## 对原设计文档的覆盖

`pxczxn-ui-design-system.md` 原先说明管理端 V1 不强制星空主题。用户的
最新明确要求将管理端星空主题提升为 V1 必做，本文档覆盖该旧决策。

## 视觉资料

完整原型归档在：

```text
docs/design/references/pxczxn-web-prototype.png
```
