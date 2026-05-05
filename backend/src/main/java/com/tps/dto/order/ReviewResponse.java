package com.tps.dto.order;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private Long orderId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
