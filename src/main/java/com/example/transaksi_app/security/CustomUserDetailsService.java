package com.example.transaksi_app.security; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.User; // PASTIKAN INI SESUAI
import com.example.transaksi_app.repository.UserRepository; // PASTIKAN INI SESUAI
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service; // Pastikan ini diimport

@Service // Pastikan anotasi ini ada dan benar
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Mencari user berdasarkan username dari database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Mengembalikan objek UserDetails yang diimplementasikan oleh UserPrincipal
        return new UserPrincipal(user);
    }
}
