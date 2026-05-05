package com.tps.repository;

/**
 * 文件说明：数据访问层，负责声明实体查询与持久化接口。
 */

import com.tps.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :convId AND m.senderId != :userId")
    void markReadByConversationAndReceiver(@Param("convId") Long conversationId, @Param("userId") Long userId);
}
