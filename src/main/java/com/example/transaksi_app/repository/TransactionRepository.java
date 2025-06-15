package com.example.transaksi_app.repository; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.Transaction; // PASTIKAN INI SESUAI
import com.example.transaksi_app.model.User;       // PASTIKAN INI SESUAI
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
}
