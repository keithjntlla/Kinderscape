package com.enrollment.system.dto;

public class LoginResponse {
    
    private boolean success;
    private String message;
    private UserDto user;
    private String sessionToken;
    
    // Constructors
    public LoginResponse() {
    }
    
    public LoginResponse(boolean success, String message, UserDto user, String sessionToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.sessionToken = sessionToken;
    }
    
    // Static factory methods
    public static LoginResponse success(UserDto user, String sessionToken) {
        return new LoginResponse(true, "Login successful", user, sessionToken);
    }
    
    public static LoginResponse failure(String message) {
        return new LoginResponse(false, message, null, null);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public UserDto getUser() {
        return user;
    }
    
    public void setUser(UserDto user) {
        this.user = user;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}