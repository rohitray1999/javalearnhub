package com.learnhub.service;

import com.learnhub.config.AppUserDetails;
import com.learnhub.domain.AppUser;
import com.learnhub.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser u = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));
        return new AppUserDetails(u.getId(), u.getEmail(), u.getName(), u.getPasswordHash());
    }

    public boolean emailTaken(String email) {
        return users.existsByEmail(email.trim().toLowerCase());
    }

    @Transactional
    public AppUser register(String name, String email, String rawPassword) {
        AppUser u = new AppUser();
        u.setName(name.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(encoder.encode(rawPassword));
        return users.save(u);
    }
}
