package com.ecom.Backend.services;

import com.ecom.Backend.entity.Address;
import com.ecom.Backend.entity.User;
import com.ecom.Backend.repository.AddressRepo;
import com.ecom.Backend.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServices {

    private final AddressRepo addressRepo;
    private final UserRepo userRepo;

    @Transactional
    public Address addAddress(Long userId, Address address) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        address.setUser(user);

        List<Address> existing = addressRepo.findByUserId(userId);
        if (existing.isEmpty()) {
            address.setIsDefault(true);
        } else if (address.getIsDefault() != null && address.getIsDefault()) {
            // Unset other default addresses
            for (Address a : existing) {
                if (a.getIsDefault()) {
                    a.setIsDefault(false);
                    addressRepo.save(a);
                }
            }
        }

        return addressRepo.save(address);
    }

    public List<Address> getAddressesByUser(Long userId) {
        return addressRepo.findByUserId(userId);
    }

    @Transactional
    public Address setDefaultAddress(Long userId, Long addressId) {
        List<Address> existing = addressRepo.findByUserId(userId);
        Address target = null;

        for (Address a : existing) {
            if (a.getId().equals(addressId)) {
                a.setIsDefault(true);
                target = a;
            } else {
                a.setIsDefault(false);
            }
            addressRepo.save(a);
        }

        if (target == null) {
            throw new RuntimeException("Address not found with ID: " + addressId + " for user: " + userId);
        }

        return target;
    }
}
