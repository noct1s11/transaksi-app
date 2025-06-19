package com.example.transaksi_app.service;

import com.example.transaksi_app.model.User;
import com.example.transaksi_app.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReminderService {
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public ReminderService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    // Kirim reminder setiap tanggal 1 jam 08:00
    @Scheduled(cron = "0 0 8 1 * *")
    public void sendMonthlyReminders() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getUsername() == null || user.getUsername().isEmpty()) continue;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getUsername());
            message.setSubject("Reminder Bulanan Catat Transaksi");
            message.setText("Jangan lupa mencatat transaksi keuangan Anda bulan ini di aplikasi Transaksi!");
            mailSender.send(message);
        }
    }
}
