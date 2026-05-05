# TPS 数据库设计细化

> 文档说明：本文件说明数据库表设计、字段关系与约束考虑。


**版本：** v1.0  
**日期：** 2026-05-05

---

## 1. 设计目标

数据库设计以校园二手交易闭环为中心，覆盖用户、商品、订单、消息、评价、举报、通知、反馈和浏览历史。

---

## 2. 核心表说明

### 2.1 users

- 主键：`id`
- 唯一约束：`phone`、`student_id`
- 关键字段：`role`、`status`、`credit_score`
- 用途：保存用户账号、身份和信用信息。

### 2.2 products

- 主键：`id`
- 外键逻辑：`user_id -> users.id`
- 索引：`status`、`category`、`created_at`
- 用途：保存商品主信息与状态信息。

### 2.3 product_images

- 主键：`id`
- 外键逻辑：`product_id -> products.id`
- 用途：保存商品多图和排序。

### 2.4 orders

- 主键：`id`
- 外键逻辑：`product_id -> products.id`
- 索引：`buyer_id`、`seller_id`
- 用途：保存交易订单、退款和物流信息。

### 2.5 conversations / messages

- `conversations` 保存买卖双方会话和未读数。
- `messages` 保存会话内消息内容、类型和已读状态。

### 2.6 reviews

- 用于交易完成后的双向评价。
- `reviewer_id` 表示评价人，`reviewee_id` 表示被评价人。

### 2.7 reports

- 用于商品举报与管理员处理。
- `status` 统一为 `PENDING` / `HANDLED`。

### 2.8 notifications

- 用于系统通知、交易通知和管理通知。
- `is_read` 表示是否已读。

### 2.9 browsing_history

- 记录用户浏览轨迹。
- 建议以 `(user_id, product_id)` 为唯一约束。

### 2.10 feedback

- 用于用户反馈与后台回复。
- `status` 支持待处理、处理中、完成、关闭。

---

## 3. 关键约束

- `favorites`：`(user_id, product_id)` 唯一。
- `conversations`：`(buyer_id, seller_id, product_id)` 唯一。
- `browsing_history`：建议按用户+商品去重。
- 订单状态变更必须遵循状态机。

---

## 4. 索引设计

### 4.1 商品表索引

- `idx_user_id`：支持按卖家查询商品。
- `idx_status`：支持按状态筛选。
- `idx_category`：支持按分类筛选。
- `idx_created_at`：支持按发布时间排序。

### 4.2 消息表索引

- `idx_conversation_id`：支持会话消息查询。
- `idx_created_at`：支持按时间排序。

### 4.3 订单表索引

- `idx_buyer_id`：支持按买家查看订单。
- `idx_seller_id`：支持按卖家查看订单。

### 4.4 通知与反馈表索引

- 通知表：`idx_user_id`
- 反馈表：`idx_feedback_user`、`idx_feedback_status`

---

## 5. 数据完整性说明

- 当前实现中，大部分关联通过业务逻辑维护，而不是数据库外键强绑定。
- 这样做便于本地开发迁移，但生产环境建议补充更严格的约束治理策略。
- 信用分由评价记录动态计算，不直接由客户端修改。
- 商品收藏数和浏览量属于冗余统计字段，需要由业务层维护一致性。
