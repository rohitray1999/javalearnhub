package com.learnhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Padhna sab ke liye free: sirf GET public hai
                .requestMatchers(HttpMethod.GET,
                        "/", "/learn", "/learn/**", "/paths", "/paths/**",
                        "/search", "/interview", "/login", "/register", "/error",
                        "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/register", "/api/quiz/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", false)
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
