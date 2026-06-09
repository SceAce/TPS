package com.tps.repository;

/**
 * 文件说明：商品详情留言数据访问接口。
 */

import com.tps.entity.ProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCommentRepository extends JpaRepository<ProductComment, Long> {
    Page<ProductComment> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId,
            ProductComment.CommentStatus status,
            Pageable pageable);
}
