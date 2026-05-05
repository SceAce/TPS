package com.tps.dto.feedback;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String userNickname;
    private String type;
    private String content;
    private String contact;
    private String status;
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
