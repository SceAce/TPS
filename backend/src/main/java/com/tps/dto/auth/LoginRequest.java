package com.tps.dto.auth;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    private String phone;

    @NotBlank
    private String password;
}
