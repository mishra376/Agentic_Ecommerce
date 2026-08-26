package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Address;
import com.ecom.Backend.security.CustomUserDetails;
import com.ecom.Backend.services.AddressServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressServices addressServices;

    @PostMapping
    public ResponseEntity<?> addAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Address address
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            Address saved = addressServices.addAddress(userDetails.getId(), address);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyAddresses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        List<Address> addresses = addressServices.getAddressesByUser(userDetails.getId());
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        try {
            Address updated = addressServices.setDefaultAddress(userDetails.getId(), id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
