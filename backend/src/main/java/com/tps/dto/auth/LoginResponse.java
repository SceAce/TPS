package com.tps.dto.auth;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String role;
}
