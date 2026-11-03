package com.learnhub.web;

import com.learnhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService users;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br) {
        if (!br.hasFieldErrors("email") && users.emailTaken(form.getEmail())) {
            br.rejectValue("email", "taken", "This email is already registered");
        }
        if (br.hasErrors()) {
            return "register";
        }
        users.register(form.getName(), form.getEmail(), form.getPassword());
        return "redirect:/login?registered";
    }
}
