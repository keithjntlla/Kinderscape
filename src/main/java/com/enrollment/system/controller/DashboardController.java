package com.enrollment.system.controller;

import com.enrollment.system.dto.UserDto;
import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.service.StudentService;
import com.enrollment.system.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DashboardController {
    
    @FXML
    private Label userNameLabel;
    
    @FXML
    private Label userRoleLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button btnDashboard;
    
    @FXML
    private Button btnStudentManagement;
    
    @FXML
    private Button btnAddStudent;
    
    @FXML
    private Button btnViewStudents;
    
    @FXML
    private VBox studentManagementSubmenu;
    
    @FXML
    private Button btnReports;
    
    @FXML
    private Button btnStudentListBySection;
    
    @FXML
    private Button btnTeacherAssignmentReport;
    
    @FXML
    private Button btnEnrollmentSummary;
    
    @FXML
    private Button btnArchiveStudents;
    
    @FXML
    private VBox reportsSubmenu;
    
    @FXML
    private Button btnSettings;
    
    @FXML
    private Button btnUserAccounts;
    
    @FXML
    private Button btnProfile;
    
    @FXML
    private Button btnTeacherAccountManagement;
    
    @FXML
    private Button btnSchoolYearManagement;
    
    @FXML
    private VBox settingsSubmenu;
    
    @FXML
    private VBox userAccountsSubmenu;
    
    @FXML
    private VBox dashboardContent;
    
    @FXML
    private VBox adminSection;
    
    @FXML
    private Button btnStrandAndSection;
    
    @FXML
    private Button btnSubjectManagement;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired(required = false)
    private StudentService studentService;
    
    @Autowired(required = false)
    private com.enrollment.system.service.SchoolYearService schoolYearService;
    
    @Autowired(required = false)
    private com.enrollment.system.repository.StudentRepository studentRepository;
    
    @Autowired(required = false)
    private AuthService authService;
    
    private UserDto currentUser;
    private String sessionToken;
    private boolean studentManagementExpanded = false;
    private boolean reportsExpanded = false;
    private boolean settingsExpanded = false;
    private boolean userAccountsExpanded = false;
    private Stage loginStage; // Reference to login stage
    
    @FXML
    public void initialize() {
        // Minimal initialization - just set button style
        // Everything else is deferred to prevent blocking login
        try {
            if (btnDashboard != null) {
                btnDashboard.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 14 20; -fx-alignment: center-left; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 250; -fx-effect: dropshadow(gaussian, rgba(52,152,219,0.4), 8, 0, 0, 0);");
            }
        } catch (Exception e) {
            // Ignore - don't break initialization
        }
        
        // Set up hover effects after a short delay to ensure UI is ready
        // Use a separate thread to avoid blocking
        new Thread(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure UI is initialized
                Platform.runLater(() -> {
                    try {
                        setupButtonHoverEffects();
                    } catch (Exception e) {
                        // Silently fail - hover effects are optional
                    }
                });
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }
    
    private void setupButtonHoverEffects() {
        // Main menu buttons with hover effects
        // Use try-catch for each to prevent one failure from breaking all
        try {
            setupMainButtonHover(btnDashboard, "#3498db");
            setupMainButtonHover(btnStudentManagement, "#3498db");
            setupMainButtonHover(btnStrandAndSection, "#9b59b6");
            setupMainButtonHover(btnSubjectManagement, "#16a085");
            setupMainButtonHover(btnReports, "#3498db");
            setupMainButtonHover(btnSettings, "#e67e22");
        } catch (Exception e) {
            System.err.println("Warning: Error setting up main button hover effects: " + e.getMessage());
        }
        
        // Submenu buttons with unique hover colors
        try {
            setupSubmenuButtonHover(btnAddStudent, "#27ae60");
            setupSubmenuButtonHover(btnViewStudents, "#3498db");
            setupSubmenuButtonHover(btnStudentListBySection, "#3498db");
            setupSubmenuButtonHover(btnTeacherAssignmentReport, "#9b59b6");
            setupSubmenuButtonHover(btnEnrollmentSummary, "#16a085");
            setupSubmenuButtonHover(btnArchiveStudents, "#95a5a6");
            setupSubmenuButtonHover(btnUserAccounts, "#3498db");
            setupSubmenuButtonHover(btnProfile, "#3498db");
            setupSubmenuButtonHover(btnTeacherAccountManagement, "#9b59b6");
            setupSubmenuButtonHover(btnSchoolYearManagement, "#16a085");
        } catch (Exception e) {
            System.err.println("Warning: Error setting up submenu button hover effects: " + e.getMessage());
        }
        
        // Logout button hover effect
        try {
            if (logoutButton != null) {
                String logoutHoverStyle = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12 30; -fx-cursor: hand; -fx-pref-width: 250; -fx-effect: dropshadow(gaussian, rgba(231,76,60,0.5), 12, 0, 0, 3); -fx-translate-y: -2;";
                final String[] originalLogoutStyle = {logoutButton.getStyle()};
                
                logoutButton.setOnMouseEntered(e -> {
                    originalLogoutStyle[0] = logoutButton.getStyle();
                    logoutButton.setStyle(logoutHoverStyle);
                });
                logoutButton.setOnMouseExited(e -> {
                    logoutButton.setStyle(originalLogoutStyle[0]);
                });
            }
        } catch (Exception e) {
            System.err.println("Warning: Error setting up logout button hover effect: " + e.getMessage());
        }
    }
    
    private void setupMainButtonHover(Button button, String hoverColor) {
        if (button == null) return;
        
        String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 14 20; -fx-alignment: center-left; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 250; -fx-effect: dropshadow(gaussian, rgba(52,152,219,0.4), 8, 0, 0, 0); -fx-translate-x: 5;";
        
        // Store original style
        final String[] originalStyle = {button.getStyle()};
        
        button.setOnMouseEntered(e -> {
            originalStyle[0] = button.getStyle();
            button.setStyle(hoverStyle);
        });
        button.setOnMouseExited(e -> {
            // Reset to original style (which might be active state)
            button.setStyle(originalStyle[0]);
        });
    }
    
    private void setupSubmenuButtonHover(Button button, String hoverColor) {
        if (button == null) return;
        
        String hoverStyle = "-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 11 18; -fx-alignment: center-left; -fx-background-radius: 6; -fx-cursor: hand; -fx-pref-width: 225; -fx-translate-x: 3;";
        
        // Store original style
        final String[] originalStyle = {button.getStyle()};
        
        button.setOnMouseEntered(e -> {
            originalStyle[0] = button.getStyle();
            button.setStyle(hoverStyle);
        });
        button.setOnMouseExited(e -> {
            // Reset to original style
            button.setStyle(originalStyle[0]);
        });
    }
    
    public void setUserSession(UserDto user, String token) {
        this.currentUser = user;
        this.sessionToken = token;
        
        if (userNameLabel != null && user != null) {
            userNameLabel.setText("Welcome, " + user.getFullName());
        }
        if (userRoleLabel != null && user != null) {
            userRoleLabel.setText("(" + user.getRoleDisplayName() + ")");
        }
        
        if (adminSection != null && user != null && !"ADMIN".equals(user.getRole())) {
            adminSection.setVisible(false);
            adminSection.setManaged(false);
        }
        
        // Set up close handler to show login when dashboard closes
        // Use Platform.runLater to ensure scene is attached
        Platform.runLater(() -> {
            try {
                Stage dashboardStage = (Stage) (userNameLabel != null && userNameLabel.getScene() != null ? userNameLabel.getScene().getWindow() : null);
                if (dashboardStage != null) {
                    dashboardStage.setOnCloseRequest(event -> {
                        // When dashboard closes, show login again
                        if (loginStage != null) {
                            loginStage.show();
                        } else {
                            // If login stage reference is lost, create a new login window
                            loadLoginPage();
                        }
                    });
                }
            } catch (Exception e) {
                // Ignore if scene is not yet available
            }
        });
        
        // Load dashboard statistics after user session is set
        // Use Platform.runLater to ensure UI is ready
        Platform.runLater(() -> {
            try {
                loadDashboardStatistics();
            } catch (Exception e) {
                // Don't let dashboard loading break login
                e.printStackTrace();
            }
        });
    }
    
    public void setLoginStage(Stage loginStage) {
        this.loginStage = loginStage;
    }
    
    
    @FXML
    private void showDashboard() {
        resetMainButtonStyles();
        if (btnDashboard != null) {
            btnDashboard.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 14 20; -fx-alignment: center-left; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 250; -fx-effect: dropshadow(gaussian, rgba(52,152,219,0.4), 8, 0, 0, 0);");
        }
        // Load dashboard statistics
        loadDashboardStatistics();
    }
    
    private void loadDashboardStatistics() {
        if (dashboardContent == null) {
            return;
        }
        
        // If studentService is not available, show a message
        if (studentService == null) {
            dashboardContent.getChildren().clear();
            Label messageLabel = new Label("Dashboard statistics will be available after login.");
            messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            dashboardContent.getChildren().add(messageLabel);
            return;
        }
        
        // Clear existing content
        dashboardContent.getChildren().clear();
        
        // Load statistics in background thread
        new Thread(() -> {
            try {
                List<StudentDto> allStudents = studentService.getAllStudents();
                
                // Calculate statistics
                long totalEnrolled = allStudents.stream()
                    .filter(s -> "Enrolled".equals(s.getEnrollmentStatus()))
                    .count();
                
                long grade11 = allStudents.stream()
                    .filter(s -> "Enrolled".equals(s.getEnrollmentStatus()) && 
                                s.getGradeLevel() != null && s.getGradeLevel() == 11)
                    .count();
                
                long grade12 = allStudents.stream()
                    .filter(s -> "Enrolled".equals(s.getEnrollmentStatus()) && 
                                s.getGradeLevel() != null && s.getGradeLevel() == 12)
                    .count();
                
                long pending = allStudents.stream()
                    .filter(s -> "Pending".equals(s.getEnrollmentStatus()))
                    .count();
                
                long male = allStudents.stream()
                    .filter(s -> "Enrolled".equals(s.getEnrollmentStatus()) && 
                                "Male".equals(s.getSex()))
                    .count();
                
                long female = allStudents.stream()
                    .filter(s -> "Enrolled".equals(s.getEnrollmentStatus()) && 
                                "Female".equals(s.getSex()))
                    .count();
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    buildDashboardCards(totalEnrolled, grade11, grade12, pending, male, female);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Error loading dashboard statistics: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    dashboardContent.getChildren().add(errorLabel);
                });
            }
        }).start();
    }
    
    private void buildDashboardCards(long totalEnrolled, long grade11, long grade12, long pending, long male, long female) {
        dashboardContent.getChildren().clear();
        
        // Title
        Label titleLabel = new Label("📊 Enrollment Dashboard Summary");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 20 0;");
        dashboardContent.getChildren().add(titleLabel);
        
        // First row: Main statistics
        HBox firstRow = new HBox(20);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        firstRow.getChildren().addAll(
            createStatCard("🎓 Total Enrolled", String.valueOf(totalEnrolled), "#3498db", "The overall number of officially enrolled students"),
            createStatCard("📚 Grade 11", String.valueOf(grade11), "#9b59b6", "Number of currently enrolled Grade 11 students"),
            createStatCard("🎓 Grade 12", String.valueOf(grade12), "#e67e22", "Number of currently enrolled Grade 12 students")
        );
        dashboardContent.getChildren().add(firstRow);
        
        // Second row: Pending and Gender stats
        HBox secondRow = new HBox(20);
        secondRow.setAlignment(Pos.CENTER_LEFT);
        secondRow.getChildren().addAll(
            createStatCard("⏳ Pending Applications", String.valueOf(pending), "#f39c12", "Students who submitted info but still need approval"),
            createStatCard("👨 Male Students", String.valueOf(male), "#16a085", "Number of enrolled male students"),
            createStatCard("👩 Female Students", String.valueOf(female), "#e91e63", "Number of enrolled female students")
        );
        dashboardContent.getChildren().add(secondRow);
    }
    
    private VBox createStatCard(String title, String value, String color, String description) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(25));
        card.setPrefWidth(280);
        card.setPrefHeight(180);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); " +
            "-fx-border-radius: 15;"
        );
        
        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        
        // Value
        Label valueLabel = new Label(value);
        valueLabel.setStyle(
            "-fx-font-size: 48px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: " + color + ";"
        );
        
        // Description
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
        descLabel.setMaxWidth(250);
        
        // Decorative line
        Region line = new Region();
        line.setPrefHeight(3);
        line.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
        line.setMaxWidth(60);
        
        VBox contentBox = new VBox(8);
        contentBox.getChildren().addAll(titleLabel, valueLabel, line, descLabel);
        
        card.getChildren().add(contentBox);
        
        return card;
    }
    
    @FXML
    private void toggleStudentManagement() {
        studentManagementExpanded = !studentManagementExpanded;
        if (studentManagementSubmenu != null) {
            studentManagementSubmenu.setVisible(studentManagementExpanded);
            studentManagementSubmenu.setManaged(studentManagementExpanded);
        }
        if (btnStudentManagement != null) {
            btnStudentManagement.setText("  👥 Student Management");
        }
    }
    
    @FXML
    private void showAddStudent() {
        resetSubmenuButtonStyles();
        btnAddStudent.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        
        try {
            // Load Add Student FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AddStudent.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent addStudentRoot = loader.load();
            
            // Create new stage for Add Student form
            Stage addStudentStage = new Stage();
            addStudentStage.setTitle("Add New Student - Kinderscape");
            addStudentStage.setScene(new Scene(addStudentRoot));
            addStudentStage.setResizable(true);
            addStudentStage.setWidth(1200);
            addStudentStage.setHeight(750);
            addStudentStage.setResizable(false);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnAddStudent.getScene().getWindow();
            addStudentStage.initOwner(dashboardStage);
            
            // Show Add Student form
            addStudentStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Add Student form");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showViewStudents() {
        resetSubmenuButtonStyles();
        btnViewStudents.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        
        try {
            // Load View Students FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/ViewStudents.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent viewStudentsRoot = loader.load();
            
            // Get the controller to refresh data
            ViewStudentsController controller = loader.getController();
            
            // Create new stage for View Students
            Stage viewStudentsStage = new Stage();
            viewStudentsStage.setTitle("View Students - Kinderscape");
            viewStudentsStage.setScene(new Scene(viewStudentsRoot));
            viewStudentsStage.setWidth(1400);
            viewStudentsStage.setHeight(800);
            viewStudentsStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnViewStudents.getScene().getWindow();
            viewStudentsStage.initOwner(dashboardStage);
            
            // Refresh data when window is shown
            viewStudentsStage.setOnShown(e -> {
                if (controller != null) {
                    controller.refreshData();
                }
            });
            
            // Show View Students window
            viewStudentsStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load View Students page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void toggleReports() {
        reportsExpanded = !reportsExpanded;
        if (reportsSubmenu != null) {
            reportsSubmenu.setVisible(reportsExpanded);
            reportsSubmenu.setManaged(reportsExpanded);
        }
        if (btnReports != null) {
            btnReports.setText("  📈 Reports");
        }
    }
    
    @FXML
    private void showStudentListBySection() {
        resetSubmenuButtonStyles();
        btnStudentListBySection.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        
        try {
            // Load Student List by Section FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/StudentListBySection.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent studentListRoot = loader.load();
            
            // Create new stage for Student List by Section
            Stage studentListStage = new Stage();
            studentListStage.setTitle("Student List by Section - Kinderscape");
            studentListStage.setScene(new Scene(studentListRoot));
            studentListStage.setWidth(1400);
            studentListStage.setHeight(800);
            studentListStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnStudentListBySection.getScene().getWindow();
            studentListStage.initOwner(dashboardStage);
            
            // Show Student List by Section window
            studentListStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Student List by Section page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showTeacherAssignmentReport() {
        resetSubmenuButtonStyles();
        btnTeacherAssignmentReport.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        
        try {
            // Load Teacher Assignment Report FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/TeacherAssignmentReport.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent teacherReportRoot = loader.load();
            
            // Create new stage for Teacher Assignment Report
            Stage teacherReportStage = new Stage();
            teacherReportStage.setTitle("Teacher Assignment Report - Kinderscape");
            teacherReportStage.setScene(new Scene(teacherReportRoot));
            teacherReportStage.setWidth(1200);
            teacherReportStage.setHeight(800);
            teacherReportStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnTeacherAssignmentReport.getScene().getWindow();
            teacherReportStage.initOwner(dashboardStage);
            
            // Show Teacher Assignment Report window
            teacherReportStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Teacher Assignment Report page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showEnrollmentSummary() {
        resetSubmenuButtonStyles();
        btnEnrollmentSummary.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        
        try {
            // Load Enrollment Summary FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/EnrollmentSummary.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent enrollmentSummaryRoot = loader.load();
            
            // Create new stage for Enrollment Summary
            Stage enrollmentSummaryStage = new Stage();
            enrollmentSummaryStage.setTitle("Enrollment Summary - Kinderscape");
            enrollmentSummaryStage.setScene(new Scene(enrollmentSummaryRoot));
            enrollmentSummaryStage.setWidth(1400);
            enrollmentSummaryStage.setHeight(900);
            enrollmentSummaryStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnEnrollmentSummary.getScene().getWindow();
            enrollmentSummaryStage.initOwner(dashboardStage);
            
            // Show Enrollment Summary window
            enrollmentSummaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Enrollment Summary page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showArchiveStudents() {
        resetSubmenuButtonStyles();
        btnArchiveStudents.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        
        try {
            // Load Archive Students FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/ArchiveStudents.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent archiveStudentsRoot = loader.load();
            
            // Create new stage for Archive Students
            Stage archiveStudentsStage = new Stage();
            archiveStudentsStage.setTitle("Archive Students - Kinderscape");
            archiveStudentsStage.setScene(new Scene(archiveStudentsRoot));
            archiveStudentsStage.setWidth(1400);
            archiveStudentsStage.setHeight(800);
            archiveStudentsStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnArchiveStudents.getScene().getWindow();
            archiveStudentsStage.initOwner(dashboardStage);
            
            // Refresh data when window is shown
            archiveStudentsStage.setOnShown(e -> {
                ArchiveStudentsController controller = loader.getController();
                if (controller != null) {
                    controller.refreshData();
                }
            });
            
            // Show Archive Students window
            archiveStudentsStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Archive Students page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void toggleSettings() {
        settingsExpanded = !settingsExpanded;
        if (settingsSubmenu != null) {
            settingsSubmenu.setVisible(settingsExpanded);
            settingsSubmenu.setManaged(settingsExpanded);
        }
        if (btnSettings != null) {
            btnSettings.setText("  🔧 SETTINGS");
        }
    }
    
    @FXML
    private void toggleUserAccounts() {
        userAccountsExpanded = !userAccountsExpanded;
        if (userAccountsSubmenu != null) {
            userAccountsSubmenu.setVisible(userAccountsExpanded);
            userAccountsSubmenu.setManaged(userAccountsExpanded);
        }
        if (btnUserAccounts != null) {
            btnUserAccounts.setText("  👥 User Accounts");
        }
    }
    
    @FXML
    private void showProfile() {
        resetSubmenuButtonStyles();
        if (btnProfile != null) {
            btnProfile.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 10 16; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 200;");
        }
        
        try {
            // Load Profile FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Profile.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent profileRoot = loader.load();
            
            // Get the controller to set user session
            ProfileController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.setUserSession(currentUser, sessionToken);
            }
            
            // Create new stage for Profile
            Stage profileStage = new Stage();
            profileStage.setTitle("User Profile - Kinderscape");
            profileStage.setScene(new Scene(profileRoot));
            profileStage.setWidth(900);
            profileStage.setHeight(700);
            profileStage.setResizable(false);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) (btnProfile != null ? btnProfile.getScene().getWindow() : btnSettings.getScene().getWindow());
            profileStage.initOwner(dashboardStage);
            
            // Show Profile window
            profileStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Profile page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showTeacherAccountManagement() {
        resetSubmenuButtonStyles();
        if (btnTeacherAccountManagement != null) {
            btnTeacherAccountManagement.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 10 16; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 200;");
        }
        
        try {
            // Load Teacher Account Management FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/TeacherAccountManagement.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent teacherAccountManagementRoot = loader.load();
            
            // Create new stage for Teacher Account Management
            Stage teacherAccountManagementStage = new Stage();
            teacherAccountManagementStage.setTitle("Teacher Account Management - Kinderscape");
            teacherAccountManagementStage.setScene(new Scene(teacherAccountManagementRoot));
            teacherAccountManagementStage.setWidth(1200);
            teacherAccountManagementStage.setHeight(800);
            teacherAccountManagementStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) (btnTeacherAccountManagement != null ? btnTeacherAccountManagement.getScene().getWindow() : btnSettings.getScene().getWindow());
            teacherAccountManagementStage.initOwner(dashboardStage);
            
            // Refresh data when window is shown
            teacherAccountManagementStage.setOnShown(e -> {
                TeacherAccountManagementController controller = loader.getController();
                if (controller != null) {
                    controller.refreshData();
                }
            });
            
            // Show Teacher Account Management window
            teacherAccountManagementStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Teacher Account Management page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showSchoolYearManagement() {
        resetSubmenuButtonStyles();
        if (btnSchoolYearManagement != null) {
            btnSchoolYearManagement.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 10 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 210;");
        }
        
        try {
            // Load School Year Management FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/SchoolYearManagement.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent schoolYearManagementRoot = loader.load();
            
            // Create new stage for School Year Management
            Stage schoolYearManagementStage = new Stage();
            schoolYearManagementStage.setTitle("School Year Management - Kinderscape");
            schoolYearManagementStage.setScene(new Scene(schoolYearManagementRoot));
            schoolYearManagementStage.setWidth(1000);
            schoolYearManagementStage.setHeight(700);
            schoolYearManagementStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) (btnSchoolYearManagement != null ? btnSchoolYearManagement.getScene().getWindow() : btnSettings.getScene().getWindow());
            schoolYearManagementStage.initOwner(dashboardStage);
            
            // Show School Year Management window
            schoolYearManagementStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load School Year Management page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showStrandAndSection() {
        resetMainButtonStyles();
        if (btnStrandAndSection != null) {
            btnStrandAndSection.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 20; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 230;");
        }
        
        try {
            // Load Strand and Section Management FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/SectionsManagement.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent sectionsManagementRoot = loader.load();
            
            // Create new stage for Strand and Section Management
            Stage sectionsManagementStage = new Stage();
            sectionsManagementStage.setTitle("Strand and Section Management - Kinderscape");
            sectionsManagementStage.setScene(new Scene(sectionsManagementRoot));
            sectionsManagementStage.setWidth(1400);
            sectionsManagementStage.setHeight(750);
            sectionsManagementStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnStrandAndSection.getScene().getWindow();
            sectionsManagementStage.initOwner(dashboardStage);
            
            // Show Strand and Section Management window
            sectionsManagementStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Strand and Section Management page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void showSubjectManagement() {
        resetMainButtonStyles();
        if (btnSubjectManagement != null) {
            btnSubjectManagement.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 14 20; -fx-alignment: center-left; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 250; -fx-effect: dropshadow(gaussian, rgba(22,160,133,0.4), 8, 0, 0, 0);");
        }
        
        try {
            // Load Subject Management FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/SubjectManagement.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent subjectManagementRoot = loader.load();
            
            // Create new stage for Subject Management
            Stage subjectManagementStage = new Stage();
            subjectManagementStage.setTitle("Subject Management - Kinderscape");
            subjectManagementStage.setScene(new Scene(subjectManagementRoot));
            subjectManagementStage.setWidth(1200);
            subjectManagementStage.setHeight(800);
            subjectManagementStage.setResizable(true);
            
            // Set owner to dashboard stage
            Stage dashboardStage = (Stage) btnSubjectManagement.getScene().getWindow();
            subjectManagementStage.initOwner(dashboardStage);
            
            // Show Subject Management window
            subjectManagementStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load Subject Management page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Invalidate session
                    if (sessionToken != null) {
                        try {
                            authService.logout(sessionToken);
                        } catch (Exception e) {
                            // Log but don't block logout
                            e.printStackTrace();
                        }
                    }
                    
                    // Close dashboard stage
                    Stage dashboardStage = (Stage) logoutButton.getScene().getWindow();
                    dashboardStage.close();
                    
                    // Show login page (either existing hidden one or create new)
                    if (loginStage != null) {
                        loginStage.show();
                        // Clear login fields
                        clearLoginFields();
                    } else {
                        loadLoginPage();
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Logout Error");
                    errorAlert.setHeaderText("Failed to logout");
                    errorAlert.setContentText("Error: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
    }
    
    private void loadLoginPage() {
        try {
            // Load Login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent loginRoot = loader.load();
            
            // Create new stage for login
            Stage newLoginStage = new Stage();
            newLoginStage.setTitle("Kinderscape - Login");
            newLoginStage.setScene(new Scene(loginRoot));
            newLoginStage.setResizable(false);
            newLoginStage.centerOnScreen();
            newLoginStage.setWidth(900);
            newLoginStage.setHeight(600);
            
            // Store reference
            this.loginStage = newLoginStage;
            
            // Show login page
            newLoginStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load login page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void clearLoginFields() {
        // This will be called when showing existing login stage
        // The login controller will handle clearing fields in its initialize or we can access it
        // For now, we'll rely on the login window being reset when shown
    }
    
    private void resetMainButtonStyles() {
        String defaultStyle = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 14 20; -fx-alignment: center-left; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 250; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 2, 2);";
        if (btnDashboard != null) {
            btnDashboard.setStyle(defaultStyle);
        }
        if (btnStudentManagement != null) {
            btnStudentManagement.setStyle(defaultStyle);
        }
        if (btnStrandAndSection != null) {
            btnStrandAndSection.setStyle(defaultStyle);
        }
        if (btnSubjectManagement != null) {
            btnSubjectManagement.setStyle(defaultStyle);
        }
        if (btnReports != null) {
            btnReports.setStyle(defaultStyle);
        }
        if (btnSettings != null) {
            btnSettings.setStyle(defaultStyle);
        }
    }
    
    private void resetSubmenuButtonStyles() {
        String defaultStyle = "-fx-background-color: #3d4f5f; -fx-text-fill: #ecf0f1; -fx-font-size: 13px; -fx-padding: 11 18; -fx-alignment: center-left; -fx-background-radius: 6; -fx-cursor: hand; -fx-pref-width: 225;";
        String nestedSubmenuStyle = "-fx-background-color: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-size: 12px; -fx-padding: 10 16; -fx-alignment: center-left; -fx-background-radius: 5; -fx-cursor: hand; -fx-pref-width: 200;";
        if (btnAddStudent != null) btnAddStudent.setStyle(defaultStyle);
        if (btnViewStudents != null) btnViewStudents.setStyle(defaultStyle);
        if (btnStudentListBySection != null) btnStudentListBySection.setStyle(defaultStyle);
        if (btnTeacherAssignmentReport != null) btnTeacherAssignmentReport.setStyle(defaultStyle);
        if (btnEnrollmentSummary != null) btnEnrollmentSummary.setStyle(defaultStyle);
        if (btnUserAccounts != null) btnUserAccounts.setStyle(defaultStyle);
        if (btnProfile != null) btnProfile.setStyle(nestedSubmenuStyle);
        if (btnTeacherAccountManagement != null) btnTeacherAccountManagement.setStyle(nestedSubmenuStyle);
        if (btnSchoolYearManagement != null) btnSchoolYearManagement.setStyle(defaultStyle);
    }
}
