package com.logancee.globalcart.controller;

import com.logancee.globalcart.model.Address;
import com.logancee.globalcart.model.User;
import com.logancee.globalcart.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getProfile(auth.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(Authentication auth, @RequestBody User updateData) {
        return ResponseEntity.ok(userService.updateProfile(auth.getName(), updateData));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<List<Address>> getAddresses(Authentication auth) {
        return ResponseEntity.ok(userService.getAddresses(auth.getName()));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<List<Address>> addAddress(Authentication auth, @RequestBody Address address) {
        return ResponseEntity.ok(userService.addAddress(auth.getName(), address));
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<List<Address>> updateAddress(Authentication auth, @PathVariable String id, @RequestBody Address address) {
        return ResponseEntity.ok(userService.updateAddress(auth.getName(), id, address));
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<List<Address>> deleteAddress(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(userService.deleteAddress(auth.getName(), id));
    }
}
