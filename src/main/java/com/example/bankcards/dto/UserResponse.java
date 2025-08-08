package com.example.bankcards.dto;

import com.example.bankcards.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private User.Role role;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }
}


