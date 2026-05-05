package com.tps.entity;

/**
 * 文件说明：JPA 实体定义，负责映射数据库表结构与领域对象。
 */

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
