package com.tps.entity;

/**
 * 文件说明：JPA 实体定义，负责映射数据库表结构与领域对象。
 */

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 500)
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
