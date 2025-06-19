package com.example.transaksi_app.controller;

import com.example.transaksi_app.model.Wallet;
import com.example.transaksi_app.model.User;
import com.example.transaksi_app.repository.WalletRepository;
import com.example.transaksi_app.repository.UserRepository;
import com.example.transaksi_app.repository.ActivityLogRepository;
import com.example.transaksi_app.model.ActivityLog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/wallets")
public class WalletController {
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private static final String ATTR_WALLETS = "wallets";
    private static final String REDIRECT_WALLETS = "redirect:/wallets";
    private static final String VIEW_USER_WALLETS = "user/wallets";
    private static final String ATTR_ERROR = "error";

    public WalletController(WalletRepository walletRepository, UserRepository userRepository, ActivityLogRepository activityLogRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public String listWallets(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        List<Wallet> wallets = walletRepository.findByUser(user);
        model.addAttribute(ATTR_WALLETS, wallets);
        return VIEW_USER_WALLETS;
    }

    @PostMapping("/add")
    public String addWallet(@RequestParam String name, @RequestParam Double balance, Principal principal, Model model) {
        if (balance == null || balance < 0) {
            model.addAttribute(ATTR_ERROR, "Saldo awal tidak boleh negatif.");
            return VIEW_USER_WALLETS;
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Wallet wallet = new Wallet(name, balance, user);
        walletRepository.save(wallet);
        // Audit log
        activityLogRepository.save(new ActivityLog(user, "Tambah Wallet", name + ", saldo: " + balance));
        return REDIRECT_WALLETS;
    }

    @PostMapping("/delete/{id}")
    public String deleteWallet(@PathVariable Long id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Wallet wallet = walletRepository.findById(id).orElse(null);
        walletRepository.deleteById(id);
        // Audit log
        activityLogRepository.save(new ActivityLog(user, "Hapus Wallet", wallet != null ? wallet.getName() : "ID: " + id));
        return REDIRECT_WALLETS;
    }

    @PostMapping("/edit/{id}")
    public String editWallet(@PathVariable Long id, @RequestParam String name, @RequestParam Double balance, Principal principal, Model model) {
        if (balance == null || balance < 0) {
            model.addAttribute(ATTR_ERROR, "Saldo tidak boleh negatif.");
            return VIEW_USER_WALLETS;
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Wallet wallet = walletRepository.findById(id).orElse(null);
        if (wallet != null) {
            wallet.setName(name);
            wallet.setBalance(balance);
            walletRepository.save(wallet);
            // Audit log
            activityLogRepository.save(new ActivityLog(user, "Edit Wallet", name + ", saldo: " + balance));
        }
        return REDIRECT_WALLETS;
    }

    // Export wallet ke CSV
    @GetMapping("/export")
    public void exportWalletsCSV(Principal principal, HttpServletResponse response) throws Exception {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        List<Wallet> wallets = walletRepository.findByUser(user);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=wallets.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Nama,Saldo");
            for (Wallet w : wallets) {
                writer.printf("%s,%.2f%n", w.getName().replace(","," "), w.getBalance()); // Ganti semua \n pada writer.printf menjadi %n untuk line separator yang benar
            }
        } catch (Exception e) {
            throw new IOException("Gagal menulis file CSV: " + e.getMessage(), e);
        }
    }
    // Import wallet dari CSV
    @PostMapping("/import")
    public String importWalletsCSV(@RequestParam("file") MultipartFile file, Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                String name = parts[0];
                double balance = Double.parseDouble(parts[1]);
                Wallet wallet = new Wallet(name, balance, user);
                walletRepository.save(wallet);
            }
        } catch (IOException e) {
            model.addAttribute(ATTR_ERROR, "Gagal mengimpor file: " + e.getMessage());
        } catch (NumberFormatException e) {
            model.addAttribute(ATTR_ERROR, "Format angka tidak valid: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute(ATTR_ERROR, "Gagal import: " + e.getMessage());
        }
        return REDIRECT_WALLETS;
    }
}
