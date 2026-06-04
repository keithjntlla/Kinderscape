package com.enrollment.system.controller;

import com.enrollment.system.dto.ChangePasswordRequest;
import com.enrollment.system.dto.LoginRequest;
import com.enrollment.system.dto.LoginResponse;
import com.enrollment.system.dto.UserDto;
import com.enrollment.system.service.AuthService;
import com.enrollment.system.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private TeacherService teacherService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String sessionToken) {
        boolean success = authService.logout(sessionToken);
        
        if (success) {
            return ResponseEntity.ok("Logged out successfully");
        } else {
            return ResponseEntity.badRequest().body("Logout failed");
        }
    }
    
    @GetMapping("/current-user")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("Authorization") String sessionToken) {
        UserDto user = authService.getCurrentUser(sessionToken);
        
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).build();
        }
    }
    
    @GetMapping("/validate-session")
    public ResponseEntity<Boolean> validateSession(@RequestHeader("Authorization") String sessionToken) {
        boolean isValid = authService.isSessionValid(sessionToken);
        return ResponseEntity.ok(isValid);
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String sessionToken,
            @Valid @RequestBody ChangePasswordRequest request) {
        boolean success = authService.changePassword(
            request.getUsername(),
            request.getCurrentPassword(),
            request.getNewPassword(),
            sessionToken
        );
        
        if (success) {
            return ResponseEntity.ok("Password changed successfully");
        } else {
            return ResponseEntity.status(400).body("Failed to change password. Please check your current password.");
        }
    }
    
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String sessionToken,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "profilePicture", required = false) MultipartFile profilePictureFile) {
        try {
            // Validate session
            UserDto currentUser = authService.getCurrentUser(sessionToken);
            if (currentUser == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid session");
                return ResponseEntity.status(401).body(error);
            }
            
            // Check if user is a teacher
            if (!"TEACHER".equals(currentUser.getRole())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only teachers can update their profile");
                return ResponseEntity.status(403).body(error);
            }
            
            String profilePicturePath = null;
            
            // Handle profile picture upload
            if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
                // Validate file size (3 MB = 3 * 1024 * 1024 bytes)
                long maxSize = 3 * 1024 * 1024;
                if (profilePictureFile.getSize() > maxSize) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Profile picture size must be less than 3 MB");
                    return ResponseEntity.status(400).body(error);
                }
                
                // Validate file type (images only)
                String contentType = profilePictureFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Only image files are allowed");
                    return ResponseEntity.status(400).body(error);
                }
                
                // Save file and get path
                profilePicturePath = saveProfilePicture(currentUser.getId(), profilePictureFile);
            }
            
            // Update teacher profile
            UserDto updatedUser = teacherService.updateTeacherProfile(
                currentUser.getId(),
                fullName,
                profilePicturePath
            );
            
            return ResponseEntity.ok(updatedUser);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    private String saveProfilePicture(Long userId, MultipartFile file) throws Exception {
        // Create uploads directory if it doesn't exist
        java.io.File uploadsDir = new java.io.File("uploads/profile-pictures");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }
        
        // Generate unique filename: userId_timestamp_originalFilename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = userId + "_" + System.currentTimeMillis() + extension;
        
        // Save file
        java.io.File targetFile = new java.io.File(uploadsDir, filename);
        file.transferTo(targetFile);
        
        // Return relative path
        return "uploads/profile-pictures/" + filename;
    }
}