# TPS 需求追踪矩阵

> 文档说明：本文件用于跟踪需求、设计、实现与测试之间的对应关系。


**版本：** v1.0  
**日期：** 2026-05-05  
**用途：** 建立“需求 -> 用例 -> 设计 -> 接口 -> 测试”的闭环追踪

---

## 1. 追踪原则

- 每条需求都应能追溯到对应用例、接口和测试项。
- 已实现功能若未进入文档，应补充到设计或测试中。
- 需求变更后，优先更新本表，再同步其它文档。

---

## 2. 追踪矩阵

| 需求编号 | 需求描述 | 对应用例/图 | 对应接口 | 对应测试 |
|---|---|---|---|---|
| R-01 | 学号/手机号注册登录 | 用例图：注册/登录 | `/api/auth/code` `/api/auth/register` `/api/auth/login` `/api/auth/refresh` | 登录、注册、刷新 token |
| R-02 | 个人资料维护 | 用例图：维护个人资料 | `/api/users/me` `/api/users/me/avatar` `/api/users/me/deactivate` | 资料修改、注销账号 |
| R-03 | 商品发布与编辑 | 用例图：发布商品、编辑商品 | `/api/products` `/api/products/{id}` `/api/files/upload` | 发布、编辑、图片上传 |
| R-04 | 商品下架与重新上架 | 用例图：下架/重新上架商品 | `/api/products/{id}/status` | 商品状态切换 |
| R-05 | 商品浏览与搜索 | 用例图：浏览商品、搜索与筛选商品 | `/api/products` `/api/products/search` `/api/products/{id}` | 列表、筛选、详情 |
| R-06 | 收藏与浏览历史 | 用例图：收藏商品、查看浏览历史 | `/api/favorites/**` `/api/users/**` | 收藏、取消收藏、历史记录 |
| R-07 | 会话与私信 | 用例图：发起会话/私信沟通 | `/api/messages/conversation` `/api/messages/conversations` `/api/messages/{conversationId}` | 会话创建、消息收发 |
| R-08 | 订单创建与流转 | 用例图：创建订单、支付订单、发货、确认收货 | `/api/orders/**` | 下单、支付、发货、确认 |
| R-09 | 退款流程 | 用例图：申请退款 | `/api/orders/{id}/refund` `/api/orders/{id}/refund/approve` | 退款申请、审批 |
| R-10 | 交易评价 | 用例图：评价交易对象 | `/api/orders/{id}/review` | 完成后评价、重复评价拦截 |
| R-11 | 商品举报 | 用例图：举报商品 | `/api/products/{id}/report` `/api/admin/reports/{id}/handle` | 举报提交、管理员处理 |
| R-12 | 管理员治理 | 用例图：管理用户、处理举报、强制下架商品、查看订单并处理纠纷 | `/api/admin/**` | 管理员权限、封禁、下架、统计 |
| R-13 | 用户反馈 | 用例图：提交反馈、回复用户反馈 | `/api/feedback/**` `/api/admin/feedback/**` | 反馈提交、后台回复 |
| R-14 | 系统通知 | 用例图：查看通知 | `/api/notifications/**` | 通知生成、已读状态 |

