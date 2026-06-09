package com.tps.controller;

/**
 * 文件说明：商品详情留言控制器。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.product.ProductCommentRequest;
import com.tps.dto.product.ProductCommentResponse;
import com.tps.security.JwtUtil;
import com.tps.service.ProductCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/comments")
@RequiredArgsConstructor
public class ProductCommentController {

    private final ProductCommentService productCommentService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ApiResponse<PageResponse<ProductCommentResponse>> list(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long viewerId = extractUserId(authorization);
        return ApiResponse.success(PageResponse.from(productCommentService.list(productId, viewerId, page, size)));
    }

    @PostMapping
    public ApiResponse<ProductCommentResponse> create(
            @PathVariable Long productId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProductCommentRequest request) {
        return ApiResponse.success(productCommentService.create(productId, userId, request));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<?> delete(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        productCommentService.delete(productId, commentId, userId);
        return ApiResponse.success();
    }

    private Long extractUserId(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                return jwtUtil.getUserId(authorization.substring(7));
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
