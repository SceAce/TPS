# TPS 详细设计说明书

> 文档说明：本文件记录系统详细设计，包括模块职责与关键流程。


**版本：** v1.0  
**日期：** 2026-05-05

---

## 1. 设计目标

本设计说明书描述 TPS 的核心模块结构、数据流、状态流转和异常处理，作为课程“详细设计”部分的基础材料。

---

## 2. 模块划分

### 2.1 认证模块

- 负责注册、登录、刷新 token、登出。
- 关键类：`AuthController`、`AuthService`、`JwtUtil`、`JwtAuthFilter`。
- 核心输出：`LoginResponse`。

### 2.2 商品模块

- 负责商品发布、编辑、列表、详情、搜索、下架、擦亮、举报。
- 关键类：`ProductController`、`ProductService`、`Product`、`ProductImage`。
- 核心输出：`ProductResponse`。

### 2.3 订单模块

- 负责订单创建、支付、发货、确认收货、退款、评价。
- 关键类：`OrderController`、`OrderService`、`Order`、`Review`。
- 核心输出：`OrderResponse`、`ReviewResponse`。

### 2.4 消息模块

- 负责会话创建、历史消息、消息发送、已读标记。
- 关键类：`MessageController`、`MessageService`、`ChatController`、`Conversation`、`Message`。

### 2.5 管理员模块

- 负责用户管理、商品治理、举报处理、订单查看、公告发布、反馈处理。
- 关键类：`AdminController`、`AdminService`、`FeedbackService`。

### 2.6 通知模块

- 负责通知列表查询、单条已读、全部已读。
- 关键类：`NotificationController`、`NotificationRepository`、`Notification`。

### 2.7 用户反馈模块

- 负责用户提交反馈、查看反馈、管理员回复与状态维护。
- 关键类：`FeedbackController`、`FeedbackService`、`Feedback`。

---

## 3. 核心状态设计

### 3.1 商品状态

- `ON_SALE`：在售
- `SOLD`：已售出
- `OFF`：下架

规则：

- 发布成功后进入 `ON_SALE`。
- 创建订单时商品可被临时置为 `OFF`。
- 完成交易后变为 `SOLD`。

### 3.2 订单状态

- `PENDING`：待付款
- `PAID`：已付款
- `SHIPPED`：已发货
- `DONE`：已完成
- `CANCELLED`：已取消
- `REFUNDING`：退款中
- `REFUNDED`：已退款

### 3.3 反馈状态

- `PENDING`：待处理
- `PROCESSING`：处理中
- `DONE`：已回复
- `CLOSED`：已关闭

---

## 4. 核心业务流程

### 4.1 发布商品

1. 用户提交商品信息。
2. 上传图片并返回图片地址。
3. 后端保存商品与图片记录。
4. 返回商品详情页。

### 4.2 创建订单

1. 买家提交商品和成交价。
2. 后端校验商品状态与价格合法性。
3. 生成订单并通知卖家。
4. 商品进入不可重复购买约束状态。

### 4.3 评价交易

1. 订单完成后双方可评价。
2. 每个订单每位用户只能评价一次。
3. 被评价者信用分更新。

### 4.4 举报处理

1. 用户提交举报。
2. 管理员查看举报列表。
3. 管理员可下架商品并标记举报为已处理。

### 4.5 通知处理

1. 系统在订单、评价、退款、公告等场景生成通知。
2. 用户查询通知列表。
3. 用户可单条已读或全部已读。

### 4.6 反馈处理

1. 用户提交反馈。
2. 系统保存反馈为待处理状态。
3. 管理员查看反馈列表并回复。
4. 系统更新反馈状态和回复内容。

---

## 5. 异常设计

- 登录失败：返回 401 或 400。
- 无权限访问：返回 403。
- 商品不存在：返回 404。
- 状态冲突：返回 409。
- 重复评价/重复举报：返回 400。

---

## 6. 接口层与业务层对应关系

| 控制器 | 主要职责 | 对应业务服务 |
|---|---|---|
| `AuthController` | 注册、登录、刷新 token | `AuthService` |
| `UserController` | 个人资料、头像、账号注销 | `UserService` |
| `ProductController` | 商品列表、详情、发布、编辑、状态修改、举报 | `ProductService` |
| `OrderController` | 下单、支付、发货、确认、退款、评价 | `OrderService` |
| `MessageController` | 会话、历史消息、已读 | `MessageService` |
| `ChatController` | WebSocket 消息收发 | `MessageService` |
| `NotificationController` | 通知查询与已读 | `NotificationRepository` |
| `FeedbackController` | 用户提交反馈和查看记录 | `FeedbackService` |
| `AdminController` | 用户管理、商品治理、订单查看、反馈处理 | `AdminService`、`FeedbackService` |

---

## 7. Android 客户端设计说明

- 界面层采用 Jetpack Compose。
- 状态管理采用 ViewModel + StateFlow。
- 网络访问通过 Retrofit 接口定义。
- 登录态由 `TokenManager` 管理。
- 实时消息通过 `StompClient` 处理。

主要页面：

- 认证页：登录、注册
- 商品页：首页、商品详情、发布商品
- 消息页：会话列表、聊天页
- 个人中心：资料、我的商品、收藏、历史、反馈
- 管理员页面：用户、商品、订单、统计
