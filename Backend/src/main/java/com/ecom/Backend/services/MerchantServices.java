package com.ecom.Backend.services;


import com.ecom.Backend.entity.Merchant;
import com.ecom.Backend.repository.MerchantRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MerchantServices {

    private MerchantRepo merchantRepo;

    public Merchant saveMerchant(Merchant merchant){
        return merchantRepo.save(merchant);
    }

    public Merchant getMerchantById(Long id){
        return merchantRepo.findById(id).orElse(null);
    }

    public Merchant getMerchantByEmail(String merchantEmail){
        return merchantRepo.getMerchantByEmail(merchantEmail);
    }

    public Merchant getMerchantByPhone(String merchantPhone){
        return merchantRepo.getMerchantByPhone(merchantPhone);
    }

    public List<Merchant> getAllMerchants(){
        return merchantRepo.findAll();
    }
}
