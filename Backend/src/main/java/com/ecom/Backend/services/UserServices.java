package com.ecom.Backend.services;

import com.ecom.Backend.entity.User;
import com.ecom.Backend.repository.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServices {

    private final UserRepo userRepo;

    public User saveUser(User user) {
        return userRepo.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepo.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public Optional<User> getUserByPhone(String phone) {
        return userRepo.findByPhone(phone);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }
}
