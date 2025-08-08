package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(authority)
        );
    }

    public void saveUser(String username, String encodedPassword, User.Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        User newUser = new User();
        if(role == null){
            newUser.setUsername(username);
            newUser.setPassword(encodedPassword);
            newUser.setRole(User.Role.ADMIN);
        } else {
            if(role.equals(User.Role.USER)){
                newUser.setUsername(username);
                newUser.setPassword(encodedPassword);
                newUser.setRole(User.Role.USER);
            }
            if(role.equals(User.Role.ADMIN)){
                newUser.setUsername(username);
                newUser.setPassword(encodedPassword);
                newUser.setRole(User.Role.ADMIN);
            }
        }

        userRepository.save(newUser);
    }
}
