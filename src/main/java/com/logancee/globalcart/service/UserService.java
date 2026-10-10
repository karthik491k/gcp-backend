package com.logancee.globalcart.service;

import com.logancee.globalcart.model.Address;
import com.logancee.globalcart.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final SpannerDataService spannerDataService;

    public UserService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public User getProfile(String email) {
        return spannerDataService.findUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
    }

    public User updateProfile(String email, User updateData) {
        User user = getProfile(email);
        if (updateData.getName() != null) user.setName(updateData.getName());
        if (updateData.getPhone() != null) user.setPhone(updateData.getPhone());
        if (updateData.getAvatar() != null) user.setAvatar(updateData.getAvatar());
        return spannerDataService.saveUser(user);
    }

    public List<Address> getAddresses(String email) {
        User user = getProfile(email);
        return spannerDataService.getUserAddresses(user.getId());
    }

    public List<Address> addAddress(String email, Address address) {
        User user = getProfile(email);
        address.setUserId(user.getId());
        return spannerDataService.saveUserAddress(user.getId(), address);
    }

    public List<Address> updateAddress(String email, String addressId, Address address) {
        User user = getProfile(email);
        return spannerDataService.updateAddress(user.getId(), addressId, address);
    }

    public List<Address> deleteAddress(String email, String addressId) {
        User user = getProfile(email);
        return spannerDataService.deleteAddress(user.getId(), addressId);
    }
}
