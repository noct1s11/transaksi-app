package com.example.transaksi_app.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute
    public void addCsrfToken(Model model, HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        if (token != null) {
            model.addAttribute("_csrf", token);
        } else {
            // Fallback: inject dummy token agar Thymeleaf tidak error, dan log ke console
            System.err.println("[CSRF] WARNING: CsrfToken is null on this request!");
            model.addAttribute("_csrf", new Object() {
            });
        }
    }
}
