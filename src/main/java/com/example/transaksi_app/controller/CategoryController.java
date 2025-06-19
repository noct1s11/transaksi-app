package com.example.transaksi_app.controller;

import com.example.transaksi_app.model.ActivityLog;
import com.example.transaksi_app.model.Category;
import com.example.transaksi_app.model.User;
import com.example.transaksi_app.repository.ActivityLogRepository;
import com.example.transaksi_app.repository.CategoryRepository;
import com.example.transaksi_app.repository.UserRepository;
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
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private static final String ATTR_CATEGORIES = "categories";
    private static final String REDIRECT_CATEGORIES = "redirect:/categories";
    private static final String ATTR_ERROR = "error";

    public CategoryController(CategoryRepository categoryRepository, UserRepository userRepository, ActivityLogRepository activityLogRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public String listCategories(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        List<Category> categories = categoryRepository.findByUser(user);
        model.addAttribute(ATTR_CATEGORIES, categories);
        return "user/categories";
    }

    @PostMapping("/add")
    public String addCategory(@RequestParam String name, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Category category = new Category(name, user);
        categoryRepository.save(category);
        // Audit log
        activityLogRepository.save(new ActivityLog(user, "Tambah Kategori", name));
        return REDIRECT_CATEGORIES;
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Category category = categoryRepository.findById(id).orElse(null);
        categoryRepository.deleteById(id);
        // Audit log
        activityLogRepository.save(new ActivityLog(user, "Hapus Kategori", category != null ? category.getName() : "ID: " + id));
        return REDIRECT_CATEGORIES;
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable Long id, @RequestParam String name, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        Category category = categoryRepository.findById(id).orElse(null);
        if (category != null) {
            category.setName(name);
            categoryRepository.save(category);
            // Audit log
            activityLogRepository.save(new ActivityLog(user, "Edit Kategori", name));
        }
        return REDIRECT_CATEGORIES;
    }

    // Export kategori ke CSV
    @GetMapping("/export")
    public void exportCategoriesCSV(Principal principal, HttpServletResponse response) throws IOException {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        List<Category> categories = categoryRepository.findByUser(user);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=categories.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Nama");
            for (Category c : categories) {
                writer.printf("%s%n", c.getName().replace(","," "));
            }
        }
    }
    // Import kategori dari CSV
    @PostMapping("/import")
    public String importCategoriesCSV(@RequestParam("file") MultipartFile file, Principal principal, Model model) {
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
                if (parts.length < 1) continue;
                String name = parts[0];
                Category category = new Category(name, user);
                categoryRepository.save(category);
            }
        } catch (IOException e) {
            model.addAttribute(ATTR_ERROR, "Gagal import: " + e.getMessage());
        }
        return REDIRECT_CATEGORIES;
    }
}
