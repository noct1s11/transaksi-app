package com.example.transaksi_app.controller; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.Transaction; // PASTIKAN INI SESUAI
import com.example.transaksi_app.model.User;       // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.TransactionRepository; // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.UserRepository;       // PASTIKAN INI SESUAI
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Untuk hash password user baru
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminController(UserRepository userRepository, TransactionRepository transactionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Helper method untuk mengambil user yang sedang login
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().equals("anonymousUser")) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    // Menampilkan Dashboard Admin (daftar users)
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpServletRequest request) {
        List<User> users = userRepository.findAll(); // Mendapatkan semua user
        model.addAttribute("users", users);
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        return "admin/dashboard"; // Mengarahkan ke admin/dashboard.html
    }

    // Menampilkan form untuk menambah user baru
    @GetMapping("/user/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User()); // Objek user kosong untuk form
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        return "admin/add-user"; // Mengarahkan ke admin/add-user.html
    }

    // Menangani penambahan user baru oleh Admin
    @PostMapping("/user/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String role,
                          RedirectAttributes redirectAttributes) {
        // Cek apakah username sudah ada
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Username '" + username + "' sudah digunakan. Pilih username lain.");
            return "redirect:/admin/user/add";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        user.setRole(role); // Role bisa ditentukan oleh admin
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "User " + username + " berhasil ditambahkan.");
        return "redirect:/admin/dashboard";
    }

    // Menangani penghapusan user oleh Admin
    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "User berhasil dihapus.");
        return "redirect:/admin/dashboard";
    }

    // Menampilkan semua transaksi untuk Admin
    @GetMapping("/transactions")
    public String adminTransactions(Model model, HttpServletRequest request) {
        List<Transaction> transactions = transactionRepository.findAll(); // Mendapatkan semua transaksi
        model.addAttribute("transactions", transactions);
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        return "admin/transactions"; // Mengarahkan ke admin/transactions.html
    }

    // Menampilkan form untuk menambah transaksi baru (oleh Admin)
    @GetMapping("/transaction/add")
    public String showAddTransactionForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("users", userRepository.findAll()); // Kirim daftar user agar admin bisa memilih
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        return "admin/add-transaction"; // Mengarahkan ke admin/add-transaction.html
    }

    // Menangani penambahan transaksi oleh Admin
    @PostMapping("/transaction")
    public String addTransaction(@RequestParam String description,
                                 @RequestParam double amount,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String category,
                                 @RequestParam Long userId, // Admin bisa memilih user untuk transaksi ini
                                 RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
            return "redirect:/admin/transaction/add";
        }
        User user = userOptional.get();

        Transaction transaction = new Transaction(description, amount, type, category, user);
        transactionRepository.save(transaction);
        redirectAttributes.addFlashAttribute("message", "Transaksi berhasil ditambahkan.");
        return "redirect:/admin/transactions";
    }

    // Menampilkan form untuk mengedit transaksi
    @GetMapping("/transaction/{id}/edit")
    public String showEditTransactionForm(@PathVariable Long id, Model model) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid transaction Id:" + id));
        model.addAttribute("transaction", transaction);
        model.addAttribute("users", userRepository.findAll()); // Kirim daftar user agar bisa dipilih
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        return "admin/edit-transaction"; // Mengarahkan ke admin/edit-transaction.html
    }

    // Menangani update transaksi oleh Admin
    @PostMapping("/transaction/{id}") // Menggunakan method POST untuk update
    public String updateTransaction(@PathVariable Long id,
                                    @RequestParam String description,
                                    @RequestParam double amount,
                                    @RequestParam String type,
                                    @RequestParam(required = false) String category,
                                    @RequestParam Long userId,
                                    RedirectAttributes redirectAttributes) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid transaction Id:" + id));

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User tidak ditemukan.");
            return "redirect:/admin/transaction/" + id + "/edit";
        }
        User user = userOptional.get();

        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setUser(user); // Update user jika diubah
        transactionRepository.save(transaction);
        redirectAttributes.addFlashAttribute("message", "Transaksi berhasil diperbarui.");
        return "redirect:/admin/transactions";
    }

    // Menangani penghapusan transaksi oleh Admin
    @PostMapping("/transaction/{id}/delete")
    public String deleteTransaction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Transaksi berhasil dihapus.");
        return "redirect:/admin/transactions";
    }

    // Menampilkan analitik transaksi untuk Admin
    @GetMapping("/analytics")
    public String adminAnalytics(Model model, HttpServletRequest request) {
        List<User> users = userRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();
        // Data agregat per user
        Map<String, Double> totalPerUser = new LinkedHashMap<>();
        for (User user : users) {
            double total = transactions.stream().filter(t -> t.getUser().getId().equals(user.getId())).mapToDouble(Transaction::getAmount).sum();
            totalPerUser.put(user.getUsername(), total);
        }
        model.addAttribute("totalPerUser", totalPerUser);
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        return "admin/analytics";
    }
    }

