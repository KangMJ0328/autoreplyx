package com.autoreplyx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;

    @JsonProperty("password_confirmation")
    private String passwordConfirmation;

    private String brandName;

    private String industry;

    // Setter to accept both brandName (camelCase) and brand_name (snake_case)
    @JsonProperty("brand_name")
    public void setBrandNameSnakeCase(String brandName) {
        if (this.brandName == null) {
            this.brandName = brandName;
        }
    }
}
