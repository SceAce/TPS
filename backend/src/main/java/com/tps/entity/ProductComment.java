package com.tps.entity;

/**
 * 文件说明：JPA 实体定义，负责映射商品详情留言数据。
 */

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_comments")
public class ProductComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "image_urls", length = 1000)
    private String imageUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status = CommentStatus.VISIBLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CommentStatus { VISIBLE, DELETED }
}
