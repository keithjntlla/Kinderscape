package com.enrollment.system.controller;

import com.enrollment.system.dto.UserDto;
import com.enrollment.system.service.TeacherService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.ResourceBundle;

@Component
public class TeacherAccountManagementController implements Initializable {
    
    @FXML
    private TableView<UserDto> teachersTable;
    
    @FXML
    private TableColumn<UserDto, Long> idColumn;
    
    @FXML
    private TableColumn<UserDto, String> rowNumberColumn;
    
    @FXML
    private TableColumn<UserDto, String> usernameColumn;
    
    @FXML
    private TableColumn<UserDto, String> fullNameColumn;
    
    @FXML
    private TableColumn<UserDto, String> statusColumn;
    
    @FXML
    private TableColumn<UserDto, String> lastLoginColumn;
    
    @FXML
    private TableColumn<UserDto, String> actionsColumn;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button addTeacherButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label teacherCountLabel;
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private ObservableList<UserDto> teacherList;
    private FilteredList<UserDto> filteredList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Row number column
        rowNumberColumn.setCellFactory(column -> new TableCell<UserDto, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    int index = getIndex();
                    setText(String.valueOf(index + 1));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        
        usernameColumn.setCellValueFactory(cellData -> {
            String username = cellData.getValue().getUsername();
            return new SimpleStringProperty(username != null ? username : "");
        });
        
        fullNameColumn.setCellValueFactory(cellData -> {
            String fullName = cellData.getValue().getFullName();
            return new SimpleStringProperty(fullName != null ? fullName : "");
        });
        
        statusColumn.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            return new SimpleStringProperty(isActive != null && isActive ? "Active" : "Inactive");
        });
        
        lastLoginColumn.setCellValueFactory(cellData -> {
            java.time.LocalDateTime lastLogin = cellData.getValue().getLastLogin();
            if (lastLogin != null) {
                return new SimpleStringProperty(lastLogin.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("Never");
        });
        
        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<UserDto, String>() {
            private final Button editButton = new Button("Edit");
            private final Button assignButton = new Button("Assign");
            private final Button deactivateButton = new Button();
            private final HBox hbox = new HBox(5, editButton, assignButton, deactivateButton);
            
            {
                editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                assignButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                
                editButton.setOnAction(e -> {
                    UserDto teacher = getTableView().getItems().get(getIndex());
                    handleEdit(teacher);
                });
                
                assignButton.setOnAction(e -> {
                    UserDto teacher = getTableView().getItems().get(getIndex());
                    handleAssign(teacher);
                });
                
                deactivateButton.setOnAction(e -> {
                    UserDto teacher = getTableView().getItems().get(getIndex());
                    if (teacher.getIsActive() != null && teacher.getIsActive()) {
                        handleDeactivate(teacher);
                    } else {
                        handleActivate(teacher);
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserDto teacher = getTableView().getItems().get(getIndex());
                    Boolean isActive = teacher.getIsActive();
                    
                    // Update button text and style based on account status
                    if (isActive != null && isActive) {
                        deactivateButton.setText("Deactivate");
                        deactivateButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                    } else {
                        deactivateButton.setText("Activate");
                        deactivateButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                    }
                    
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
        
        // Set placeholder
        if (teachersTable != null) {
            Label emptyPlaceholder = new Label("No teachers found.");
            emptyPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            teachersTable.setPlaceholder(emptyPlaceholder);
        }
        
        // Load teachers
        Platform.runLater(() -> {
            loadTeachers();
        });
    }
    
    public void refreshData() {
        loadTeachers();
    }
    
    private void loadTeachers() {
        if (teachersTable != null) {
            Label loadingPlaceholder = new Label("Loading teachers...");
            loadingPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            teachersTable.setPlaceholder(loadingPlaceholder);
        }
        
        Platform.runLater(() -> {
            try {
                java.util.List<UserDto> teachers = teacherService.getAllTeachers();
                
                teacherList = FXCollections.observableArrayList(teachers);
                filteredList = new FilteredList<>(teacherList, p -> true);
                
                SortedList<UserDto> sortedList = new SortedList<>(filteredList);
                sortedList.comparatorProperty().bind(teachersTable.comparatorProperty());
                
                teachersTable.setItems(sortedList);
                
                Label emptyPlaceholder = new Label("No teachers found.");
                emptyPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
                teachersTable.setPlaceholder(emptyPlaceholder);
                
                updateTeacherCount();
                applyFilters(); // Apply any existing search filter
                
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error loading teachers: " + e.getMessage());
                teacherList = FXCollections.observableArrayList();
                filteredList = new FilteredList<>(teacherList, p -> true);
                teachersTable.setItems(new SortedList<>(filteredList));
                updateTeacherCount();
            }
        });
    }
    
    @FXML
    private void handleAddTeacher() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AddEditTeacher.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent addTeacherRoot = loader.load();
            
            AddEditTeacherController controller = loader.getController();
            controller.setMode(AddEditTeacherController.Mode.CREATE);
            
            Stage addTeacherStage = new Stage();
            addTeacherStage.setTitle("Add Teacher Account - Kinderscape");
            addTeacherStage.setScene(new Scene(addTeacherRoot));
            addTeacherStage.setWidth(700);
            addTeacherStage.setHeight(650);
            addTeacherStage.setResizable(true);
            addTeacherStage.setMinWidth(650);
            addTeacherStage.setMinHeight(550);
            
            Stage parentStage = (Stage) addTeacherButton.getScene().getWindow();
            addTeacherStage.initOwner(parentStage);
            
            addTeacherStage.setOnHidden(e -> refreshData());
            
            addTeacherStage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading add teacher form: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadTeachers();
        searchField.clear();
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }
    
    private void applyFilters() {
        if (filteredList == null) {
            return;
        }
        
        String searchText = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        
        filteredList.setPredicate(teacher -> {
            if (teacher == null) {
                return false;
            }
            
            if (searchText.isEmpty()) {
                return true;
            }
            
            // Search in username and full name
            boolean matchesUsername = teacher.getUsername() != null && 
                                     teacher.getUsername().toLowerCase().contains(searchText);
            boolean matchesFullName = teacher.getFullName() != null && 
                                     teacher.getFullName().toLowerCase().contains(searchText);
            
            return matchesUsername || matchesFullName;
        });
        
        updateTeacherCount();
    }
    
    private void updateTeacherCount() {
        if (filteredList != null && teacherCountLabel != null) {
            int count = filteredList.size();
            teacherCountLabel.setText("Total Teachers: " + count);
        }
    }
    
    private void handleEdit(UserDto teacher) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AddEditTeacher.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent editTeacherRoot = loader.load();
            
            AddEditTeacherController controller = loader.getController();
            controller.setMode(AddEditTeacherController.Mode.EDIT);
            controller.setTeacher(teacher);
            
            Stage editTeacherStage = new Stage();
            editTeacherStage.setTitle("Edit Teacher Account - Kinderscape");
            editTeacherStage.setScene(new Scene(editTeacherRoot));
            editTeacherStage.setWidth(700);
            editTeacherStage.setHeight(650);
            editTeacherStage.setResizable(true);
            editTeacherStage.setMinWidth(650);
            editTeacherStage.setMinHeight(550);
            
            Stage parentStage = (Stage) teachersTable.getScene().getWindow();
            editTeacherStage.initOwner(parentStage);
            
            editTeacherStage.setOnHidden(e -> refreshData());
            
            editTeacherStage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading edit form: " + e.getMessage());
        }
    }
    
    private void handleDeactivate(UserDto teacher) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Deactivate Teacher");
        confirmAlert.setHeaderText("Deactivate Teacher Account");
        confirmAlert.setContentText("Are you sure you want to deactivate the teacher account for:\n\n" +
                                   "Username: " + teacher.getUsername() + "\n" +
                                   "Full Name: " + teacher.getFullName() + "\n\n" +
                                   "The teacher will not be able to login until the account is activated again.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        teacherService.deactivateTeacher(teacher.getId());
                        
                        Platform.runLater(() -> {
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText("Teacher Deactivated");
                            successAlert.setContentText("Teacher account has been deactivated successfully. The teacher will not be able to login.");
                            successAlert.showAndWait();
                            
                            refreshData();
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            showError("Error deactivating teacher: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    private void handleActivate(UserDto teacher) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Activate Teacher");
        confirmAlert.setHeaderText("Activate Teacher Account");
        confirmAlert.setContentText("Are you sure you want to activate the teacher account for:\n\n" +
                                   "Username: " + teacher.getUsername() + "\n" +
                                   "Full Name: " + teacher.getFullName() + "\n\n" +
                                   "The teacher will be able to login after activation.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        teacherService.activateTeacher(teacher.getId());
                        
                        Platform.runLater(() -> {
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText("Teacher Activated");
                            successAlert.setContentText("Teacher account has been activated successfully. The teacher can now login.");
                            successAlert.showAndWait();
                            
                            refreshData();
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            showError("Error activating teacher: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    private void handleAssign(UserDto teacher) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/AssignTeacherSubjectsSections.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent assignRoot = loader.load();
            
            AssignTeacherSubjectsSectionsController controller = loader.getController();
            controller.setTeacher(teacher);
            
            Stage assignStage = new Stage();
            assignStage.setTitle("Assign Subjects & Sections - " + teacher.getFullName());
            assignStage.setScene(new Scene(assignRoot));
            assignStage.setWidth(900);
            assignStage.setHeight(700);
            assignStage.setResizable(true);
            assignStage.setMinWidth(800);
            assignStage.setMinHeight(600);
            
            Stage parentStage = (Stage) teachersTable.getScene().getWindow();
            assignStage.initOwner(parentStage);
            
            assignStage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading assignment form: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

