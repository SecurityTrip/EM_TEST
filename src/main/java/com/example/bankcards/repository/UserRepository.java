package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String name);
    org.springframework.data.domain.Page<User> findByUsernameContainingIgnoreCase(String username, org.springframework.data.domain.Pageable pageable);

}
