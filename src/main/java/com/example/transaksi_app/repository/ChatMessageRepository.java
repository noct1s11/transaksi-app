package com.example.transaksi_app.repository;

import com.example.transaksi_app.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderAndReceiverOrderByTimestampAsc(String sender, String receiver);
    List<ChatMessage> findByReceiverAndIsReadIsFalse(String receiver);
    List<ChatMessage> findBySenderOrReceiverOrderByTimestampAsc(String sender, String receiver);

    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatBetweenUsers(@Param("user1") String user1, @Param("user2") String user2);
}
