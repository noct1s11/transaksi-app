package com.example.transaksi_app.repository;

import com.example.transaksi_app.model.Wallet;
import com.example.transaksi_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUser(User user);
}
