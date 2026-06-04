package com.enrollment.system.service;

import com.enrollment.system.dto.LoginRequest;
import com.enrollment.system.dto.LoginResponse;
import com.enrollment.system.dto.UserDto;
import com.enrollment.system.model.User;
import com.enrollment.system.repository.UserRepository;
import com.enrollment.system.util.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SessionManager sessionManager;
    
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Find user by username
            User user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);
            
            if (user == null) {
                return LoginResponse.failure("Invalid username or password");
            }
            
            // Check if user is active
            if (!user.getIsActive()) {
                return LoginResponse.failure("Account is disabled. Please contact administrator.");
            }
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return LoginResponse.failure("Invalid username or password");
            }
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            // Create session
            UserDto userDto = UserDto.fromUser(user);
            String sessionToken = sessionManager.createSession(userDto);
            
            return LoginResponse.success(userDto, sessionToken);
            
        } catch (Exception e) {
            e.printStackTrace();
            return LoginResponse.failure("An error occurred during login. Please try again.");
        }
    }
    
    public boolean logout(String sessionToken) {
        try {
            sessionManager.invalidateSession(sessionToken);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public UserDto getCurrentUser(String sessionToken) {
        return sessionManager.getUser(sessionToken);
    }
    
    public boolean isSessionValid(String sessionToken) {
        return sessionManager.isValidSession(sessionToken);
    }
    
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword, String sessionToken) {
        try {
            // Validate session
            if (!isSessionValid(sessionToken)) {
                return false;
            }
            
            // Verify the user from session matches the username
            UserDto sessionUser = getCurrentUser(sessionToken);
            if (sessionUser == null || !sessionUser.getUsername().equals(username)) {
                return false;
            }
            
            // Find user by username
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (user == null) {
                return false;
            }
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return false;
            }
            
            // Validate new password
            if (newPassword == null || newPassword.length() < 8) {
                return false;
            }
            
            // Check if new password is different from current
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                return false;
            }
            
            // Encode and set new password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}