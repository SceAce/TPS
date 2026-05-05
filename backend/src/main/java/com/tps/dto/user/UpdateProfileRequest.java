package com.tps.dto.user;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 50)
    private String nickname;

    @Size(max = 200)
    private String bio;

    @Size(max = 100)
    private String location;

    @Size(max = 255)
    private String shippingAddress;

    private String avatarUrl;
}
