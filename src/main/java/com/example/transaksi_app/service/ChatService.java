package com.example.transaksi_app.service;

import com.example.transaksi_app.model.ChatMessage;
import com.example.transaksi_app.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public ChatMessage sendMessage(String sender, String receiver, String content) {
        ChatMessage message = new ChatMessage(sender, receiver, content, LocalDateTime.now(), false);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChat(String user1, String user2) {
        return chatMessageRepository.findChatBetweenUsers(user1, user2);
    }

    public List<ChatMessage> getAllChatsForUser(String username) {
        return chatMessageRepository.findBySenderOrReceiverOrderByTimestampAsc(username, username);
    }

    public void markAsRead(Long messageId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setRead(true);
            chatMessageRepository.save(msg);
        });
    }
}
