package com.example.transaksi_app.controller; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.Transaction; // PASTIKAN INI SESUAI
import com.example.transaksi_app.model.User;       // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.TransactionRepository; // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.UserRepository;       // PASTIKAN INI SESUAI
import com.example.transaksi_app.security.UserPrincipal;         // PASTIKAN INI SESUAI
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, TransactionRepository transactionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Menangani registrasi user baru
    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, Model model) {
        // Cek apakah username sudah ada
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username '" + username + "' sudah digunakan. Pilih username lain.");
            return "register"; // Kembali ke halaman register dengan pesan error
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        user.setRole("USER"); // Default role untuk registrasi adalah USER
        userRepository.save(user);
        return "redirect:/login?registered"; // Redirect ke login dengan pesan sukses (opsional)
    }

    // Menampilkan dashboard user
    @GetMapping("/user/dashboard")
    public String userDashboard(Authentication authentication, Model model) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser(); // Ambil objek User asli dari UserPrincipal

        List<Transaction> transactions = transactionRepository.findByUser(user);
        model.addAttribute("transactions", transactions);
        // Tambahkan atribut user untuk menampilkan nama user di dashboard
        model.addAttribute("currentUser", user);
        return "user/dashboard";
    }

    // Menangani penambahan transaksi oleh user
    @PostMapping("/user/transaction")
    public String addTransaction(@RequestParam String description,
                                 @RequestParam double amount,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String category, // Kategori opsional
                                 Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        Transaction transaction = new Transaction(description, amount, type, category, user);
        transactionRepository.save(transaction);
        return "redirect:/user/dashboard";
    }

    // Endpoint untuk menampilkan daftar transaksi user
    @GetMapping("/user/transactions")
    public String userTransactions(Authentication authentication, Model model) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentUser", user);
        return "user/transactions";
    }
}
