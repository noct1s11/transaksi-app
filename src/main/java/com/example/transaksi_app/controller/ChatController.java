package com.example.transaksi_app.controller;

import com.example.transaksi_app.model.ChatMessage;
import com.example.transaksi_app.service.ChatService;
import com.example.transaksi_app.model.User;
import com.example.transaksi_app.repository.UserRepository;
import com.example.transaksi_app.model.SimpleUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String chatHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("currentUser", username);
        return "chat/home";
    }

    @GetMapping("/{receiver}")
    public String chatWith(@PathVariable String receiver, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        String sender = userDetails != null ? userDetails.getUsername() : null;
        if (sender == null || receiver == null || receiver.isEmpty()) {
            // Log error jika data tidak valid
            System.err.println("[ChatController] Data user/receiver null: sender=" + sender + ", receiver=" + receiver);
            model.addAttribute("messages", List.of());
            model.addAttribute("receiver", receiver);
            model.addAttribute("currentUser", sender);
            return "chat/chatbox";
        }
        List<ChatMessage> messages = chatService.getChat(sender, receiver);
        model.addAttribute("messages", messages);
        model.addAttribute("receiver", receiver);
        model.addAttribute("currentUser", sender);
        return "chat/chatbox";
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam String receiver, @RequestParam String content, @AuthenticationPrincipal UserDetails userDetails) {
        String sender = userDetails.getUsername();
        chatService.sendMessage(sender, receiver, content);
        return "redirect:/chat/" + receiver;
    }

    // REST: Get all users except current (for chat list)
    @GetMapping("/api/users")
    @ResponseBody
    public List<SimpleUserDTO> getUsers(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(username))
                .map(u -> new SimpleUserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getProfilePicture(), u.getRole()))
                .toList();
    }

    // REST: Get all admins except current (for user chat list)
    @GetMapping("/api/admins")
    @ResponseBody
    public List<SimpleUserDTO> getAdmins(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(username) && u.getRole().equalsIgnoreCase("ADMIN"))
                .map(u -> new SimpleUserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getProfilePicture(), u.getRole()))
                .toList();
    }

    // REST: Get chat messages between current user and target
    @GetMapping("/api/messages/{target}")
    @ResponseBody
    public List<ChatMessage> getMessages(@PathVariable String target, @AuthenticationPrincipal UserDetails userDetails) {
        String current = userDetails.getUsername();
        return chatService.getChat(current, target);
    }

    // REST: Send chat message (AJAX)
    @PostMapping("/api/send")
    @ResponseBody
    public ChatMessage sendMessageAjax(@RequestParam String receiver, @RequestParam String content, @AuthenticationPrincipal UserDetails userDetails) {
        String sender = userDetails.getUsername();
        return chatService.sendMessage(sender, receiver, content);
    }
}
