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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

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
    public String userDashboard(Authentication authentication, Model model, HttpServletRequest request) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser(); // Ambil objek User asli dari UserPrincipal

        List<Transaction> transactions = transactionRepository.findByUser(user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentUser", user);
        // Hitung total pemasukan dan pengeluaran
        double totalPemasukan = transactions.stream().filter(t -> "Pemasukan".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double totalPengeluaran = transactions.stream().filter(t -> "Pengeluaran".equalsIgnoreCase(t.getType())).mapToDouble(Transaction::getAmount).sum();
        model.addAttribute("totalPemasukan", totalPemasukan);
        model.addAttribute("totalPengeluaran", totalPengeluaran);

        // Data untuk grafik garis: total transaksi per tanggal
        Map<String, Double> transaksiPerTanggal = new TreeMap<>();
        transactions.forEach(t -> {
            String tanggal = t.getDate().toString();
            transaksiPerTanggal.put(tanggal, transaksiPerTanggal.getOrDefault(tanggal, 0.0) + t.getAmount());
        });
        model.addAttribute("transaksiPerTanggal", transaksiPerTanggal);

        // Data untuk pie chart kategori
        Map<String, Double> kategoriMap = new HashMap<>();
        transactions.forEach(t -> {
            String kategori = (t.getCategory() != null && !t.getCategory().isEmpty()) ? t.getCategory() : "Lainnya";
            kategoriMap.put(kategori, kategoriMap.getOrDefault(kategori, 0.0) + t.getAmount());
        });
        model.addAttribute("kategoriMap", kategoriMap);
        model.addAttribute("currentUri", request.getRequestURI());

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
    public String userTransactions(Authentication authentication, Model model, HttpServletRequest request) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentUser", user);
        model.addAttribute("currentUri", request.getRequestURI());
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
        model.addAttribute("currentUri", request.getRequestURI());
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
        return "redirect:/user/dashboard";
    }

    // Endpoint untuk mencetak rapor transaksi ke PDF
    @GetMapping("/user/cetak-rapor")
    public void cetakRaporTransaksi(Authentication authentication, HttpServletResponse response) throws Exception {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        List<Transaction> transactions = transactionRepository.findByUser(user);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=rapor-transaksi-" + user.getUsername() + ".pdf");
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLUE);
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

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
        document.close();
    }
}
