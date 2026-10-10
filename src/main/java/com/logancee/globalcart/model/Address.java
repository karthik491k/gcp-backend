package com.logancee.globalcart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String id;
    private String userId;
    private String fullName;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phone;
    private boolean isDefault;
    private String type; // Home, Work, Other
}
