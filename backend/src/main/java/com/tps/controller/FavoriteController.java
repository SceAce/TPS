package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.product.ProductResponse;
import com.tps.security.JwtUtil;
import com.tps.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{productId}")
    public ApiResponse<Boolean> add(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserId(request);
        favoriteService.add(userId, productId);
        return ApiResponse.success(true);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Boolean> remove(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserId(request);
        favoriteService.remove(userId, productId);
        return ApiResponse.success(false);
    }

    @PostMapping("/{productId}/toggle")
    public ApiResponse<Boolean> toggle(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(favoriteService.toggle(userId, productId));
    }

    @GetMapping("/{productId}/status")
    public ApiResponse<Boolean> status(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(favoriteService.isFavorited(userId, productId));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> myFavorites(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(favoriteService.myFavorites(userId));
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.getUserId(token);
    }
}
