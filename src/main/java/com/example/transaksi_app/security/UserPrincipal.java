package com.example.transaksi_app.security; // PASTIKAN INI SESUAI

import com.example.transaksi_app.model.User; // PASTIKAN INI SESUAI
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections; // Menggunakan Collections.singletonList untuk satu role

public class UserPrincipal implements UserDetails {

    private User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    // Mengembalikan koleksi GrantedAuthority berdasarkan role pengguna
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security mengharapkan role diawali dengan "ROLE_"
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // Metode lain dari UserDetails (biasanya return true untuk fungsionalitas dasar)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    // Metode tambahan untuk mendapatkan objek User asli
    public User getUser() {
        return user;
    }
}
