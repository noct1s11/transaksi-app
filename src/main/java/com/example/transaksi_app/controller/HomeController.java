package com.example.transaksi_app.controller; // PASTIKAN INI SESUAI

import com.example.transaksi_app.security.UserPrincipal; // PASTIKAN INI SESUAI
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Mengarahkan ke halaman login jika user belum login atau mengakses root URL
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    // Menampilkan halaman login
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    // Menampilkan halaman registrasi
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    // Endpoint yang dituju setelah login sukses, akan mengarahkan user berdasarkan role
    @GetMapping("/default-dashboard")
    public String defaultDashboard(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        if (userPrincipal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        } else if (userPrincipal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER"))) {
            return "redirect:/user/dashboard";
        }
        return "redirect:/login"; // Fallback
    }
}
