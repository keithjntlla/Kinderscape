package com.enrollment.system.controller;

import com.enrollment.system.dto.UserDto;
import com.enrollment.system.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProfileController {
    
    @FXML
    private Label lblFullName;
    
    @FXML
    private Label lblUsername;
    
    @FXML
    private Label lblEmail;
    
    @FXML
    private Label lblRole;
    
    @FXML
    private Label lblStatus;
    
    @FXML
    private PasswordField txtCurrentPassword;
    
    @FXML
    private TextField txtCurrentPasswordVisible;
    
    @FXML
    private Button btnToggleCurrentPassword;
    
    @FXML
    private PasswordField txtNewPassword;
    
    @FXML
    private TextField txtNewPasswordVisible;
    
    @FXML
    private Button btnToggleNewPassword;
    
    @FXML
    private PasswordField txtConfirmPassword;
    
    @FXML
    private TextField txtConfirmPasswordVisible;
    
    @FXML
    private Button btnToggleConfirmPassword;
    
    @FXML
    private Label lblPasswordError;
    
    @FXML
    private Label lblPasswordSuccess;
    
    @FXML
    private Button btnChangePassword;
    
    @FXML
    private Button btnCancelPassword;
    
    @Autowired
    private AuthService authService;
    
    private UserDto currentUser;
    private String sessionToken;
    
    private boolean currentPasswordVisible = false;
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;
    
    @FXML
    public void initialize() {
        hideMessages();
        
        // Sync password fields - PasswordField to TextField
        txtCurrentPassword.textProperty().addListener((obs, oldText, newText) -> {
            if (!currentPasswordVisible) {
                txtCurrentPasswordVisible.setText(newText);
            }
        });
        
        txtNewPassword.textProperty().addListener((obs, oldText, newText) -> {
            if (!newPasswordVisible) {
                txtNewPasswordVisible.setText(newText);
            }
        });
        
        txtConfirmPassword.textProperty().addListener((obs, oldText, newText) -> {
            if (!confirmPasswordVisible) {
                txtConfirmPasswordVisible.setText(newText);
            }
        });
        
        // Sync password fields - TextField to PasswordField
        txtCurrentPasswordVisible.textProperty().addListener((obs, oldText, newText) -> {
            if (currentPasswordVisible) {
                txtCurrentPassword.setText(newText);
            }
        });
        
        txtNewPasswordVisible.textProperty().addListener((obs, oldText, newText) -> {
            if (newPasswordVisible) {
                txtNewPassword.setText(newText);
            }
        });
        
        txtConfirmPasswordVisible.textProperty().addListener((obs, oldText, newText) -> {
            if (confirmPasswordVisible) {
                txtConfirmPassword.setText(newText);
            }
        });
    }
    
    public void setUserSession(UserDto user, String token) {
        this.currentUser = user;
        this.sessionToken = token;
        loadUserProfile();
    }
    
    private void loadUserProfile() {
        if (currentUser != null) {
            Platform.runLater(() -> {
                lblFullName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "N/A");
                lblUsername.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "N/A");
                lblEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");
                lblRole.setText(currentUser.getRoleDisplayName() != null ? currentUser.getRoleDisplayName() : "N/A");
                lblStatus.setText(currentUser.getIsActive() != null && currentUser.getIsActive() ? "✓ Active" : "✗ Inactive");
                
                if (currentUser.getIsActive() != null && currentUser.getIsActive()) {
                    lblStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            });
        }
    }
    
    @FXML
    private void toggleCurrentPasswordVisibility() {
        currentPasswordVisible = !currentPasswordVisible;
        
        if (currentPasswordVisible) {
            String currentPassword = txtCurrentPassword.getText();
            txtCurrentPasswordVisible.setText(currentPassword);
            txtCurrentPassword.setVisible(false);
            txtCurrentPassword.setManaged(false);
            txtCurrentPasswordVisible.setVisible(true);
            txtCurrentPasswordVisible.setManaged(true);
            btnToggleCurrentPassword.setText("🙈");
        } else {
            String currentPassword = txtCurrentPasswordVisible.getText();
            txtCurrentPassword.setText(currentPassword);
            txtCurrentPasswordVisible.setVisible(false);
            txtCurrentPasswordVisible.setManaged(false);
            txtCurrentPassword.setVisible(true);
            txtCurrentPassword.setManaged(true);
            btnToggleCurrentPassword.setText("👁️");
        }
    }
    
    @FXML
    private void toggleNewPasswordVisibility() {
        newPasswordVisible = !newPasswordVisible;
        
        if (newPasswordVisible) {
            String newPassword = txtNewPassword.getText();
            txtNewPasswordVisible.setText(newPassword);
            txtNewPassword.setVisible(false);
            txtNewPassword.setManaged(false);
            txtNewPasswordVisible.setVisible(true);
            txtNewPasswordVisible.setManaged(true);
            btnToggleNewPassword.setText("🙈");
        } else {
            String newPassword = txtNewPasswordVisible.getText();
            txtNewPassword.setText(newPassword);
            txtNewPasswordVisible.setVisible(false);
            txtNewPasswordVisible.setManaged(false);
            txtNewPassword.setVisible(true);
            txtNewPassword.setManaged(true);
            btnToggleNewPassword.setText("👁️");
        }
    }
    
    @FXML
    private void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible;
        
        if (confirmPasswordVisible) {
            String confirmPassword = txtConfirmPassword.getText();
            txtConfirmPasswordVisible.setText(confirmPassword);
            txtConfirmPassword.setVisible(false);
            txtConfirmPassword.setManaged(false);
            txtConfirmPasswordVisible.setVisible(true);
            txtConfirmPasswordVisible.setManaged(true);
            btnToggleConfirmPassword.setText("🙈");
        } else {
            String confirmPassword = txtConfirmPasswordVisible.getText();
            txtConfirmPassword.setText(confirmPassword);
            txtConfirmPasswordVisible.setVisible(false);
            txtConfirmPasswordVisible.setManaged(false);
            txtConfirmPassword.setVisible(true);
            txtConfirmPassword.setManaged(true);
            btnToggleConfirmPassword.setText("👁️");
        }
    }
    
    @FXML
    private void handleChangePassword() {
        hideMessages();
        
        String currentPassword = currentPasswordVisible ? txtCurrentPasswordVisible.getText() : txtCurrentPassword.getText();
        String newPassword = newPasswordVisible ? txtNewPasswordVisible.getText() : txtNewPassword.getText();
        String confirmPassword = confirmPasswordVisible ? txtConfirmPasswordVisible.getText() : txtConfirmPassword.getText();
        
        // Validation
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required");
            return;
        }
        
        if (newPassword.length() < 8) {
            showError("New password must be at least 8 characters long");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("New password and confirm password do not match");
            return;
        }
        
        if (currentPassword.equals(newPassword)) {
            showError("New password must be different from current password");
            return;
        }
        
        // Disable button during processing
        btnChangePassword.setDisable(true);
        
        // Perform password change in background thread
        new Thread(() -> {
            try {
                boolean success = authService.changePassword(
                    currentUser.getUsername(),
                    currentPassword,
                    newPassword,
                    sessionToken
                );
                
                Platform.runLater(() -> {
                    btnChangePassword.setDisable(false);
                    
                    if (success) {
                        showSuccess("Password changed successfully!");
                        clearPasswordFields();
                    } else {
                        showError("Failed to change password. Please check your current password.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnChangePassword.setDisable(false);
                    showError("Error changing password: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    @FXML
    private void handleCancelPassword() {
        clearPasswordFields();
        hideMessages();
    }
    
    private void clearPasswordFields() {
        txtCurrentPassword.clear();
        txtCurrentPasswordVisible.clear();
        txtNewPassword.clear();
        txtNewPasswordVisible.clear();
        txtConfirmPassword.clear();
        txtConfirmPasswordVisible.clear();
    }
    
    private void showError(String message) {
        lblPasswordError.setText(message);
        lblPasswordError.setVisible(true);
        lblPasswordError.setManaged(true);
        lblPasswordSuccess.setVisible(false);
        lblPasswordSuccess.setManaged(false);
    }
    
    private void showSuccess(String message) {
        lblPasswordSuccess.setText(message);
        lblPasswordSuccess.setVisible(true);
        lblPasswordSuccess.setManaged(true);
        lblPasswordError.setVisible(false);
        lblPasswordError.setManaged(false);
    }
    
    private void hideMessages() {
        lblPasswordError.setVisible(false);
        lblPasswordError.setManaged(false);
        lblPasswordSuccess.setVisible(false);
        lblPasswordSuccess.setManaged(false);
    }
}

