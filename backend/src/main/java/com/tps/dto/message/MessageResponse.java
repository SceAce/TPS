package com.tps.dto.message;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
