package com.learnhub.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterForm {
    @NotBlank(message = "Enter your name")
    @Size(max = 80)
    private String name;

    @NotBlank(message = "Enter your email")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
    private String password;
}
