package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Merchant;
import com.ecom.Backend.services.MerchantServices;
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
@RequestMapping("/api/merchants")
@AllArgsConstructor
public class MerchantController {

    private final MerchantServices merchantServices;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> registerMerchant(@RequestBody Merchant merchant) {
        if (merchantServices.getMerchantByEmail(merchant.getEmail()) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
        }
        if (merchantServices.getMerchantByPhone(merchant.getPhone()) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Phone number already exists");
        }
        merchant.setPasswordHash(passwordEncoder.encode(merchant.getPasswordHash()));
        Merchant savedMerchant = merchantServices.saveMerchant(merchant);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMerchant);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginMerchant(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        Merchant merchant = merchantServices.getMerchantByEmail(request.getEmail());
        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Merchant not found after authentication");
        }

        String token = jwtService.generateToken(merchant.getEmail(), merchant.getId(), "ROLE_MERCHANT");
        return ResponseEntity.ok(new AuthResponse(token, merchant.getId(), merchant.getEmail(), "ROLE_MERCHANT"));
    }

    @GetMapping("/{id}")
    public Merchant getMerchantById(@PathVariable Long id) {
        return merchantServices.getMerchantById(id);
    }

    @GetMapping("/email/{merchantEmail}")
    public Merchant getMerchantByEmail(@PathVariable String merchantEmail) {
        return merchantServices.getMerchantByEmail(merchantEmail);
    }

    @GetMapping("/phone/{merchantPhone}")
    public Merchant getMerchantByPhone(@PathVariable String merchantPhone) {
        return merchantServices.getMerchantByPhone(merchantPhone);
    }

    @GetMapping
    public List<Merchant> getAllMerchants() {
        return merchantServices.getAllMerchants();
    }
}