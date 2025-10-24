package io.github.srushti1125.aggregator.controller;

import io.github.srushti1125.aggregator.model.User;
import io.github.srushti1125.aggregator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class WebController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Home Page ---
    @GetMapping("/")
    public String home() {
        return "index"; // This will look for src/main/resources/templates/index.html
    }

    // --- Login Page ---
    @GetMapping("/login")
    public String login() {
        return "login"; // This will look for src/main/resources/templates/login.html
    }

    // --- Registration Page ---
    @GetMapping("/register")
    public String register() {
        return "register"; // This will look for src/main/resources/templates/register.html
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String email, @RequestParam String password) {
        // Create new user
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // Hash the password!
        userRepository.save(newUser);

        return "redirect:/login"; // Send them to the login page
    }

    // --- Dashboard (User Preferences) ---
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        // 'Principal' is the currently logged-in user
        String email = principal.getName();
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElseThrow();

        // Pass the user's keywords to the HTML page
        model.addAttribute("keywords", String.join(", ", user.getKeywords()));
        return "dashboard"; // src/main/resources/templates/dashboard.html
    }

    @PostMapping("/keywords")
    public String updateKeywords(@RequestParam String keywords, Principal principal) {
        // Find the logged-in user
        String email = principal.getName();
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElseThrow();

        // Update their keywords
        Set<String> keywordSet = Arrays.stream(keywords.split(","))
                .map(String::trim) // Remove whitespace
                .filter(s -> !s.isEmpty()) // Remove empty strings
                .collect(Collectors.toSet());

        user.setKeywords(keywordSet);
        userRepository.save(user);

        return "redirect:/dashboard"; // Reload the dashboard
    }
}