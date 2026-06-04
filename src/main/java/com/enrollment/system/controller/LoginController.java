package com.enrollment.system.controller;

import com.enrollment.system.dto.LoginRequest;
import com.enrollment.system.dto.LoginResponse;
import com.enrollment.system.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private TextField passwordVisibleField;
    
    @FXML
    private Button btnTogglePassword;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    private boolean passwordVisible = false;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private String sessionToken;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        loadingIndicator.setVisible(false);
        
        // Add enter key listener to password fields
        passwordField.setOnAction(event -> handleLogin());
        passwordVisibleField.setOnAction(event -> handleLogin());
        
        // Sync password fields
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (!passwordVisible) {
                passwordVisibleField.setText(newText);
            }
        });
        
        passwordVisibleField.textProperty().addListener((obs, oldText, newText) -> {
            if (passwordVisible) {
                passwordField.setText(newText);
            }
        });
        
        // Set up window shown handler after scene is attached
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) (usernameField != null && usernameField.getScene() != null ? usernameField.getScene().getWindow() : null);
                if (stage != null) {
                    stage.setOnShown(event -> {
                        clearFields();
                    });
                }
            } catch (Exception e) {
                // Ignore if scene is not yet available
            }
        });
    }
    
    public void clearFields() {
        if (usernameField != null) {
            usernameField.clear();
        }
        if (passwordField != null) {
            passwordField.clear();
        }
        if (passwordVisibleField != null) {
            passwordVisibleField.clear();
        }
        if (errorLabel != null) {
            hideError();
        }
        passwordVisible = false;
        if (btnTogglePassword != null) {
            btnTogglePassword.setText("🔍");
        }
        if (passwordVisibleField != null && passwordVisibleField.isVisible()) {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
        }
        if (passwordField != null && !passwordField.isVisible()) {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        
        if (passwordVisible) {
            // Show text field, hide password field
            String currentPassword = passwordField.getText();
            passwordVisibleField.setText(currentPassword);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            btnTogglePassword.setText("🕵️");
        } else {
            // Show password field, hide text field
            String currentPassword = passwordVisibleField.getText();
            passwordField.setText(currentPassword);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            btnTogglePassword.setText("🔍");
        }
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordVisible ? passwordVisibleField.getText() : passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform login in background thread
        new Thread(() -> {
            try {
                LoginRequest loginRequest = new LoginRequest(username, password);
                LoginResponse response = authService.login(loginRequest);
                
                if (response.isSuccess()) {
                    sessionToken = response.getSessionToken();
                    Platform.runLater(() -> {
                        setLoading(false);
                        showSuccessAndNavigate(response);
                    });
                } else {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError(response.getMessage());
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Login failed: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    private void showSuccessAndNavigate(LoginResponse response) {
        try {
            // Show welcome message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Successful");
            alert.setHeaderText("Welcome, " + response.getUser().getFullName() + "!");
            alert.setContentText("Role: " + response.getUser().getRoleDisplayName());
            alert.showAndWait();
            
            // Navigate to dashboard
            loadDashboard(response);
            
        } catch (Exception e) {
            showError("Error loading dashboard");
            e.printStackTrace();
        }
    }
    
    private void loadDashboard(LoginResponse response) {
        try {
            // Check user role and load appropriate dashboard
            String userRole = response.getUser().getRole();
            String fxmlPath;
            String stageTitle;
            
            if ("TEACHER".equals(userRole)) {
                fxmlPath = "/FXML/TeacherDashboard.fxml";
                stageTitle = "Kinderscape - Teacher Dashboard";
            } else {
                fxmlPath = "/FXML/Dashboard.fxml";
                stageTitle = "Kinderscape - Dashboard";
            }
            
            // Load Dashboard FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            
            // Load the FXML - this will call initialize()
            Parent dashboardRoot = loader.load();
            
            // Get dashboard controller and set user session
            Object dashboardController = loader.getController();
            
            if ("TEACHER".equals(userRole)) {
                com.enrollment.system.controller.TeacherDashboardController teacherController = 
                    (com.enrollment.system.controller.TeacherDashboardController) dashboardController;
                if (teacherController != null) {
                    teacherController.setUserSession(response.getUser(), response.getSessionToken());
                }
            } else {
                DashboardController adminController = (DashboardController) dashboardController;
                if (adminController != null) {
                    adminController.setUserSession(response.getUser(), response.getSessionToken());
                }
            }
            
            // Create new stage for dashboard
            Stage dashboardStage = new Stage();
            dashboardStage.setTitle(stageTitle);
            dashboardStage.setScene(new Scene(dashboardRoot));
            dashboardStage.setMaximized(true);
            
            // Hide login window instead of closing (to prevent app shutdown)
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.hide();
            
            // Store login stage reference in dashboard controller
            if ("TEACHER".equals(userRole)) {
                com.enrollment.system.controller.TeacherDashboardController teacherController = 
                    (com.enrollment.system.controller.TeacherDashboardController) dashboardController;
                if (teacherController != null) {
                    teacherController.setLoginStage(loginStage);
                }
            } else {
                DashboardController adminController = (DashboardController) dashboardController;
                if (adminController != null) {
                    adminController.setLoginStage(loginStage);
                }
            }
            
            // Set dashboard to show login when closed via X button
            dashboardStage.setOnCloseRequest(event -> {
                // When dashboard closes, show login again and clear fields
                loginStage.show();
                usernameField.clear();
                passwordField.clear();
                passwordVisibleField.clear();
                passwordVisible = false;
                if (passwordVisibleField.isVisible()) {
                    togglePasswordVisibility();
                }
                hideError();
            });
            
            // Show dashboard
            dashboardStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
            // Show full stack trace for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Dashboard Loading Error");
            errorAlert.setHeaderText("Failed to load dashboard");
            errorAlert.setContentText("Error: " + e.getMessage() + "\n\nCheck console for details.");
            errorAlert.showAndWait();
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        passwordVisibleField.setDisable(loading);
        btnTogglePassword.setDisable(loading);
    }
    
    @FXML
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText("Password Reset");
        alert.setContentText("Please contact the system administrator to reset your password.");
        alert.showAndWait();
    }
}