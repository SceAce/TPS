package com.tps.repository;

/**
 * 文件说明：数据访问层，负责声明实体查询与持久化接口。
 */

import com.tps.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Feedback> findByStatusOrderByCreatedAtDesc(Feedback.FeedbackStatus status, Pageable pageable);
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
