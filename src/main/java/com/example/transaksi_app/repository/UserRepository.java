package com.example.transaksi_app.repository; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.User; // PASTIKAN INI SESUAI
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findAllByRole(String role); // Untuk mendapatkan user berdasarkan role (misal: semua USER)
}
