package com.example.bankcards.util;

import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;

public class UserMapper {
    public static UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        r.setRole(u.getRole());
        return r;
    }
}


