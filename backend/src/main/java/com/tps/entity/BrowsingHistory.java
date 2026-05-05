package com.tps.entity;

/**
 * 文件说明：JPA 实体定义，负责映射数据库表结构与领域对象。
 */

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "browsing_history",
        uniqueConstraints = @UniqueConstraint(name = "uk_history_user_product", columnNames = {"user_id", "product_id"})
)
public class BrowsingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();
}
