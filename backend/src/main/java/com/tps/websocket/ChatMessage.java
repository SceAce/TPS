package com.tps.websocket;

/**
 * 文件说明：WebSocket 消息入口与载荷定义，负责聊天实时通信链路。
 */

import lombok.Data;

@Data
public class ChatMessage {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String type; // TEXT, IMAGE
    private String createdAt;
}
