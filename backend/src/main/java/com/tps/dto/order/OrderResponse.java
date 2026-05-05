package com.tps.dto.order;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private String productCover;
    private Long buyerId;
    private String buyerNickname;
    private Long sellerId;
    private String sellerNickname;
    private BigDecimal price;
    private String status;
    private String remark;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
