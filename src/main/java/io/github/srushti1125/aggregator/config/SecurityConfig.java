package io.github.srushti1125.aggregator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // This creates a "PasswordEncoder" bean, which we will use to hash passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Allow public access to home, register, login, h2 console, AND CSS/JS/IMAGES
                        .requestMatchers("/", "/register", "/login", "/h2-console/**", "/css/**", "/js/**", "/images/**").permitAll() // <-- Make sure /css/** is here
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // Use our custom login page
                        .defaultSuccessUrl("/dashboard", true) // Go to dashboard on success
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/") // Go to home page on logout
                        .permitAll()
                )
                // These are needed to allow the H2 console to work
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}