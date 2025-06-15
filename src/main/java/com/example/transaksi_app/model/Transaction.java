package com.example.transaksi_app.model; // PASTIKAN INI SESUAI

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description; // Deskripsi/Keterangan

    @Column(nullable = false)
    private double amount; // Jumlah

    @Column(nullable = false)
    private LocalDate date; // Tanggal

    @Column(nullable = false)
    private String type; // Tipe: "Pemasukan", "Pengeluaran"

    @Column(nullable = true) // Kategori bisa null jika tidak ada
    private String category;

    @ManyToOne(fetch = FetchType.LAZY) // Relasi Many-to-One dengan User
    @JoinColumn(name = "user_id", nullable = false) // Kolom foreign key
    private User user;

    // --- Constructors ---
    public Transaction() {
        this.date = LocalDate.now(); // Set tanggal default saat dibuat
    }

    public Transaction(String description, double amount, String type, String category, User user) {
        this.description = description;
        this.amount = amount;
        this.date = LocalDate.now();
        this.type = type;
        this.category = category;
        this.user = user;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
