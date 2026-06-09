package com.tps.dto.product;

/**
 * 文件说明：商品详情留言响应 DTO。
 */

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
