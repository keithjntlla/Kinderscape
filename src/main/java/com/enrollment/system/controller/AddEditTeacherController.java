package com.enrollment.system.controller;

import com.enrollment.system.dto.UserDto;
import com.enrollment.system.service.TeacherService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URL;
import java.util.ResourceBundle;

@Component
public class AddEditTeacherController implements Initializable {
    
    public enum Mode {
        CREATE, EDIT
    }
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private CheckBox isActiveCheckBox;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    private Mode mode = Mode.CREATE;
    private UserDto teacher;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set default values
        isActiveCheckBox.setSelected(true);
        
        // Set up save button action
        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> handleCancel());
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
        if (titleLabel != null) {
            titleLabel.setText(mode == Mode.CREATE ? "Add New Teacher Account" : "Edit Teacher Account");
        }
        if (saveButton != null) {
            saveButton.setText(mode == Mode.CREATE ? "Create" : "Update");
        }
        
        // In edit mode, password is optional
        if (mode == Mode.EDIT && passwordField != null) {
            passwordField.setPromptText("Leave blank to keep current password");
        }
    }
    
    public void setTeacher(UserDto teacher) {
        this.teacher = teacher;
        if (teacher != null) {
            if (usernameField != null) {
                usernameField.setText(teacher.getUsername());
            }
            if (fullNameField != null) {
                fullNameField.setText(teacher.getFullName());
            }
            if (isActiveCheckBox != null) {
                isActiveCheckBox.setSelected(teacher.getIsActive() != null ? teacher.getIsActive() : true);
            }
            // Don't set password in edit mode
        }
    }
    
    private void handleSave() {
        // Validate fields first (on UI thread)
        if (usernameField.getText().trim().isEmpty()) {
            showError("Username is required");
            return;
        }
        
        if (fullNameField.getText().trim().isEmpty()) {
            showError("Full name is required");
            return;
        }
        
        // Password is required only in create mode
        if (mode == Mode.CREATE && passwordField.getText().trim().isEmpty()) {
            showError("Password is required");
            return;
        }
        
        // Create UserDto
        UserDto teacherDto = new UserDto();
        if (mode == Mode.EDIT && teacher != null) {
            teacherDto.setId(teacher.getId());
        }
        teacherDto.setUsername(usernameField.getText().trim());
        if (!passwordField.getText().trim().isEmpty()) {
            teacherDto.setPassword(passwordField.getText().trim());
        }
        teacherDto.setFullName(fullNameField.getText().trim());
        teacherDto.setIsActive(isActiveCheckBox.isSelected());
        
        // Disable save button to prevent multiple clicks
        saveButton.setDisable(true);
        saveButton.setText("Saving...");
        
        // Run database operation in background thread to prevent UI blocking and ensure proper transaction management
        new Thread(() -> {
            try {
                // Use TransactionTemplate to ensure proper transaction management in background thread
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                transactionTemplate.setTimeout(30); // 30 seconds timeout
                
                // Retry logic for SQLite busy errors
                int maxRetries = 5;
                int retryDelay = 200; // milliseconds
                Exception lastException = null;
                
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        // Execute the save operation within a transaction
                        transactionTemplate.execute(status -> {
                            if (mode == Mode.CREATE) {
                                teacherService.createTeacher(teacherDto);
                            } else {
                                teacherService.updateTeacher(teacher.getId(), teacherDto);
                            }
                            return null;
                        });
                        
                        // Success - break out of retry loop
                        lastException = null;
                        break;
                        
                    } catch (DataAccessException e) {
                        lastException = e;
                        String errorMessage = e.getMessage();
                        
                        // Check if it's a SQLite busy error
                        if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                            errorMessage.contains("database is locked") || 
                            errorMessage.contains("database locked") ||
                            errorMessage.contains("SQLITE_BUSY_SNAPSHOT"))) {
                            
                            if (attempt < maxRetries - 1) {
                                // Wait before retry with exponential backoff
                                try {
                                    Thread.sleep(retryDelay * (attempt + 1));
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    Platform.runLater(() -> {
                                        saveButton.setDisable(false);
                                        saveButton.setText(mode == Mode.CREATE ? "Create" : "Update");
                                        showError("Operation was interrupted. Please try again.");
                                    });
                                    return;
                                }
                                continue; // Retry
                            }
                        }
                        
                        // If it's not a busy error or we've exhausted retries, throw it
                        throw e;
                    }
                }
                
                // If we get here and lastException is not null, all retries failed
                if (lastException != null) {
                    throw lastException;
                }
                
                // Success - update UI on JavaFX thread
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText(mode == Mode.CREATE ? "Create" : "Update");
                    showSuccess(mode == Mode.CREATE ? "Teacher account created successfully!" : "Teacher account updated successfully!");
                    
                    // Close window
                    Stage stage = (Stage) saveButton.getScene().getWindow();
                    stage.close();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                String errorMessage = e.getMessage();
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText(mode == Mode.CREATE ? "Create" : "Update");
                    
                    // Provide more helpful error messages
                    if (errorMessage != null && errorMessage.contains("CHECK constraint")) {
                        showError("Database schema error: The TEACHER role is not recognized in the database.\n\n" +
                                 "Solution: Please delete the database files (enrollment_db.db, enrollment_db.db-wal, enrollment_db.db-shm) " +
                                 "and restart the application. The database will be recreated automatically with the TEACHER role.\n\n" +
                                 "Note: This will delete all existing data. Default admin and registrar accounts will be recreated.");
                    } else if (errorMessage != null && errorMessage.contains("already exists")) {
                        showError(errorMessage);
                    } else if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                               errorMessage.contains("database is locked") || 
                               errorMessage.contains("database locked"))) {
                        showError("Database is currently busy. Please wait a moment and try again.\n\n" +
                                 "If this problem persists, try closing other database connections or restarting the application.");
                    } else {
                        showError("Error saving teacher: " + (errorMessage != null ? errorMessage : "Unknown error occurred"));
                    }
                });
            }
        }).start();
    }
    
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

