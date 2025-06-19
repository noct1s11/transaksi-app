package com.example.transaksi_app.controller;

import com.example.transaksi_app.model.Transaction;
import com.example.transaksi_app.model.User;
import com.example.transaksi_app.model.Category;
import com.example.transaksi_app.model.Wallet;
import com.example.transaksi_app.model.PasswordResetToken;
import com.example.transaksi_app.repository.TransactionRepository;
import com.example.transaksi_app.repository.UserRepository;
import com.example.transaksi_app.repository.CategoryRepository;
import com.example.transaksi_app.repository.WalletRepository;
import com.example.transaksi_app.repository.PasswordResetTokenRepository;
import com.example.transaksi_app.security.UserPrincipal;
import com.example.transaksi_app.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.example.transaksi_app.repository.ActivityLogRepository;
import com.example.transaksi_app.model.ActivityLog;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final ActivityLogRepository activityLogRepository;

    public UserController(UserRepository userRepository, TransactionRepository transactionRepository, CategoryRepository categoryRepository, WalletRepository walletRepository, NotificationService notificationService, PasswordResetTokenRepository passwordResetTokenRepository, JavaMailSender mailSender, ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.notificationService = notificationService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
        this.activityLogRepository = activityLogRepository;
    }

    // Tambahkan constant untuk string literal yang sering diulang
    private static final String ATTR_CURRENT_USER = "currentUser";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_CATEGORIES = "categories";
    private static final String ATTR_WALLETS = "wallets";
    private static final String REDIRECT_DASHBOARD = "redirect:/user/dashboard";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String VIEW_FORGOT_PASSWORD = "forgot-password";

    // Jadikan string literal yang sering dipakai sebagai constant
    private static final String PEMASUKAN = "Pemasukan";
    private static final String PENGELUARAN = "Pengeluaran";
    private static final String TOTAL_PEMASUKAN = "totalPemasukan";
    private static final String TOTAL_PENGELUARAN = "totalPengeluaran";
    private static final String TRANSAKSI_PER_TANGGAL = "transaksiPerTanggal";
    private static final String KATEGORI_MAP = "kategoriMap";
    private static final String LAINNYA = "Lainnya";
    private static final String ATTR_CURRENT_URI = "currentUri";

    // Menangani registrasi user baru dengan validasi email
    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, @RequestParam String email, Model model) {
        // Validasi email sederhana
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            model.addAttribute(ATTR_ERROR, "Format email tidak valid.");
            return "register";
        }
        // Cek apakah username sudah ada
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute(ATTR_ERROR, "Username '" + username + "' sudah digunakan. Pilih username lain.");
            return "register";
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Simpan password tanpa hash
        user.setEmail(email);
        user.setRole("USER"); // Default role untuk registrasi adalah USER
        userRepository.save(user);
        return REDIRECT_LOGIN + "?registered"; // Redirect ke login dengan pesan sukses (opsional)
    }

    // Menampilkan dashboard user
    @GetMapping("/user/dashboard")
    public String userDashboard(Authentication authentication, Model model, HttpServletRequest request) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser(); // Ambil objek User asli dari UserPrincipal

        List<Transaction> transactions = transactionRepository.findByUser(user);
        model.addAttribute("transactions", transactions); // Tambahkan baris ini agar data transaksi tampil di dashboard
        model.addAttribute(ATTR_CURRENT_USER, user);
        model.addAttribute(ATTR_MESSAGE, "message");
        // Hitung total pemasukan dan pengeluaran
        double totalPemasukan = transactions.stream().filter(t -> PEMASUKAN.equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double totalPengeluaran = transactions.stream().filter(t -> PENGELUARAN.equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        model.addAttribute(TOTAL_PEMASUKAN, totalPemasukan);
        model.addAttribute(TOTAL_PENGELUARAN, totalPengeluaran);

        // Data untuk grafik garis: total transaksi per tanggal
        Map<String, Double> transaksiPerTanggal = new TreeMap<>();
        transactions.forEach(t -> {
            String tanggal = t.getDate().toString();
            transaksiPerTanggal.put(tanggal, transaksiPerTanggal.getOrDefault(tanggal, 0.0) + t.getAmount());
        });
        model.addAttribute(TRANSAKSI_PER_TANGGAL, transaksiPerTanggal);

        // Data untuk pie chart kategori
        Map<String, Double> kategoriMap = new HashMap<>();
        transactions.forEach(t -> {
            String kategori = (t.getCategory() != null && !t.getCategory().isEmpty()) ? t.getCategory() : LAINNYA;
            kategoriMap.put(kategori, kategoriMap.getOrDefault(kategori, 0.0) + t.getAmount());
        });
        model.addAttribute(KATEGORI_MAP, kategoriMap);
        // Tambahan agar Thymeleaf tidak error: alias kategoriStat
        model.addAttribute("kategoriStat", kategoriMap);
        model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());

        return "user/dashboard";
    }

    // Menangani penambahan transaksi oleh user
    @PostMapping("/user/transaction")
    public String addTransaction(@RequestParam String description,
                                 @RequestParam String amount,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String category, // Kategori opsional
                                 Authentication authentication,
                                 Model model,
                                 HttpServletRequest request) {
        double parsedAmount = 0;
        try {
            // Hilangkan semua karakter selain angka dan koma/titik
            String cleanAmount = amount.replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
            parsedAmount = Double.parseDouble(cleanAmount);
        } catch (Exception e) {
            // Jika gagal parsing, tampilkan error dan isi ulang data dashboard
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            List<Transaction> transactions = transactionRepository.findByUser(user);
            model.addAttribute(ATTR_CURRENT_USER, user);
            model.addAttribute(ATTR_ERROR, "Format jumlah tidak valid. Masukkan angka tanpa pemisah ribuan, contoh: 50000 atau 50000.50");
            // Data grafik
            double totalPemasukan = transactions.stream().filter(t -> PEMASUKAN.equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
            double totalPengeluaran = transactions.stream().filter(t -> PENGELUARAN.equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
            model.addAttribute(TOTAL_PEMASUKAN, totalPemasukan);
            model.addAttribute(TOTAL_PENGELUARAN, totalPengeluaran);
            Map<String, Double> transaksiPerTanggal = new TreeMap<>();
            transactions.forEach(t -> {
                String tanggal = t.getDate().toString();
                transaksiPerTanggal.put(tanggal, transaksiPerTanggal.getOrDefault(tanggal, 0.0) + t.getAmount());
            });
            model.addAttribute(TRANSAKSI_PER_TANGGAL, transaksiPerTanggal);
            Map<String, Double> kategoriMap = new HashMap<>();
            transactions.forEach(t -> {
                String kategori = (t.getCategory() != null && !t.getCategory().isEmpty()) ? t.getCategory() : LAINNYA;
                kategoriMap.put(kategori, kategoriMap.getOrDefault(kategori, 0.0) + t.getAmount());
            });
            model.addAttribute(KATEGORI_MAP, kategoriMap);
            // Tambahan agar Thymeleaf tidak error: alias kategoriStat
            model.addAttribute("kategoriStat", kategoriMap);
            model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());
            return "user/dashboard";
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        try {
            Transaction transaction = new Transaction(description, parsedAmount, type, category, user);
            transactionRepository.save(transaction);
            // Audit log
            activityLogRepository.save(new ActivityLog(user, "Tambah Transaksi", description + ", jumlah: " + parsedAmount));
        } catch (Exception ex) {
            ex.printStackTrace(); // Log error ke console
            model.addAttribute(ATTR_ERROR, "Gagal menyimpan transaksi: " + ex.getMessage());
            return "user/dashboard";
        }
        return REDIRECT_DASHBOARD;
    }

    // Endpoint untuk menampilkan daftar transaksi user
    @GetMapping("/user/transactions")
    public String userTransactions(
            Authentication authentication,
            Model model,
            HttpServletRequest request,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions;
        if (search != null && !search.isEmpty()) {
            transactions = transactionRepository.findByUserAndDescriptionContainingIgnoreCase(user, search);
        } else if (category != null && !category.isEmpty()) {
            transactions = transactionRepository.findByUserAndCategory(user, category);
        } else if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            transactions = transactionRepository.findByUserAndDateBetween(user, java.time.LocalDate.parse(startDate), java.time.LocalDate.parse(endDate));
        } else {
            transactions = transactionRepository.findByUser(user);
        }
        model.addAttribute(ATTR_CURRENT_USER, user);
        model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute(ATTR_MESSAGE, "message");
        model.addAttribute("transactions", transactions);
        return "user/transactions";
    }

    // Menampilkan halaman analitik pengguna
    @GetMapping("/user/analytics")
    public String userAnalytics(Authentication authentication, Model model, HttpServletRequest request) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);
        // Data agregat sama seperti dashboard
        double totalPemasukan = transactions.stream().filter(t -> "Pemasukan".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double totalPengeluaran = transactions.stream().filter(t -> "Pengeluaran".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        model.addAttribute("totalPemasukan", totalPemasukan);
        model.addAttribute("totalPengeluaran", totalPengeluaran);
        Map<String, Double> transaksiPerTanggal = new TreeMap<>();
        transactions.forEach(t -> {
            String tanggal = t.getDate().toString();
            transaksiPerTanggal.put(tanggal, transaksiPerTanggal.getOrDefault(tanggal, 0.0) + t.getAmount());
        });
        model.addAttribute("transaksiPerTanggal", transaksiPerTanggal);
        Map<String, Double> kategoriMap = new HashMap<>();
        transactions.forEach(t -> {
            String kategori = (t.getCategory() != null && !t.getCategory().isEmpty()) ? t.getCategory() : "Lainnya";
            kategoriMap.put(kategori, kategoriMap.getOrDefault(kategori, 0.0) + t.getAmount());
        });
        model.addAttribute("kategoriMap", kategoriMap);
        // Tambahan agar Thymeleaf tidak error: alias kategoriStat
        model.addAttribute("kategoriStat", kategoriMap);
        model.addAttribute(ATTR_CURRENT_URI, request.getRequestURI());
        return "user/analytics";
    }

    // Endpoint untuk upload foto profil user
    @PostMapping("/user/upload-profile-picture")
    public String uploadProfilePicture(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        if (!file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) originalFilename = "unknown";
            String fileName = user.getId() + "_" + StringUtils.cleanPath(originalFilename);
            // Use absolute path for uploads/profile-pictures/
            File uploadPath = new File(System.getProperty("user.dir"), "uploads/profile-pictures/");
            if (!uploadPath.exists()) uploadPath.mkdirs();
            File dest = new File(uploadPath, fileName);
            file.transferTo(dest);
            user.setProfilePicture("/profile-pictures/" + fileName);
            userRepository.save(user);
        }
        return REDIRECT_DASHBOARD;
    }

    // Endpoint untuk mencetak rapor transaksi ke PDF
    @GetMapping("/user/cetak-rapor")
    public void cetakRaporTransaksi(Authentication authentication, HttpServletResponse response) throws IOException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=rapor-transaksi-" + user.getUsername() + ".pdf");
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, Color.BLUE);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, Color.WHITE);
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL, Color.BLACK);

            Paragraph title = new Paragraph("REKAP TRANSAKSI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Nama: " + user.getUsername(), cellFont));
            document.add(new Paragraph("Tanggal Cetak: " + java.time.LocalDate.now(), cellFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 3f, 2f, 2.2f, 2f, 2.2f});

            String[] headers = {"ID", "Deskripsi", "Jumlah", "Tanggal", "Tipe", "Kategori"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(Color.BLUE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6f);
                table.addCell(cell);
            }
            for (Transaction t : transactions) {
                table.addCell(new Phrase(String.valueOf(t.getId()), cellFont));
                table.addCell(new Phrase(t.getDescription(), cellFont));
                table.addCell(new Phrase(String.format("%,.2f Rp", t.getAmount()), cellFont));
                table.addCell(new Phrase(t.getDate().toString(), cellFont));
                table.addCell(new Phrase(t.getType(), cellFont));
                table.addCell(new Phrase(t.getCategory() != null ? t.getCategory() : "-", cellFont));
            }
            document.add(table);
        } finally {
            document.close();
        }
    }

    // Endpoint untuk import transaksi dari file CSV/Excel
    @PostMapping("/user/transactions/import")
    public String importTransactions(@RequestParam("file") MultipartFile file, Authentication authentication, RedirectAttributes redirectAttributes) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        try {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.endsWith(".xlsx")) {
                try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (org.apache.poi.ss.usermodel.Row row : sheet) {
                        if (row.getRowNum() == 0) continue; // skip header
                        String description = row.getCell(0).getStringCellValue();
                        double amount = row.getCell(1).getNumericCellValue();
                        String type = row.getCell(2).getStringCellValue();
                        String category = row.getCell(3) != null ? row.getCell(3).getStringCellValue() : null;
                        Transaction t = new Transaction(description, amount, type, category, user);
                        transactionRepository.save(t);
                    }
                }
            } else if (filename != null && filename.endsWith(".csv")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (first) { first = false; continue; } // skip header
                        String[] parts = line.split(",");
                        if (parts.length < 3) continue;
                        String description = parts[0];
                        double amount = Double.parseDouble(parts[1]);
                        String type = parts[2];
                        String category = parts.length > 3 ? parts[3] : null;
                        Transaction t = new Transaction(description, amount, type, category, user);
                        transactionRepository.save(t);
                    }
                }
            } else {
                redirectAttributes.addFlashAttribute(ATTR_ERROR, "Format file tidak didukung. Hanya .csv dan .xlsx");
                return REDIRECT_DASHBOARD;
            }
            redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Import transaksi berhasil!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Gagal import: " + e.getMessage());
        }
        return REDIRECT_DASHBOARD;
    }

    // Endpoint untuk export transaksi user ke CSV
    @GetMapping("/user/transactions/export")
    public void exportTransactionsCSV(Authentication authentication, HttpServletResponse response) throws IOException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transaksi.csv");
        PrintWriter writer = response.getWriter();
        writer.println("Deskripsi,Jumlah,Tipe,Kategori,Tanggal");
        for (Transaction t : transactions) {
            writer.printf("%s,%.2f,%s,%s,%s\n",
                t.getDescription().replace(","," "),
                t.getAmount(),
                t.getType(),
                t.getCategory() != null ? t.getCategory().replace(","," ") : "",
                t.getDate()
            );
        }
        writer.flush();
        writer.close();
    }

    // Form tambah transaksi user
    @GetMapping("/user/transactions/add")
    public String showAddTransactionForm(Authentication authentication, Model model) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Category> categories = categoryRepository.findByUser(user);
        List<Wallet> wallets = walletRepository.findByUser(user);
        model.addAttribute(ATTR_CATEGORIES, categories);
        model.addAttribute(ATTR_WALLETS, wallets);
        return "user/add-transaction";
    }

    // Proses tambah transaksi user
    @PostMapping("/user/transactions/add")
    public String addTransaction(@RequestParam String description,
                                 @RequestParam double amount,
                                 @RequestParam String type,
                                 @RequestParam String categoryName,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (description == null || description.trim().length() < 3 || description.trim().length() > 100) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Deskripsi minimal 3 karakter dan maksimal 100 karakter.");
            return REDIRECT_DASHBOARD + "/add";
        }
        if (amount <= 0) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, "Jumlah harus lebih dari 0.");
            return REDIRECT_DASHBOARD + "/add";
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setUser(user);
        transaction.setCategory(categoryName);
        transactionRepository.save(transaction);
        // Kirim notifikasi jika transaksi besar
        notificationService.sendLargeTransactionNotification(user, transaction, 1000000.0);
        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, "Transaksi berhasil ditambahkan.");
        return REDIRECT_DASHBOARD;
    }

    // Endpoint request reset password
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return VIEW_FORGOT_PASSWORD;
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        Optional<User> userOpt = userRepository.findAll().stream().filter(u -> email.equals(u.getEmail())).findFirst();
        if (userOpt.isEmpty()) {
            model.addAttribute(ATTR_ERROR, "Email tidak ditemukan.");
            return VIEW_FORGOT_PASSWORD;
        }
        User user = userOpt.get();
        // Hapus token lama jika ada
        passwordResetTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);
        // Kirim email
        String resetUrl = "http://localhost:8080/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Reset Password");
        message.setText("Klik link berikut untuk reset password: " + resetUrl);
        mailSender.send(message);
        model.addAttribute(ATTR_MESSAGE, "Link reset password telah dikirim ke email Anda.");
        return VIEW_FORGOT_PASSWORD;
    }
}
