package com.ecom.Backend.controller;

import com.ecom.Backend.entity.User;
import com.ecom.Backend.services.UserServices;
import com.ecom.Backend.dto.LoginRequest;
import com.ecom.Backend.dto.AuthResponse;
import com.ecom.Backend.security.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserServices userServices;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userServices.getUserByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
        }
        if (userServices.getUserByPhone(user.getPhone()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number already exists");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        User savedUser = userServices.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        User user = userServices.getUserByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        
        String token = jwtService.generateToken(user.getEmail(), user.getId(), "ROLE_USER");
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail(), "ROLE_USER"));
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userServices.getUserById(id).orElse(null);
    }

    @GetMapping("/email/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userServices.getUserByEmail(email).orElse(null);
    }

    @GetMapping("/phone/{phone}")
    public User getUserByPhone(@PathVariable String phone) {
        return userServices.getUserByPhone(phone).orElse(null);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userServices.getAllUsers();
    }
}
