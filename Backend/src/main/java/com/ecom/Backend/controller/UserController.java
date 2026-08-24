package com.ecom.Backend.controller;

import com.ecom.Backend.entity.User;
import com.ecom.Backend.services.UserServices;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserServices userServices;

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userServices.saveUser(user);
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
