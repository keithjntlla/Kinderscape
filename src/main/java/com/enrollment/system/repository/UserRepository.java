package com.enrollment.system.repository;

import com.enrollment.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByUsernameAndIsActive(String username, Boolean isActive);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}