package com.example.transaksi_app.repository;

import com.example.transaksi_app.model.Category;
import com.example.transaksi_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    Category findByNameAndUser(String name, User user);
}
