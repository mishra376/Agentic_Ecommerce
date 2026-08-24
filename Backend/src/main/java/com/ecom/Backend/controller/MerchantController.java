package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Merchant;
import com.ecom.Backend.services.MerchantServices;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@AllArgsConstructor
public class MerchantController {

    private final MerchantServices merchantServices;

    @PostMapping("/register")
    public Merchant registerMerchant(@RequestBody Merchant merchant) {
        return merchantServices.saveMerchant(merchant);
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