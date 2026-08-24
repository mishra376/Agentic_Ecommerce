package com.ecom.Backend.repository;

import com.ecom.Backend.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepo extends JpaRepository<Merchant, Long> {

    public Merchant getMerchantByEmail(String merchantEmail);

    public Merchant getMerchantByPhone(String merchantPhone);

    public Optional<Merchant> findByShopDomain(String shopDomain);
}
