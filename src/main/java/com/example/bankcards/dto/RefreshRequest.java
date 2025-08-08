package com.example.bankcards.dto;

public class RefreshRequest {
    @jakarta.validation.constraints.NotBlank
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
