package com.tps.dto.product;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import com.tps.entity.Product;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {
    @NotBlank
    @Size(max = 100)
    private String title;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    private String category;

    private Product.Condition condition;

    private String location;

    @Size(max = 9, message = "最多9张图片")
    private List<String> imageUrls;
}
