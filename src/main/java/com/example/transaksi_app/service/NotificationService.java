package com.example.transaksi_app.service;

import com.example.transaksi_app.model.Transaction;
import com.example.transaksi_app.model.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {
    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendLargeTransactionNotification(User user, Transaction transaction, double threshold) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) return;
        if (transaction.getAmount() < threshold) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Notifikasi Transaksi Besar");
        message.setText("Transaksi dengan jumlah besar terdeteksi: " + transaction.getDescription() + " sejumlah " + transaction.getAmount());
        try {
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Gagal mengirim email notifikasi: {}", e.getMessage());
        }
    }
}
