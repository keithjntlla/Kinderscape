package com.enrollment.system.dto;

import java.time.LocalDateTime;

import com.enrollment.system.model.User;

public class UserDto {
    
    private Long id;
    private String username;
    private String password; // For creating/updating users
    private String fullName;
    private String email;
    private String role;
    private String roleDisplayName;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private String profilePicture;
    
    // Constructors
    public UserDto() {
    }
    
    public UserDto(Long id, String username, String fullName, String email, 
                   String role, String roleDisplayName, Boolean isActive, LocalDateTime lastLogin) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.roleDisplayName = roleDisplayName;
        this.isActive = isActive;
        this.lastLogin = lastLogin;
    }
    
    // Static factory method
    public static UserDto fromUser(User user) {
        UserDto dto = new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name(),
            user.getRole().getDisplayName(),
            user.getIsActive(),
            user.getLastLogin()
        );
        dto.setProfilePicture(user.getProfilePicture());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getRoleDisplayName() {
        return roleDisplayName;
    }
    
    public void setRoleDisplayName(String roleDisplayName) {
        this.roleDisplayName = roleDisplayName;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public String getProfilePicture() {
        return profilePicture;
    }
    
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}