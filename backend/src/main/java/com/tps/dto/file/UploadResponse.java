package com.tps.dto.file;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResponse {
    private String url;
    private String path;
}
