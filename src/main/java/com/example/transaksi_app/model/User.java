package com.example.transaksi_app.model; // PASTIKAN INI SESUAI

import jakarta.persistence.*;
import java.util.Set; // Untuk relasi OneToMany dengan Transaction

@Entity
@Table(name = "users") // Gunakan nama tabel 'users' untuk menghindari konflik keyword SQL
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // Contoh: "ADMIN", "USER"

    @Column(name = "profile_picture")
    private String profilePicture; // Path atau URL foto profil

    @Column(nullable = false)
    private boolean active = true; // Status user aktif/nonaktif

    @Column(nullable = false, unique = true)
    private String email; // Email user untuk validasi dan notifikasi

    // Relasi One-to-Many dengan Transaction
    // mappedBy menunjukkan field "user" di entity Transaction
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Transaction> transactions;

    // Relasi One-to-Many dengan Category
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Category> categories;

    // Relasi One-to-Many dengan Wallet
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Wallet> wallets;

    // Relasi One-to-Many dengan ActivityLog
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ActivityLog> activityLogs;

    // --- Constructors ---
    public User() {
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Transaction> transactions) {
        this.transactions = transactions;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public Set<Wallet> getWallets() {
        return wallets;
    }

    public void setWallets(Set<Wallet> wallets) {
        this.wallets = wallets;
    }

    public Set<ActivityLog> getActivityLogs() {
        return activityLogs;
    }

    public void setActivityLogs(Set<ActivityLog> activityLogs) {
        this.activityLogs = activityLogs;
    }
}
