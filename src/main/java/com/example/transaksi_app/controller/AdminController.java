package com.example.transaksi_app.controller; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.Transaction; // PASTIKAN INI SESUAI
import com.example.transaksi_app.model.User;       // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.TransactionRepository; // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.UserRepository;       // PASTIKAN INI SESUAI
import com.example.transaksi_app.model.Category;
import com.example.transaksi_app.model.Wallet;
import com.example.transaksi_app.repository.CategoryRepository;
import com.example.transaksi_app.repository.WalletRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder; // Untuk hash password user baru
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import jakarta.servlet.http.HttpServletResponse;
import com.example.transaksi_app.model.ActivityLog;
import com.example.transaksi_app.repository.ActivityLogRepository;

import java.io.IOException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final ActivityLogRepository activityLogRepository;

    public AdminController(UserRepository userRepository, TransactionRepository transactionRepository, PasswordEncoder passwordEncoder, CategoryRepository categoryRepository, WalletRepository walletRepository, ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // Tambahkan constant untuk string yang sering diulang
    private static final String ATTR_USERS = "users";
    private static final String ATTR_CURRENT_USER = "currentUser";
    private static final String ATTR_CURRENT_URI = "currentUri";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_CATEGORIES = "categories";
    private static final String ATTR_WALLETS = "wallets";
    private static final String REDIRECT_ADMIN_DASHBOARD = "redirect:/admin/dashboard";
    private static final String REDIRECT_ADMIN_USER_ADD = "redirect:/admin/user/add";
    private static final String REDIRECT_ADMIN_TRANSACTIONS = "redirect:/admin/transactions";
    private static final String REDIRECT_ADMIN_TRANSACTION_ADD = "redirect:/admin/transaction/add";
    private static final String USER_NOT_FOUND = "User tidak ditemukan.";
    // Jadikan string literal yang sering dipakai sebagai constant
    private static final String USER_PREFIX = "User: ";
    // Ganti password default dengan instruksi aman
    private static final String DEFAULT_PASSWORD = "GantiSaya123!";

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
        model.addAttribute(ATTR_USERS, users);
        User currentUser = getCurrentUser();
        model.addAttribute(ATTR_CURRENT_USER, currentUser);
        model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());
        return "admin/dashboard"; // Mengarahkan ke admin/dashboard.html
    }

    // Menampilkan form untuk menambah user baru
    @GetMapping("/user/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User()); // Objek user kosong untuk form
        User currentUser = getCurrentUser();
        model.addAttribute(ATTR_CURRENT_USER, currentUser);
        return "admin/add-user"; // Mengarahkan ke admin/add-user.html
    }

    // Menangani penambahan user baru oleh Admin
    @PostMapping("/user/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String role,
                          @RequestParam String email,
                          RedirectAttributes redirectAttributes) {
        // Validasi username
        if (username == null || username.length() < 4 || username.length() > 32 || !username.matches("\\w+")) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Username harus 4-32 karakter dan hanya huruf, angka, atau underscore.");
            return REDIRECT_ADMIN_USER_ADD;
        }
        // Validasi password
        if (password == null || password.length() < 6 || password.length() > 64) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Password minimal 6 karakter dan maksimal 64 karakter.");
            return REDIRECT_ADMIN_USER_ADD;
        }
        // Validasi email (minimal tidak kosong)
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Email wajib diisi (boleh dummy).");
            return REDIRECT_ADMIN_USER_ADD;
        }
        // Cek apakah username sudah ada
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Username '" + username + "' sudah digunakan. Pilih username lain.");
            return REDIRECT_ADMIN_USER_ADD;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        user.setRole(role); // Role bisa ditentukan oleh admin
        user.setEmail(email); // Set email (boleh dummy)
        userRepository.save(user);
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "User " + username + " berhasil ditambahkan.");
        return REDIRECT_ADMIN_DASHBOARD;
    }

    // Menangani penghapusan user oleh Admin
    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "User berhasil dihapus.");
        return REDIRECT_ADMIN_DASHBOARD;
    }

    // Toggle user active/nonactive status
    @PostMapping("/user/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(!user.isActive());
            userRepository.save(user);
            // Log activity
            User admin = getCurrentUser();
            String action = user.isActive() ? "Aktifkan User" : "Nonaktifkan User";
            String details = USER_PREFIX + user.getUsername();
            activityLogRepository.save(new ActivityLog(admin, action, details));
            redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Status user '" + user.getUsername() + "' diubah menjadi " + (user.isActive() ? "Aktif" : "Nonaktif") + ".");
        } else {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, USER_NOT_FOUND);
        }
        return REDIRECT_ADMIN_DASHBOARD;
    }

    // Reset user password to default (e.g., 'password123')
    @PostMapping("/user/{id}/reset-password")
    public String resetUserPassword(@PathVariable Long id, @RequestParam("newPassword") String newPassword, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String passwordToSet = (newPassword != null && !newPassword.trim().isEmpty()) ? newPassword : DEFAULT_PASSWORD;
            user.setPassword(passwordEncoder.encode(passwordToSet));
            userRepository.save(user);
            // Log activity
            User admin = getCurrentUser();
            String action = "Reset Password";
            String details = USER_PREFIX + user.getUsername();
            activityLogRepository.save(new ActivityLog(admin, action, details));
            redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Password user '" + user.getUsername() + "' telah diubah.");
        } else {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, USER_NOT_FOUND);
        }
        return REDIRECT_ADMIN_DASHBOARD;
    }

    // Form ganti password user oleh admin
    @PostMapping("/user/{id}/change-password")
    public String changeUserPassword(@PathVariable Long id, @RequestParam String newPassword, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            // Log activity
            User admin = getCurrentUser();
            String action = "Ganti Password";
            String details = USER_PREFIX + user.getUsername();
            activityLogRepository.save(new ActivityLog(admin, action, details));
            redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Password user '" + user.getUsername() + "' berhasil diganti.");
        } else {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, USER_NOT_FOUND);
        }
        return REDIRECT_ADMIN_DASHBOARD;
    }

    // Menampilkan semua transaksi untuk Admin
    @GetMapping("/transactions")
    public String adminTransactions(Model model, HttpServletRequest request) {
        List<Transaction> transactions = transactionRepository.findAll(); // Mendapatkan semua transaksi
        model.addAttribute("transactions", transactions);
        User currentUser = getCurrentUser();
        model.addAttribute(ATTR_CURRENT_USER, currentUser);
        model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());
        return "admin/transactions"; // Mengarahkan ke admin/transactions.html
    }

    // Menampilkan form untuk menambah transaksi baru (oleh Admin)
    @GetMapping("/transaction/add")
    public String showAddTransactionForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        List<User> users = userRepository.findAll();
        model.addAttribute(ATTR_USERS, users);
        // Default: ambil kategori & wallet user pertama jika ada
        if (!users.isEmpty()) {
            User firstUser = users.get(0);
            model.addAttribute(ATTR_CATEGORIES, categoryRepository.findByUser(firstUser));
            model.addAttribute(ATTR_WALLETS, walletRepository.findByUser(firstUser));
        } else {
            model.addAttribute(ATTR_CATEGORIES, List.of());
            model.addAttribute(ATTR_WALLETS, List.of());
        }
        User currentUser = getCurrentUser();
        model.addAttribute(ATTR_CURRENT_USER, currentUser);
        return "admin/add-transaction"; // Mengarahkan ke admin/add-transaction.html
    }

    // Menangani penambahan transaksi oleh Admin
    @PostMapping("/transaction")
    public String addTransaction(@RequestParam String description,
                                 @RequestParam double amount,
                                 @RequestParam String type,
                                 @RequestParam Long categoryId,
                                 @RequestParam Long walletId,
                                 @RequestParam Long userId,
                                 RedirectAttributes redirectAttributes) {
        if (description == null || description.trim().length() < 3 || description.trim().length() > 100) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Deskripsi minimal 3 karakter dan maksimal 100 karakter.");
            return REDIRECT_ADMIN_TRANSACTION_ADD;
        }
        if (amount <= 0) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Jumlah harus lebih dari 0.");
            return REDIRECT_ADMIN_TRANSACTION_ADD;
        }
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, USER_NOT_FOUND);
            return REDIRECT_ADMIN_TRANSACTION_ADD;
        }
        User user = userOptional.get();
        Category category = categoryRepository.findById(categoryId).orElse(null);
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCategoryEntity(category);
        if (category != null) transaction.setCategory(category.getName());
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transactionRepository.save(transaction);
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Transaksi berhasil ditambahkan.");
        return REDIRECT_ADMIN_TRANSACTIONS;
    }

    // Menampilkan form untuk mengedit transaksi
    @GetMapping("/transaction/{id}/edit")
    public String showEditTransactionForm(@PathVariable Long id, Model model) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid transaction Id:" + id));
        model.addAttribute("transaction", transaction);
        List<User> users = userRepository.findAll();
        model.addAttribute(ATTR_USERS, users);
        // Ambil kategori & wallet milik user transaksi ini
        if (transaction.getUser() != null) {
            model.addAttribute(ATTR_CATEGORIES, categoryRepository.findByUser(transaction.getUser()));
            model.addAttribute(ATTR_WALLETS, walletRepository.findByUser(transaction.getUser()));
        } else {
            model.addAttribute(ATTR_CATEGORIES, List.of());
            model.addAttribute(ATTR_WALLETS, List.of());
        }
        User currentUser = getCurrentUser();
        model.addAttribute(ATTR_CURRENT_USER, currentUser);
        return "admin/edit-transaction"; // Mengarahkan ke admin/edit-transaction.html
    }

    // Menangani update transaksi oleh Admin
    @PostMapping("/transaction/{id}") // Menggunakan method POST untuk update
    public String updateTransaction(@PathVariable Long id,
                                    @RequestParam String description,
                                    @RequestParam String amount, // Ubah ke String
                                    @RequestParam String type,
                                    @RequestParam String category, // Nama kategori dari form
                                    @RequestParam Long userId,
                                    RedirectAttributes redirectAttributes) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid transaction Id:" + id));
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, USER_NOT_FOUND);
            return "redirect:/admin/transaction/" + id + "/edit";
        }
        User user = userOptional.get();
        // Parsing amount: hapus titik ribuan
        double amountValue = 0;
        try {
            amountValue = Double.parseDouble(amount.replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Format jumlah tidak valid.");
            return "redirect:/admin/transaction/" + id + "/edit";
        }
        // Cari kategori berdasarkan nama dan user
        Category categoryEntity = categoryRepository.findByNameAndUser(category, user);
        transaction.setDescription(description);
        transaction.setAmount(amountValue);
        transaction.setType(type);
        transaction.setUser(user);
        transaction.setCategoryEntity(categoryEntity);
        if (categoryEntity != null) transaction.setCategory(categoryEntity.getName());
        else transaction.setCategory(category); // fallback jika kategori tidak ditemukan
        // Tidak perlu setWallet karena field dompet sudah dihapus dari form
        transactionRepository.save(transaction);
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Transaksi berhasil diupdate.");
        return REDIRECT_ADMIN_TRANSACTIONS;
    }

    // Menangani penghapusan transaksi oleh Admin
    @PostMapping("/transaction/{id}/delete")
    public String deleteTransaction(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        transactionRepository.deleteById(id);
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Transaksi berhasil dihapus.");
        return REDIRECT_ADMIN_TRANSACTIONS;
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
        model.addAttribute(ATTR_CURRENT_USER, currentUser);
        model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());
        return "admin/analytics";
    }

    // Endpoint untuk generate PDF semua transaksi (admin) dengan OpenPDF
    @GetMapping("/transactions/cetak-rapor")
    public void cetakRaporSemuaTransaksi(HttpServletResponse response) throws IOException {
        List<Transaction> transactions = transactionRepository.findAll();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=rapor-semua-transaksi.pdf");
        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLUE);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            Paragraph title = new Paragraph("REKAP SEMUA TRANSAKSI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Tanggal Cetak: " + java.time.LocalDate.now(), cellFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.2f, 3f, 2f, 2.2f, 2f, 2.2f});

            String[] headers = {"No", "User", "Deskripsi", "Jumlah", "Tanggal", "Tipe", "Kategori"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(Color.BLUE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6f);
                table.addCell(cell);
            }
            int no = 1;
            for (Transaction t : transactions) {
                table.addCell(new Phrase(String.valueOf(no++), cellFont));
                table.addCell(new Phrase(t.getUser().getUsername(), cellFont));
                table.addCell(new Phrase(t.getDescription(), cellFont));
                table.addCell(new Phrase(String.format("%,.2f Rp", t.getAmount()), cellFont));
                table.addCell(new Phrase(t.getDate().toString(), cellFont));
                table.addCell(new Phrase(t.getType(), cellFont));
                table.addCell(new Phrase(t.getCategory() != null ? t.getCategory() : "-", cellFont));
            }
            document.add(table);
        }
    }

    // Menampilkan Activity Log
    @GetMapping("/activity-log")
    public String viewActivityLog(Model model) {
        List<ActivityLog> logs = activityLogRepository.findAll();
        model.addAttribute("logs", logs);
        return "admin/activity-log";
    }

    // Menangani pengeditan user oleh Admin
    @PostMapping("/user/{id}/edit")
    @ResponseBody
    public String editUser(@PathVariable Long id, @RequestParam String username, @RequestParam String email, @RequestParam String role, @RequestParam(required = false) String password) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setRole(role);
            // Update password jika diisi
            if (password != null && !password.isBlank()) {
                user.setPassword(passwordEncoder.encode(password)); // Hash password
            }
            userRepository.save(user);
            return "OK";
        }
        return "NOT_FOUND";
    }
}

