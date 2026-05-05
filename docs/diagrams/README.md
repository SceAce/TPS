# TPS 软件工程课程图目录

> 文档说明：本文件说明各软件工程图的编号、用途与重新渲染方式。


本目录用于存放《软件工程》课程常见分析与设计图，内容基于当前 TPS 校园二手交易 APP 的实际实现与已有需求整理而来。

## 图清单

- `01-use-case.puml`：系统用例图
- `02-system-context.puml`：系统上下文图
- `03-architecture.puml`：总体架构图
- `04-er-diagram.puml`：数据库 ER 图
- `05-class-diagram.puml`：核心类图
- `06-sequence-publish-product.puml`：发布商品时序图
- `07-state-order.puml`：订单状态图
- `08-navigation.puml`：Android 页面导航图

## 使用建议

- 课程报告中的“需求分析”章节优先使用：用例图、系统上下文图。
- “概要设计”章节优先使用：总体架构图、ER 图、页面导航图。
- “详细设计”章节优先使用：类图、时序图、状态图。

## 说明

- 这些图不是照搬模板，而是按当前仓库中的 `app/`、`backend/`、`docs/`、`file_docx/` 和现有接口/实体结构整理。
- 如果后续业务继续变化，优先改 `.puml` 源文件，再统一导出图片。
