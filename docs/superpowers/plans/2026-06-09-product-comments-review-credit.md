# Product Comments And Review Credit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real product comment system on product detail pages while preserving the existing completed-order review and credit-score flow.

**Architecture:** Keep two concepts separate. `Review` remains the transaction review entity tied to completed orders and credit scoring. New `ProductComment` records are lightweight product-detail comments that do not affect credit score.

**Tech Stack:** Spring Boot 3.2 + JPA + MySQL/H2 tests, Android Kotlin + Jetpack Compose + Retrofit.

---

## File Structure

Backend product comments:
- Create: `backend/src/main/java/com/tps/entity/ProductComment.java`
- Create: `backend/src/main/java/com/tps/dto/product/ProductCommentRequest.java`
- Create: `backend/src/main/java/com/tps/dto/product/ProductCommentResponse.java`
- Create: `backend/src/main/java/com/tps/repository/ProductCommentRepository.java`
- Create: `backend/src/main/java/com/tps/service/ProductCommentService.java`
- Create: `backend/src/main/java/com/tps/controller/ProductCommentController.java`
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/test/java/com/tps/BackendIntegrationTest.java`

Android data layer:
- Modify: `app/src/main/java/com/tps/data/remote/dto/Dtos.kt`
- Modify: `app/src/main/java/com/tps/data/remote/api/ApiService.kt`

Android product detail UI:
- Modify: `app/src/main/java/com/tps/ui/product/ProductDetailViewModel.kt`
- Modify: `app/src/main/java/com/tps/ui/product/ProductDetailScreen.kt`

Verification:
- Run: `mvn test` from `backend/`
- Run: `./gradlew :app:compileDebugKotlin`
- Run: `./build-android-emulator.sh`

## Task 1: Backend Product Comment API

**Files:**
- Create: `backend/src/main/java/com/tps/entity/ProductComment.java`
- Create: `backend/src/main/java/com/tps/dto/product/ProductCommentRequest.java`
- Create: `backend/src/main/java/com/tps/dto/product/ProductCommentResponse.java`
- Create: `backend/src/main/java/com/tps/repository/ProductCommentRepository.java`
- Create: `backend/src/main/java/com/tps/service/ProductCommentService.java`
- Create: `backend/src/main/java/com/tps/controller/ProductCommentController.java`
- Modify: `backend/src/main/resources/sql/init.sql`

- [ ] **Step 1: Add `product_comments` table**

Add this table after `product_images` in `backend/src/main/resources/sql/init.sql`:

```sql
-- 商品评论表
CREATE TABLE IF NOT EXISTS product_comments (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id  BIGINT NOT NULL COMMENT '商品ID',
  user_id     BIGINT NOT NULL COMMENT '评论用户ID',
  content     VARCHAR(500) NOT NULL COMMENT '评论内容',
  status      ENUM('VISIBLE','DELETED') DEFAULT 'VISIBLE' COMMENT '评论状态',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_comment_product_created (product_id, created_at),
  INDEX idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Add JPA entity**

Create `ProductComment.java`:

```java
package com.tps.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_comments")
public class ProductComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status = CommentStatus.VISIBLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CommentStatus { VISIBLE, DELETED }
}
```

- [ ] **Step 3: Add request/response DTOs**

Create `ProductCommentRequest.java`:

```java
package com.tps.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductCommentRequest {
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;
}
```

Create `ProductCommentResponse.java`:

```java
package com.tps.dto.product;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductCommentResponse {
    private Long id;
    private Long productId;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private String status;
    private Boolean mine;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: Add repository**

Create `ProductCommentRepository.java`:

```java
package com.tps.repository;

import com.tps.entity.ProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCommentRepository extends JpaRepository<ProductComment, Long> {
    Page<ProductComment> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId,
            ProductComment.CommentStatus status,
            Pageable pageable);
}
```

- [ ] **Step 5: Add service**

Create `ProductCommentService.java` with:
- `list(productId, viewerId, page, size)`
- `create(productId, userId, request)`
- `delete(productId, commentId, userId)`
- product existence check via `ProductRepository`
- delete permission: comment author or product owner
- trim content and reject blank content

- [ ] **Step 6: Add controller**

Create `ProductCommentController.java`:
- `GET /api/products/{productId}/comments`
- `POST /api/products/{productId}/comments`
- `DELETE /api/products/{productId}/comments/{commentId}`
- Use `@AuthenticationPrincipal Long userId` for write/delete.
- For GET, accept optional `Authorization` header and parse viewer id with `JwtUtil`, matching `ProductController.extractUserId`.

- [ ] **Step 7: Run backend compile/test**

Run:

```bash
cd backend
mvn test
```

Expected: existing backend tests pass. If unrelated existing tests fail, report exact failing test and error.

## Task 2: Android Comment DTO And API

**Files:**
- Modify: `app/src/main/java/com/tps/data/remote/dto/Dtos.kt`
- Modify: `app/src/main/java/com/tps/data/remote/api/ApiService.kt`

- [ ] **Step 1: Add DTOs**

Add to the product section of `Dtos.kt`:

```kotlin
data class ProductCommentDto(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val userNickname: String?,
    val userAvatar: String?,
    val content: String,
    val status: String,
    val mine: Boolean = false,
    val createdAt: String?
)

data class ProductCommentRequest(
    val content: String
)
```

- [ ] **Step 2: Add Retrofit endpoints**

Add to the product section of `ApiService.kt`:

```kotlin
@GET("api/products/{id}/comments")
suspend fun getProductComments(
    @Path("id") id: Long,
    @Query("page") page: Int = 0,
    @Query("size") size: Int = 20
): ApiResponse<PageResponse<ProductCommentDto>>

@POST("api/products/{id}/comments")
suspend fun createProductComment(
    @Path("id") id: Long,
    @Body req: ProductCommentRequest
): ApiResponse<ProductCommentDto>

@DELETE("api/products/{productId}/comments/{commentId}")
suspend fun deleteProductComment(
    @Path("productId") productId: Long,
    @Path("commentId") commentId: Long
): ApiResponse<Unit>
```

- [ ] **Step 3: Run Android compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: Kotlin compilation succeeds.

## Task 3: Product Detail Comment State And UI

**Files:**
- Modify: `app/src/main/java/com/tps/ui/product/ProductDetailViewModel.kt`
- Modify: `app/src/main/java/com/tps/ui/product/ProductDetailScreen.kt`

- [ ] **Step 1: Extend `ProductDetailUiState`**

Add:
- `comments: List<ProductCommentDto> = emptyList()`
- `commentsLoading: Boolean = false`
- `commentSubmitting: Boolean = false`

- [ ] **Step 2: Load comments with product detail**

After `apiService.getProduct(productId)` succeeds, call `apiService.getProductComments(product.id, page = 0, size = 20)` and store the response content. If comments fail, keep product visible and set `error` to a concise message only when needed.

- [ ] **Step 3: Add comment actions**

Add ViewModel functions:
- `submitComment(content: String)`
- `deleteComment(commentId: Long)`
- `refreshComments()`

Rules:
- Blank comment shows `error = "请输入评论内容"`.
- Limit content to 500 chars before sending.
- On submit success, prepend or reload comments and clear submitting.
- On delete success, remove item locally.

- [ ] **Step 4: Add comments UI card**

In `ProductDetailScreen.kt`, after the seller card, add a `ProductCommentsCard` composable:
- Header: `商品评论`
- Secondary text: `向卖家提问，或查看其他同学的留言`
- Show first 20 comments.
- Empty state: `还没有评论，先问问商品细节。`
- Each row shows avatar circle, nickname, content, time, delete icon if `mine == true`.
- Input opens a bottom sheet or dialog with `OutlinedTextField`, submit button, `imePadding()`, and `navigationBarsPadding()`.

- [ ] **Step 5: Keep keyboard behavior correct**

The comment input must not add fake keyboard spacers. Use `ModalBottomSheet` or `AlertDialog` content with:

```kotlin
Modifier
    .fillMaxWidth()
    .imePadding()
    .navigationBarsPadding()
```

- [ ] **Step 6: Run Android compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: Kotlin compilation succeeds.

## Task 4: Integration Verification

**Files:**
- Modify: `backend/src/test/java/com/tps/BackendIntegrationTest.java` only if Task 1 did not add product comment coverage.

- [ ] **Step 1: Verify backend API tests**

Run:

```bash
cd backend
mvn test
```

Expected: test suite passes.

- [ ] **Step 2: Verify Android Kotlin compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: app Kotlin compile passes.

- [ ] **Step 3: Build emulator APK**

Run:

```bash
./build-android-emulator.sh
```

Expected:
- API base is `http://10.0.2.2:8080/`
- APK generated at `app/build/outputs/apk/debug/app-debug.apk`
- If emulator is attached, install succeeds.

## Subagent Execution Notes

Use one fresh subagent per task. Do not let Task 1 edit Android files. Do not let Task 2 edit ViewModels or UI. Do not let Task 3 edit backend files. The controller agent reviews each task before dispatching the next task.
