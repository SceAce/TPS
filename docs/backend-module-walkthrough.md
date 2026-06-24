# TPS 后端核心模块讲解

本文用于讲解校园二手交易平台后端核心业务。范围以 `backend/src/main/java/com/tps` 为主，前端暂不展开。

## 1. 后端总体结构

后端采用典型 Spring Boot 分层结构：

- `controller/`：HTTP 接口入口，负责接收参数、读取当前登录用户、返回统一响应。
- `service/`：核心业务规则，负责事务、状态流转、权限校验、敏感词拦截、通知创建。
- `repository/`：Spring Data JPA 数据访问，包含分页查询、条件查询、悲观锁更新。
- `entity/`：数据库实体，定义商品、订单、用户、会话、评价、举报等核心领域对象。
- `config/`、`security/`：JWT 鉴权、RBAC 权限控制、统一异常响应、WebSocket 鉴权。

统一响应入口在：

- `backend/src/main/java/com/tps/dto/ApiResponse.java`
- `backend/src/main/java/com/tps/dto/PageResponse.java`
- `backend/src/main/java/com/tps/config/GlobalExceptionHandler.java`

安全控制入口在：

- `backend/src/main/java/com/tps/config/SecurityConfig.java`
- `backend/src/main/java/com/tps/security/JwtAuthFilter.java`

核心安全规则：

```java
// backend/src/main/java/com/tps/config/SecurityConfig.java
sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/img/**", "/ws/**",
        "/api/products", "/api/products/search", "/api/products/{id}")
    .permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

讲解要点：

- 用户端商品列表、搜索、详情允许未登录访问；登录后会带上收藏状态和浏览历史。
- `/api/admin/**` 统一要求 `ADMIN` 角色，后台 Controller 还使用 `@PreAuthorize("hasRole('ADMIN')")` 做方法级保护。
- 后端不使用服务端 Session，身份来自 JWT。

敏感词和错误返回是跨模块能力：

```java
// backend/src/main/java/com/tps/service/SensitiveWordService.java
public void rejectIfSensitiveFields(FieldContent... fields) {
    List<SensitiveContentException.FieldViolation> violations = new ArrayList<>();
    for (FieldContent field : fields) {
        if (field != null && containsSensitive(field.value())) {
            violations.add(new SensitiveContentException.FieldViolation(
                    field.field(),
                    field.label(),
                    field.label() + "包含敏感词"
            ));
        }
    }
    if (!violations.isEmpty()) {
        throw new SensitiveContentException(violations);
    }
}
```

```java
// backend/src/main/java/com/tps/config/GlobalExceptionHandler.java
@ExceptionHandler(SensitiveContentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<?> handleSensitiveContent(SensitiveContentException e) {
    return ApiResponse.fail(400, e.getMessage(),
            new SensitiveContentErrorResponse(e.getFields()));
}
```

讲解要点：

- 普通业务错误返回 `400 + message`。
- 敏感词错误返回 `400 + message + data.fields`，前端可以据此弹窗提示“标题包含敏感词”“描述包含敏感词”等具体字段。

## 2. 商品发布与下架

核心代码路径：

- `backend/src/main/java/com/tps/controller/ProductController.java`
- `backend/src/main/java/com/tps/service/ProductService.java`
- `backend/src/main/java/com/tps/entity/Product.java`
- `backend/src/main/java/com/tps/repository/ProductRepository.java`
- `backend/src/main/java/com/tps/controller/AdminController.java`
- `backend/src/main/java/com/tps/service/AdminService.java`

核心接口：

| 功能 | 接口 | 入口方法 |
| --- | --- | --- |
| 发布商品 | `POST /api/products` | `ProductController.create` |
| 修改商品 | `PUT /api/products/{id}` | `ProductController.update` |
| 用户上下架 | `PATCH /api/products/{id}/status` | `ProductController.updateStatus` |
| 擦亮商品 | `POST /api/products/{id}/bump` | `ProductController.bump` |
| 举报商品 | `POST /api/products/{id}/report` | `ProductController.report` |
| 管理员强制下架 | `PUT /api/admin/products/{id}/takedown` | `AdminController.takedownProduct` |

商品状态定义：

```java
// backend/src/main/java/com/tps/entity/Product.java
public enum ProductStatus { ON_SALE, SOLD, OFF }
```

发布商品的核心逻辑：

```java
// backend/src/main/java/com/tps/service/ProductService.java
@Transactional
public ProductResponse create(Long userId, ProductRequest req) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    if (Boolean.TRUE.equals(user.getPublishBanned())) {
        throw new IllegalArgumentException("账号已被禁止发布商品，请联系管理员");
    }
    validateProductContent(req);

    Product product = new Product();
    product.setUserId(userId);
    product.setTitle(req.getTitle());
    product.setDescription(req.getDescription());
    product.setPrice(req.getPrice());
    product.setCategory(req.getCategory());
    product.setCondition(req.getCondition());
    product.setLocation(req.getLocation());
    productRepository.save(product);
    ...
}
```

发布前校验：

```java
// backend/src/main/java/com/tps/service/ProductService.java
private void validateProductContent(ProductRequest req) {
    sensitiveWordService.rejectIfSensitiveFields(
            sensitiveWordService.field("title", "标题", req.getTitle()),
            sensitiveWordService.field("description", "描述", req.getDescription()),
            sensitiveWordService.field("category", "分类", req.getCategory()),
            sensitiveWordService.field("location", "交易地点", req.getLocation())
    );
}
```

用户自主上下架的核心逻辑：

```java
// backend/src/main/java/com/tps/service/ProductService.java
@Transactional
public void updateStatus(Long userId, Long productId, Product.ProductStatus status) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    if (!product.getUserId().equals(userId)) throw new IllegalArgumentException("无权操作");
    if (status == Product.ProductStatus.SOLD) {
        throw BusinessException.conflict("商品成交状态只能由订单流程更新");
    }
    if (product.getStatus() == Product.ProductStatus.SOLD && status != Product.ProductStatus.SOLD) {
        throw BusinessException.conflict("已售出商品不能重新上架");
    }
    product.setStatus(status);
    productRepository.save(product);
}
```

讲解要点：

- 用户只能操作自己的商品。
- `SOLD` 不能由用户手动设置，只能由订单确认收货流程设置，防止绕过交易闭环。
- 已售出商品不能重新上架。

管理员强制下架：

```java
// backend/src/main/java/com/tps/service/AdminService.java
@Transactional
public void takedownProduct(Long productId, String reason, Long adminId) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    if (product.getStatus() == Product.ProductStatus.SOLD) {
        throw new IllegalArgumentException("已售出商品不能强制下架");
    }
    product.setStatus(Product.ProductStatus.OFF);
    product.setTakedownReason(reason.trim());
    product.setTakedownBy(adminId);
    product.setTakedownAt(LocalDateTime.now());
    productRepository.save(product);
    ...
}
```

举报商品：

```java
// backend/src/main/java/com/tps/service/ProductService.java
@Transactional
public void report(Long userId, Long productId, String reason, List<String> evidenceImageUrls) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    if (product.getUserId().equals(userId)) {
        throw new IllegalArgumentException("不能举报自己的商品");
    }
    if (reportRepository.existsByReporterIdAndProductIdAndStatus(
            userId, productId, Report.ReportStatus.PENDING)) {
        return;
    }
    sensitiveWordService.rejectIfSensitiveFields(
            sensitiveWordService.field("reason", "举报原因", reason)
    );
    ...
}
```

讲解要点：

- 举报不允许举报自己的商品。
- 同一用户对同一商品已有待处理举报时幂等返回，避免重复刷举报。
- 举报原因也纳入敏感词检测。
- 举报进入后台 `reports` 表，管理员在 `/api/admin/reports` 查看和处理。

擦亮商品：

```java
// backend/src/main/java/com/tps/service/ProductService.java
Product product = productRepository.findByIdForUpdate(productId)
        .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
...
if (count >= 3) {
    throw new IllegalArgumentException("每件商品每天最多擦亮3次");
}
product.setBumpCountToday(count + 1);
product.setBumpedAt(LocalDateTime.now());
```

讲解要点：

- `findByIdForUpdate` 使用悲观写锁，避免用户并发点击导致每日次数超限。
- 商品列表排序优先使用 `bumpedAt`，擦亮会影响曝光。

## 3. 搜索与浏览

核心代码路径：

- `backend/src/main/java/com/tps/controller/ProductController.java`
- `backend/src/main/java/com/tps/service/ProductService.java`
- `backend/src/main/java/com/tps/service/BrowsingHistoryService.java`
- `backend/src/main/java/com/tps/controller/BrowsingHistoryController.java`
- `backend/src/main/java/com/tps/repository/ProductRepository.java`

核心接口：

| 功能 | 接口 | 入口方法 |
| --- | --- | --- |
| 首页商品列表 | `GET /api/products` | `ProductController.list` |
| 商品搜索 | `GET /api/products/search` | `ProductController.search` |
| 商品详情 | `GET /api/products/{id}` | `ProductController.detail` |
| 浏览历史列表 | `GET /api/history/products` | `BrowsingHistoryController.list` |
| 清空浏览历史 | `DELETE /api/history/products` | `BrowsingHistoryController.clear` |

列表和搜索入口：

```java
// backend/src/main/java/com/tps/controller/ProductController.java
@GetMapping
public ApiResponse<PageResponse<ProductResponse>> list(..., Authentication authentication) {
    Long userId = extractUserId(authentication);
    if ((keyword != null && !keyword.isBlank())
            || (category != null && !category.isBlank())
            || minPrice != null || maxPrice != null
            || (condition != null && !condition.isBlank())) {
        return ApiResponse.success(PageResponse.from(
                productService.search(keyword, category, minPrice, maxPrice,
                        condition, page, size, userId)));
    }
    return ApiResponse.success(PageResponse.from(productService.list(page, size, userId)));
}
```

普通列表：

```java
// backend/src/main/java/com/tps/service/ProductService.java
public Page<ProductResponse> list(int page, int size, Long currentUserId) {
    Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("bumpedAt"), Sort.Order.desc("createdAt")));
    return productRepository.findByStatus(Product.ProductStatus.ON_SALE, pageable)
            .map(p -> toResponse(p, currentUserId));
}
```

搜索条件：

```java
// backend/src/main/java/com/tps/service/ProductService.java
public Page<ProductResponse> search(String keyword, String category, BigDecimal minPrice,
                                    BigDecimal maxPrice, String condition, int page, int size,
                                    Long currentUserId) {
    if (sensitiveWordService.containsSensitive(keyword)) {
        throw new IllegalArgumentException(SensitiveWordService.SEARCH_MESSAGE);
    }
    Specification<Product> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), Product.ProductStatus.ON_SALE));
        ...
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    return productRepository.findAll(spec, pageable).map(p -> toResponse(p, currentUserId));
}
```

讲解要点：

- 搜索只返回 `ON_SALE` 商品。
- 支持关键词、分类、价格区间、成色筛选。
- 关键词会匹配标题、描述、交易地点、分类。
- 搜索词命中敏感词时返回“无法搜索请重试”，避免把敏感词命中细节暴露在搜索场景。

浏览详情与浏览历史：

```java
// backend/src/main/java/com/tps/controller/ProductController.java
@GetMapping("/{id}")
public ApiResponse<ProductResponse> detail(@PathVariable Long id, Authentication authentication) {
    Long userId = extractUserId(authentication);
    if (userId != null) {
        browsingHistoryService.record(userId, id);
    }
    return ApiResponse.success(productService.getDetail(id, userId));
}
```

```java
// backend/src/main/java/com/tps/service/ProductService.java
@Transactional
public ProductResponse getDetail(Long productId, Long currentUserId) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    productRepository.incrementViewCount(productId);
    return toResponse(product, currentUserId);
}
```

```java
// backend/src/main/java/com/tps/service/BrowsingHistoryService.java
@Transactional
public void record(Long userId, Long productId) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    if (product.getUserId().equals(userId)) {
        return;
    }
    BrowsingHistory history = historyRepository.findByUserIdAndProductId(userId, productId)
            .orElseGet(() -> { ... });
    history.setViewedAt(LocalDateTime.now());
    historyRepository.save(history);
}
```

讲解要点：

- 未登录用户可以看商品详情，但不会记录浏览历史。
- 登录用户浏览他人商品时记录或更新时间。
- 浏览自己的商品不记录历史，避免污染用户浏览记录。
- 管理员查看商品详情使用 `getDetailWithoutIncrement`，不会增加浏览量。

## 4. 交流与沟通

核心代码路径：

- `backend/src/main/java/com/tps/controller/MessageController.java`
- `backend/src/main/java/com/tps/service/MessageService.java`
- `backend/src/main/java/com/tps/websocket/ChatController.java`
- `backend/src/main/java/com/tps/config/WebSocketConfig.java`
- `backend/src/main/java/com/tps/entity/Conversation.java`
- `backend/src/main/java/com/tps/entity/Message.java`

核心接口：

| 功能 | 接口 | 入口方法 |
| --- | --- | --- |
| 创建或获取会话 | `POST /api/messages/conversation` | `MessageController.getOrCreateConversation` |
| 会话列表 | `GET /api/messages/conversations` | `MessageController.getConversations` |
| 消息列表 | `GET /api/messages/{conversationId}` | `MessageController.getMessages` |
| REST 发送消息 | `POST /api/messages/{conversationId}` | `MessageController.sendMessage` |
| 标记已读 | `PUT /api/messages/{conversationId}/read` | `MessageController.markRead` |
| WebSocket 发消息 | `/app/chat.send` | `ChatController.sendMessage` |

会话模型：

```java
// backend/src/main/java/com/tps/entity/Conversation.java
private Long buyerId;
private Long sellerId;
private Long productId;
private String lastMessage;
private Integer unreadBuyer = 0;
private Integer unreadSeller = 0;
```

创建会话：

```java
// backend/src/main/java/com/tps/service/MessageService.java
@Transactional
public ConversationResponse getOrCreateConversation(Long userId, Long targetUserId, Long productId) {
    if (userId.equals(targetUserId)) {
        throw new IllegalArgumentException("不能和自己创建会话");
    }
    var product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

    Long sellerId = product.getUserId();
    Long buyerId = userId.equals(sellerId) ? targetUserId : userId;
    if (!targetUserId.equals(sellerId) && !userId.equals(sellerId)) {
        throw new IllegalArgumentException("会话必须包含商品卖家");
    }
    ...
}
```

讲解要点：

- 会话必须围绕某个商品建立。
- 会话必须包含商品卖家，防止两个无关用户借商品创建私聊。
- 同一商品、同一买家、同一卖家只生成一条会话。

发送消息：

```java
// backend/src/main/java/com/tps/service/MessageService.java
@Transactional
public Message sendMessage(Long senderId, Long conversationId, String content, String type) {
    if (content == null || content.isBlank()) {
        throw new IllegalArgumentException("消息内容不能为空");
    }
    sensitiveWordService.rejectIfSensitiveFields(
            sensitiveWordService.field("content", "消息内容", content)
    );
    User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    if (Boolean.TRUE.equals(sender.getMuted())) {
        throw new IllegalArgumentException("账号已被禁止发言，请联系管理员");
    }
    Conversation conv = getOwnedConversation(conversationId, senderId);
    ...
}
```

讲解要点：

- 消息内容不能为空。
- 消息内容纳入敏感词检测。
- 被管理员禁言的用户不能发送消息。
- 发送前会校验当前用户是否属于该会话。
- 会话维护买家和卖家的独立未读数。

WebSocket 复用同一套业务逻辑：

```java
// backend/src/main/java/com/tps/websocket/ChatController.java
@MessageMapping("/chat.send")
public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
    Long senderId = Long.valueOf(principal.getName());
    var savedMessage = messageService.sendMessage(
            senderId,
            chatMessage.getConversationId(),
            chatMessage.getContent(),
            chatMessage.getType() != null ? chatMessage.getType() : "TEXT"
    );
    messagingTemplate.convertAndSend(
            "/topic/conversation/" + savedMessage.getConversationId(),
            chatMessage
    );
}
```

```java
// backend/src/main/java/com/tps/config/WebSocketConfig.java
if (authorization != null && authorization.startsWith("Bearer ")) {
    String token = authorization.substring(7);
    if (jwtUtil.isTokenValid(token) && !"refresh".equals(jwtUtil.getType(token))) {
        Long userId = jwtUtil.getUserId(token);
        accessor.setUser(new UsernamePasswordAuthenticationToken(...));
    }
}
```

讲解要点：

- REST 和 WebSocket 都调用 `MessageService.sendMessage`，业务规则不会分叉。
- WebSocket 在 STOMP `CONNECT` 阶段解析 JWT，建立实时消息身份。

## 5. 评价与信用

核心代码路径：

- `backend/src/main/java/com/tps/controller/OrderController.java`
- `backend/src/main/java/com/tps/service/OrderService.java`
- `backend/src/main/java/com/tps/entity/Order.java`
- `backend/src/main/java/com/tps/entity/Review.java`
- `backend/src/main/java/com/tps/repository/ReviewRepository.java`

核心接口：

| 功能 | 接口 | 入口方法 |
| --- | --- | --- |
| 创建订单 | `POST /api/orders` | `OrderController.createOrder` |
| 我的订单 | `GET /api/orders/my` | `OrderController.myOrders` |
| 支付 | `PUT /api/orders/{id}/pay` | `OrderController.pay` |
| 发货 | `PUT /api/orders/{id}/ship` | `OrderController.ship` |
| 确认收货 | `PUT /api/orders/{id}/confirm` | `OrderController.confirm` |
| 取消订单 | `PUT /api/orders/{id}/cancel` | `OrderController.cancel` |
| 评价 | `POST /api/orders/{id}/review` | `OrderController.review` |
| 查看用户评价 | `GET /api/orders/reviews/user/{userId}` | `OrderController.getUserReviews` |

订单状态定义：

```java
// backend/src/main/java/com/tps/entity/Order.java
public enum OrderStatus {
    PENDING, PAID, SHIPPED, DONE, CANCELLED, REFUNDING, REFUNDED
}
```

创建订单：

```java
// backend/src/main/java/com/tps/service/OrderService.java
@Transactional
public OrderResponse createOrder(Long buyerId, Long productId, BigDecimal finalPrice) {
    Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
    if (product.getStatus() != Product.ProductStatus.ON_SALE)
        throw new IllegalArgumentException("商品已下架或已售出");
    if (product.getUserId().equals(buyerId))
        throw new IllegalArgumentException("不能购买自己的商品");
    if (orderRepository.existsByProductIdAndStatusIn(productId, activeStatuses()))
        throw new IllegalArgumentException("该商品已有进行中的订单");

    Order order = new Order();
    order.setStatus(Order.OrderStatus.PENDING);
    product.setStatus(Product.ProductStatus.OFF);
    ...
}
```

讲解要点：

- 创建订单时锁定商品，避免多人同时下单。
- 商品必须在售，不能购买自己的商品。
- 同一商品不能存在多个进行中的订单。
- 创建订单后商品变为 `OFF`，防止继续被搜索购买。

订单状态流转：

```java
// backend/src/main/java/com/tps/service/OrderService.java
public void pay(Long orderId, Long userId) {
    if (!order.getBuyerId().equals(userId)) throw new IllegalArgumentException("只有买家可以支付");
    if (order.getStatus() != Order.OrderStatus.PENDING) throw new IllegalArgumentException("订单状态不正确");
    order.setStatus(Order.OrderStatus.PAID);
}

public void ship(Long orderId, Long userId, String trackingNumber) {
    if (!order.getSellerId().equals(userId)) throw new IllegalArgumentException("只有卖家可以发货");
    if (order.getStatus() != Order.OrderStatus.PAID) throw new IllegalArgumentException("订单未支付");
    order.setStatus(Order.OrderStatus.SHIPPED);
}

public void confirm(Long orderId, Long userId) {
    if (!order.getBuyerId().equals(userId)) throw new IllegalArgumentException("只有买家可以确认收货");
    if (order.getStatus() != Order.OrderStatus.SHIPPED) throw new IllegalArgumentException("订单未发货");
    order.setStatus(Order.OrderStatus.DONE);
    product.setStatus(Product.ProductStatus.SOLD);
}
```

正常交易链路：

```text
商品 ON_SALE
  -> 买家创建订单：订单 PENDING，商品 OFF
  -> 买家支付：订单 PAID
  -> 卖家发货：订单 SHIPPED
  -> 买家确认：订单 DONE，商品 SOLD
```

取消订单：

```java
// backend/src/main/java/com/tps/service/OrderService.java
if (order.getStatus() == Order.OrderStatus.DONE)
    throw new IllegalArgumentException("已完成的订单不能取消");
if (order.getStatus() == Order.OrderStatus.SHIPPED)
    throw new IllegalArgumentException("已发货订单不能直接取消");
order.setStatus(Order.OrderStatus.CANCELLED);
if (product.getStatus() == Product.ProductStatus.OFF) {
    product.setStatus(Product.ProductStatus.ON_SALE);
}
```

讲解要点：

- 未完成且未发货订单可以取消。
- 取消后如果商品还只是被订单占用，则恢复为 `ON_SALE`。

评价与信用分：

```java
// backend/src/main/java/com/tps/service/OrderService.java
@Transactional
public ReviewResponse review(Long orderId, Long userId, Integer score, String content) {
    if (score == null || score < 1 || score > 5) {
        throw new IllegalArgumentException("评分必须为1-5");
    }
    Order order = getOwnedOrder(orderId, userId);
    if (order.getStatus() != Order.OrderStatus.DONE) {
        throw new IllegalArgumentException("订单完成后才能评价");
    }
    if (reviewRepository.existsByOrderIdAndReviewerId(orderId, userId)) {
        throw new IllegalArgumentException("不能重复评价");
    }
    sensitiveWordService.rejectIfSensitiveFields(
            sensitiveWordService.field("content", "评价内容", content)
    );
    ...
    updateCreditScore(revieweeId);
}
```

```java
// backend/src/main/java/com/tps/service/OrderService.java
private void updateCreditScore(Long userId) {
    List<Review> reviews = reviewRepository.findByRevieweeId(userId);
    double average = reviews.stream().mapToInt(Review::getScore).average().orElse(5.0);
    user.setCreditScore((int) Math.round(average * 20));
}
```

讲解要点：

- 订单完成后才能评价。
- 同一用户对同一订单只能评价一次。
- 评价内容纳入敏感词检测。
- 信用分由被评价人的历史评分平均值换算，`1-5` 分映射到 `20-100` 分。

退款链路：

- 买家在 `PAID` 或 `SHIPPED` 状态可申请退款，订单变为 `REFUNDING`。
- 卖家或管理员同意退款后订单变为 `REFUNDED`，商品恢复 `ON_SALE`。
- 管理员驳回退款后订单恢复到 `PAID` 或 `SHIPPED`。

## 6. 后台管理

核心代码路径：

- `backend/src/main/java/com/tps/controller/AdminController.java`
- `backend/src/main/java/com/tps/service/AdminService.java`
- `backend/src/main/java/com/tps/config/SecurityConfig.java`
- `backend/src/main/java/com/tps/entity/User.java`
- `backend/src/main/java/com/tps/entity/Report.java`

后台权限入口：

```java
// backend/src/main/java/com/tps/controller/AdminController.java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    ...
}
```

```java
// backend/src/main/java/com/tps/config/SecurityConfig.java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

核心后台接口：

| 模块 | 接口 | 功能 |
| --- | --- | --- |
| 用户管理 | `GET /api/admin/users` | 用户分页、状态筛选、关键词搜索、排序 |
| 用户管理 | `PUT /api/admin/users/{id}/ban` | 封禁账号 |
| 用户管理 | `PUT /api/admin/users/{id}/unban` | 解除封禁 |
| 用户管理 | `PUT /api/admin/users/{id}/mute` | 禁止发言 |
| 用户管理 | `PUT /api/admin/users/{id}/unmute` | 解除禁言 |
| 用户管理 | `PUT /api/admin/users/{id}/publish-ban` | 禁止发布商品 |
| 用户管理 | `PUT /api/admin/users/{id}/publish-unban` | 解除禁止发布 |
| 商品管理 | `GET /api/admin/products` | 商品分页、状态、分类、卖家、关键词筛选 |
| 商品管理 | `GET /api/admin/products/{id}` | 商品详情 |
| 商品管理 | `PUT /api/admin/products/{id}/takedown` | 强制下架 |
| 举报管理 | `GET /api/admin/reports` | 举报列表 |
| 举报管理 | `PUT /api/admin/reports/{id}/handle` | 处理举报 |
| 订单管理 | `GET /api/admin/orders` | 订单列表 |
| 退款管理 | `GET /api/admin/orders/refunding` | 退款中订单 |
| 统计 | `GET /api/admin/stats` | 平台统计 |
| 公告 | `POST /api/admin/notifications` | 系统公告 |
| 反馈 | `GET /api/admin/feedback` | 用户反馈列表 |

用户状态和限制字段：

```java
// backend/src/main/java/com/tps/entity/User.java
public enum Role { USER, ADMIN }
public enum UserStatus { ACTIVE, BANNED, DEACTIVATED }

private Boolean muted = false;
private Boolean publishBanned = false;
```

用户管理：

```java
// backend/src/main/java/com/tps/service/AdminService.java
@Transactional
public void banUser(Long userId) {
    User user = getMutableUser(userId, "管理员账号不能被封禁");
    user.setStatus(User.UserStatus.BANNED);
    userRepository.save(user);
}

@Transactional
public void muteUser(Long userId) {
    User user = getMutableUser(userId, "管理员账号不能被禁言");
    user.setMuted(true);
    userRepository.save(user);
}

@Transactional
public void publishBanUser(Long userId) {
    User user = getMutableUser(userId, "管理员账号不能被禁止发布商品");
    user.setPublishBanned(true);
    userRepository.save(user);
}
```

讲解要点：

- 后台用户管理不是单一“封禁”，而是三类限制：
  - `status = BANNED`：账号不可用。
  - `muted = true`：不能发送聊天消息。
  - `publishBanned = true`：不能发布商品。
- `getMutableUser` 防止管理员账号被普通后台操作封禁、禁言或禁止发布。

商品后台查询：

```java
// backend/src/main/java/com/tps/service/AdminService.java
public Page<ProductResponse> getProducts(String status, String keyword,
                                         String category, Long sellerId,
                                         int page, int size) {
    Specification<Product> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        if (hasFilter(status)) {
            predicates.add(cb.equal(root.get("status"),
                    Product.ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
        }
        if (sellerId != null) {
            predicates.add(cb.equal(root.get("userId"), sellerId));
        }
        ...
    };
    return productRepository.findAll(spec, pageable(page, size, "createdAt"))
            .map(product -> productService.toResponse(product, null));
}
```

讲解要点：

- 后台商品列表可以看不同状态，不只看在售商品。
- 后台商品详情不增加浏览量：

```java
// backend/src/main/java/com/tps/service/AdminService.java
public ProductResponse getProductDetail(Long productId) {
    return productService.getDetailWithoutIncrement(productId, null);
}
```

举报处理：

```java
// backend/src/main/java/com/tps/service/AdminService.java
@Transactional
public void handleReport(Long reportId, boolean takedown, String reason, Long adminId) {
    Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("举报不存在"));
    if (takedown) {
        String takedownReason = reason == null || reason.isBlank()
                ? report.getReason()
                : reason.trim();
        takedownProduct(report.getProductId(), takedownReason, adminId);
        report.setStatus(Report.ReportStatus.HANDLED);
    } else {
        report.setStatus(Report.ReportStatus.REJECTED);
    }
    report.setHandledBy(adminId);
    report.setHandledAt(LocalDateTime.now());
    reportRepository.save(report);
}
```

讲解要点：

- 举报处理和商品下架复用 `takedownProduct`，保证下架原因、下架人、下架时间、用户通知一致。
- 举报状态有 `PENDING`、`HANDLED`、`REJECTED`。

后台统计：

```java
// backend/src/main/java/com/tps/service/AdminService.java
public Map<String, Object> getStats() {
    stats.put("totalUsers", userRepository.count());
    stats.put("activeUsers", userRepository.countByStatus(User.UserStatus.ACTIVE));
    stats.put("bannedUsers", userRepository.countByStatus(User.UserStatus.BANNED));
    stats.put("totalProducts", productRepository.count());
    stats.put("onSaleProducts", productRepository.countByStatus(Product.ProductStatus.ON_SALE));
    stats.put("totalOrders", orderRepository.count());
    stats.put("pendingReports", reportRepository.countByStatus(Report.ReportStatus.PENDING));
    return stats;
}
```

讲解要点：

- 后台首页统计覆盖用户、商品、订单、退款和举报待处理数量。
- 管理员可以通过统计快速定位平台风险，例如待处理举报和退款中订单。

## 7. 模块串联讲解建议

推荐讲解顺序：

1. 从总体架构开始：Controller 接口入口、Service 业务规则、Repository 数据访问、Entity 状态模型。
2. 讲商品生命周期：发布 `ON_SALE`、用户下架 `OFF`、订单完成 `SOLD`、管理员强制下架 `OFF`。
3. 讲搜索浏览：只展示在售商品，详情增加浏览量，登录用户记录浏览历史。
4. 讲沟通模块：商品维度会话、买卖双方权限、敏感词和禁言拦截、REST/WebSocket 复用同一服务。
5. 讲交易闭环：订单创建锁商品、支付、发货、确认、取消、退款。
6. 讲信用体系：完成订单后评价，平均评分换算信用分。
7. 讲后台管理：RBAC 权限、用户多维限制、商品下架、举报处理、订单退款和平台统计。

一句话总结：

```text
平台以商品为核心入口，通过订单完成交易闭环，通过消息支撑买卖沟通，通过评价形成信用分，通过后台管理处理违规、举报和平台运营。
```
