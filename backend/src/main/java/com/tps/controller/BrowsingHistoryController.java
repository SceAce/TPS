package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.security.JwtUtil;
import com.tps.service.BrowsingHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history/products")
@RequiredArgsConstructor
public class BrowsingHistoryController {

    private final BrowsingHistoryService historyService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{productId}")
    public ApiResponse<?> record(@PathVariable Long productId, HttpServletRequest request) {
        historyService.record(getUserId(request), productId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<?> list(HttpServletRequest request,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(historyService.list(getUserId(request), page, size)));
    }

    @DeleteMapping
    public ApiResponse<?> clear(HttpServletRequest request) {
        historyService.clear(getUserId(request));
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<?> delete(@PathVariable Long productId, HttpServletRequest request) {
        historyService.delete(getUserId(request), productId);
        return ApiResponse.success();
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.getUserId(token);
    }
}
