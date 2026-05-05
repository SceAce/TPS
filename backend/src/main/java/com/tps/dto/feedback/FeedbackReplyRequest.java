package com.tps.dto.feedback;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackReplyRequest {
    @NotBlank(message = "回复不能为空")
    @Size(max = 1000, message = "回复最多1000字")
    private String reply;
}
