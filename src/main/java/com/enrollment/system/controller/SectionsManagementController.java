package com.enrollment.system.controller;

import com.enrollment.system.model.Section;
import com.enrollment.system.model.Strand;
import com.enrollment.system.service.SectionService;
import com.enrollment.system.service.StrandService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class SectionsManagementController implements Initializable {
    
    // Strands Table
    @FXML
    private TableView<Strand> strandsTable;
    
    @FXML
    private TableColumn<Strand, Long> strandIdColumn;
    
    @FXML
    private TableColumn<Strand, String> strandNameColumn;
    
    @FXML
    private TableColumn<Strand, String> strandDescriptionColumn;
    
    @FXML
    private TableColumn<Strand, String> strandStatusColumn;
    
    @FXML
    private TableColumn<Strand, String> strandActionsColumn;
    
    // Sections Table
    @FXML
    private TableView<Section> sectionsTable;
    
    @FXML
    private TableColumn<Section, Long> sectionIdColumn;
    
    @FXML
    private TableColumn<Section, String> sectionNameColumn;
    
    @FXML
    private TableColumn<Section, String> sectionStrandColumn;
    
    @FXML
    private TableColumn<Section, Integer> sectionGradeLevelColumn;
    
    @FXML
    private TableColumn<Section, Integer> sectionCapacityColumn;
    
    @FXML
    private TableColumn<Section, String> sectionStatusColumn;
    
    @FXML
    private TableColumn<Section, String> sectionActionsColumn;
    
    // Buttons
    @FXML
    private Button addStrandButton;
    
    @FXML
    private Button addSectionButton;
    
    @FXML
    private Button refreshButton;
    
    // Labels
    @FXML
    private Label strandCountLabel;
    
    @FXML
    private Label sectionCountLabel;
    
    
    // Filter fields
    @FXML
    private TextField strandFilterField;
    
    @FXML
    private ComboBox<String> strandStatusFilter;
    
    @FXML
    private TextField sectionFilterField;
    
    @FXML
    private ComboBox<String> sectionStrandFilter;
    
    @FXML
    private ComboBox<String> sectionGradeFilter;
    
    @FXML
    private ComboBox<String> sectionStatusFilter;
    
    @Autowired
    private SectionService sectionService;
    
    @Autowired
    private StrandService strandService;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    private ObservableList<Strand> strandList;
    private FilteredList<Strand> filteredStrandList;
    
    private ObservableList<Section> sectionList;
    private FilteredList<Section> filteredSectionList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStrandsTable();
        setupSectionsTable();
        setupFilterControls();
        setupFilters();
        
        Platform.runLater(() -> {
            loadStrands();
            loadSections();
        });
    }
    
    private void setupFilterControls() {
        // Setup strand status filter
        if (strandStatusFilter != null) {
            strandStatusFilter.getItems().addAll("All", "Active", "Inactive");
            strandStatusFilter.setValue("All");
        }
        
        // Setup section status filter
        if (sectionStatusFilter != null) {
            sectionStatusFilter.getItems().addAll("All", "Active", "Inactive");
            sectionStatusFilter.setValue("All");
        }
        
        // Setup section grade filter
        if (sectionGradeFilter != null) {
            sectionGradeFilter.getItems().addAll("All Grades", "11", "12");
            sectionGradeFilter.setValue("All Grades");
        }
        
        // Setup section strand filter - will be populated when strands are loaded
        if (sectionStrandFilter != null) {
            sectionStrandFilter.getItems().add("All Strands");
        }
    }
    
    private void setupFilters() {
        // Setup strand filters
        if (strandFilterField != null) {
            strandFilterField.textProperty().addListener((observable, oldValue, newValue) -> applyStrandFilters());
        }
        if (strandStatusFilter != null) {
            strandStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyStrandFilters());
        }
        
        // Setup section filters
        if (sectionFilterField != null) {
            sectionFilterField.textProperty().addListener((observable, oldValue, newValue) -> applySectionFilters());
        }
        if (sectionStrandFilter != null) {
            sectionStrandFilter.valueProperty().addListener((observable, oldValue, newValue) -> applySectionFilters());
        }
        if (sectionGradeFilter != null) {
            sectionGradeFilter.valueProperty().addListener((observable, oldValue, newValue) -> applySectionFilters());
        }
        if (sectionStatusFilter != null) {
            sectionStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applySectionFilters());
        }
    }
    
    private void applyStrandFilters() {
        if (filteredStrandList == null) return;
        
        filteredStrandList.setPredicate(strand -> {
            // Text filter
            String searchText = strandFilterField != null ? strandFilterField.getText() : "";
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                boolean matchesText = (strand.getName() != null && strand.getName().toLowerCase().contains(lowerCaseFilter)) ||
                                     (strand.getDescription() != null && strand.getDescription().toLowerCase().contains(lowerCaseFilter));
                if (!matchesText) return false;
            }
            
            // Status filter
            String statusFilter = strandStatusFilter != null ? strandStatusFilter.getValue() : "All";
            if (statusFilter != null && !"All".equals(statusFilter)) {
                Boolean isActive = strand.getIsActive();
                boolean isActiveValue = (isActive != null && isActive);
                if ("Active".equals(statusFilter) && !isActiveValue) return false;
                if ("Inactive".equals(statusFilter) && isActiveValue) return false;
            }
            
            return true;
        });
        updateStrandCount();
    }
    
    private void applySectionFilters() {
        if (filteredSectionList == null) return;
        
        filteredSectionList.setPredicate(section -> {
            // Text filter
            String searchText = sectionFilterField != null ? sectionFilterField.getText() : "";
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                boolean matchesText = (section.getName() != null && section.getName().toLowerCase().contains(lowerCaseFilter)) ||
                                     (section.getStrand() != null && section.getStrand().toLowerCase().contains(lowerCaseFilter)) ||
                                     (section.getGradeLevel() != null && section.getGradeLevel().toString().contains(lowerCaseFilter));
                if (!matchesText) return false;
            }
            
            // Strand filter
            String strandFilter = sectionStrandFilter != null ? sectionStrandFilter.getValue() : "All Strands";
            if (strandFilter != null && !"All Strands".equals(strandFilter)) {
                if (section.getStrand() == null || !section.getStrand().equals(strandFilter)) {
                    return false;
                }
            }
            
            // Grade filter
            String gradeFilter = sectionGradeFilter != null ? sectionGradeFilter.getValue() : "All Grades";
            if (gradeFilter != null && !"All Grades".equals(gradeFilter)) {
                Integer sectionGrade = section.getGradeLevel();
                if (sectionGrade == null || !gradeFilter.equals(String.valueOf(sectionGrade))) {
                    return false;
                }
            }
            
            // Status filter
            String statusFilter = sectionStatusFilter != null ? sectionStatusFilter.getValue() : "All";
            if (statusFilter != null && !"All".equals(statusFilter)) {
                Boolean isActive = section.getIsActive();
                boolean isActiveValue = (isActive != null && isActive);
                if ("Active".equals(statusFilter) && !isActiveValue) return false;
                if ("Inactive".equals(statusFilter) && isActiveValue) return false;
            }
            
            return true;
        });
        updateSectionCount();
    }
    
    private void setupStrandsTable() {
        strandIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        strandNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        strandDescriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            return new javafx.beans.property.SimpleStringProperty(desc != null ? desc : "");
        });
        strandStatusColumn.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            String status = (isActive != null && isActive) ? "Active" : "Inactive";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        strandStatusColumn.setCellFactory(column -> new TableCell<Strand, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        strandActionsColumn.setCellFactory(column -> new TableCell<Strand, String>() {
            private final Button editButton = new Button("Edit");
            private final Button deactivateButton = new Button("Deactivate");
            private final Button activateButton = new Button("Activate");
            private final Button deletePermanentlyButton = new Button("Delete Permanently");
            private final HBox hbox = new HBox(3);
            
            {
                editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
                deactivateButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
                activateButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
                deletePermanentlyButton.setStyle("-fx-background-color: #8b0000; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 4 6; -fx-background-radius: 3; -fx-cursor: hand;");
                
                editButton.setOnAction(e -> {
                    Strand strand = getTableView().getItems().get(getIndex());
                    handleEditStrand(strand);
                });
                
                deactivateButton.setOnAction(e -> {
                    Strand strand = getTableView().getItems().get(getIndex());
                    handleDeleteStrand(strand);
                });
                
                activateButton.setOnAction(e -> {
                    Strand strand = getTableView().getItems().get(getIndex());
                    handleActivateStrand(strand);
                });
                
                deletePermanentlyButton.setOnAction(e -> {
                    Strand strand = getTableView().getItems().get(getIndex());
                    handleDeletePermanentlyStrand(strand);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Strand strand = getTableView().getItems().get(getIndex());
                    hbox.getChildren().clear();
                    hbox.getChildren().add(editButton);
                    
                    Boolean isActive = strand.getIsActive();
                    if (isActive != null && isActive) {
                        hbox.getChildren().add(deactivateButton);
                    } else {
                        hbox.getChildren().add(activateButton);
                        hbox.getChildren().add(deletePermanentlyButton);
                    }
                    
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }
    
    private void setupSectionsTable() {
        sectionIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        sectionNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        sectionStrandColumn.setCellValueFactory(new PropertyValueFactory<>("strand"));
        sectionGradeLevelColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevel"));
        sectionCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        sectionStatusColumn.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            String status = (isActive != null && isActive) ? "Active" : "Inactive";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        sectionStatusColumn.setCellFactory(column -> new TableCell<Section, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equals(status)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        sectionActionsColumn.setCellFactory(column -> new TableCell<Section, String>() {
            private final Button editButton = new Button("Edit");
            private final Button deactivateButton = new Button("Deactivate");
            private final Button activateButton = new Button("Activate");
            private final Button deletePermanentlyButton = new Button("Delete Permanently");
            private final HBox hbox = new HBox(3);
            
            {
                editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
                deactivateButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
                activateButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
                deletePermanentlyButton.setStyle("-fx-background-color: #8b0000; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 4 6; -fx-background-radius: 3; -fx-cursor: hand;");
                
                editButton.setOnAction(e -> {
                    Section section = getTableView().getItems().get(getIndex());
                    handleEditSection(section);
                });
                
                deactivateButton.setOnAction(e -> {
                    Section section = getTableView().getItems().get(getIndex());
                    handleDeleteSection(section);
                });
                
                activateButton.setOnAction(e -> {
                    Section section = getTableView().getItems().get(getIndex());
                    handleActivateSection(section);
                });
                
                deletePermanentlyButton.setOnAction(e -> {
                    Section section = getTableView().getItems().get(getIndex());
                    handleDeletePermanentlySection(section);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Section section = getTableView().getItems().get(getIndex());
                    hbox.getChildren().clear();
                    hbox.getChildren().add(editButton);
                    
                    Boolean isActive = section.getIsActive();
                    if (isActive != null && isActive) {
                        hbox.getChildren().add(deactivateButton);
                    } else {
                        hbox.getChildren().add(activateButton);
                        hbox.getChildren().add(deletePermanentlyButton);
                    }
                    
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }
    
    public void refreshData() {
        loadStrands();
        loadSections();
    }
    
    private void loadStrands() {
        // Run database operation in background thread
        new Thread(() -> {
            try {
                // Load all strands (both active and inactive) for management - this is a blocking DB call
                java.util.List<Strand> strands = strandService.getAllStrands();
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    try {
                        strandList = FXCollections.observableArrayList(strands);
                        
                        filteredStrandList = new FilteredList<>(strandList, p -> true);
                        SortedList<Strand> sortedList = new SortedList<>(filteredStrandList);
                        sortedList.comparatorProperty().bind(strandsTable.comparatorProperty());
                        
                        strandsTable.setItems(sortedList);
                        applyStrandFilters();
                        
                        // Update section strand filter
                        if (sectionStrandFilter != null) {
                            String currentValue = sectionStrandFilter.getValue();
                            sectionStrandFilter.getItems().clear();
                            sectionStrandFilter.getItems().add("All Strands");
                            for (Strand strand : strands) {
                                sectionStrandFilter.getItems().add(strand.getName());
                            }
                            if (currentValue != null && sectionStrandFilter.getItems().contains(currentValue)) {
                                sectionStrandFilter.setValue(currentValue);
                            } else {
                                sectionStrandFilter.setValue("All Strands");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error updating strands UI: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error loading strands: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void loadSections() {
        // Run database operation in background thread
        new Thread(() -> {
            try {
                // Load all sections (both active and inactive) for management - this is a blocking DB call
                java.util.List<Section> sections = sectionService.getAllSections();
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    try {
                        sectionList = FXCollections.observableArrayList(sections);
                        
                        filteredSectionList = new FilteredList<>(sectionList, p -> true);
                        SortedList<Section> sortedList = new SortedList<>(filteredSectionList);
                        sortedList.comparatorProperty().bind(sectionsTable.comparatorProperty());
                        
                        sectionsTable.setItems(sortedList);
                        applySectionFilters();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error updating sections UI: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error loading sections: " + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void handleRefresh() {
        loadStrands();
        loadSections();
    }
    
    @FXML
    private void handleAddStrand() {
        showStrandDialog(null);
    }
    
    private void handleEditStrand(Strand strand) {
        showStrandDialog(strand);
    }
    
    private void handleDeleteStrand(Strand strand) {
        // Check if strand has active students - do this check FIRST before showing confirmation
        boolean hasStudents = false;
        int studentCount = 0;
        try {
            hasStudents = strandService.hasActiveStudents(strand.getName());
            if (hasStudents) {
                // Get the actual count for better error message
                try {
                    java.util.List<com.enrollment.system.model.Student> students = 
                        strandService.getEnrolledStudentsByStrand(strand.getName());
                    studentCount = students.size();
                } catch (Exception e) {
                    // If we can't get count, just use the boolean check
                    studentCount = 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error checking strand enrollment: " + e.getMessage());
            return;
        }
        
        // If students exist, show error immediately and don't proceed
        if (hasStudents) {
            showError("Cannot deactivate strand " + strand.getName() + 
                    " because it has " + studentCount + " enrolled student(s). " +
                    "Please reassign or archive these students first.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Deactivate Strand");
        confirmAlert.setHeaderText("Deactivate Strand: " + strand.getName());
        confirmAlert.setContentText("Are you sure you want to deactivate this strand? This will set it as inactive.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Double-check again right before deactivation (defensive programming)
                    if (strandService.hasActiveStudents(strand.getName())) {
                        showError("Cannot deactivate strand " + strand.getName() + 
                                " because it has enrolled students. The strand status may have changed.");
                        return;
                    }
                    strandService.deleteStrand(strand.getId());
                    showSuccess("Strand " + strand.getName() + " has been deactivated successfully.");
                    refreshData();
                } catch (IllegalStateException e) {
                    showError(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error deactivating strand: " + e.getMessage());
                }
            }
        });
    }
    
    private void showStrandDialog(Strand strand) {
        if (strandService == null) {
            showError("Strand service is not available. Please refresh the page.");
            return;
        }
        
        Dialog<Strand> dialog = new Dialog<>();
        dialog.setTitle(strand == null ? "Add Strand" : "Edit Strand");
        dialog.setHeaderText(strand == null ? "Add New Strand" : "Edit Strand: " + strand.getName());
        
        // Set dialog owner and modality
        Window ownerWindow = null;
        if (strandsTable != null && strandsTable.getScene() != null) {
            ownerWindow = strandsTable.getScene().getWindow();
        }
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }
        
        TextField nameField = new TextField();
        nameField.setPromptText("Strand Name (e.g., ABM, HUMSS, STEM)");
        
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description (optional)");
        
        if (strand != null) {
            nameField.setText(strand.getName());
            if (strand.getDescription() != null) {
                descriptionField.setText(strand.getDescription());
            }
        }
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Strand Name *:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        // Request focus on name field when dialog is shown
        dialog.setOnShown(e -> nameField.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Strand result = strand != null ? strand : new Strand();
                result.setName(nameField.getText().trim());
                result.setDescription(descriptionField.getText().trim().isEmpty() ? null : descriptionField.getText().trim());
                if (strand == null) {
                    result.setIsActive(true);
                }
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            // Run database operation in background thread to prevent UI blocking
            new Thread(() -> {
                try {
                    if (strandService == null) {
                        Platform.runLater(() -> {
                            showError("Strand service is not available. Please refresh the page.");
                        });
                        return;
                    }
                    
                    // Use TransactionTemplate to ensure proper transaction management in background thread
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                    transactionTemplate.setTimeout(30); // 30 seconds timeout
                    
                    // Retry logic for SQLite busy errors
                    int maxRetries = 3;
                    int retryDelay = 100; // milliseconds
                    Exception lastException = null;
                    
                    for (int attempt = 0; attempt < maxRetries; attempt++) {
                        try {
                            if (strand == null) {
                                transactionTemplate.execute(status -> {
                                    strandService.createStrand(result.getName(), result.getDescription());
                                    return null;
                                });
                                Platform.runLater(() -> {
                                    showSuccess("Strand " + result.getName() + " has been created successfully.");
                                    refreshData();
                                });
                                return; // Success, exit retry loop
                            } else {
                                transactionTemplate.execute(status -> {
                                    strandService.updateStrand(strand.getId(), result.getName(), result.getDescription());
                                    return null;
                                });
                                Platform.runLater(() -> {
                                    showSuccess("Strand " + result.getName() + " has been updated successfully.");
                                    refreshData();
                                });
                                return; // Success, exit retry loop
                            }
                        } catch (DataAccessException e) {
                            lastException = e;
                            String errorMessage = e.getMessage();
                            // Check if it's a SQLite busy error
                            if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                                errorMessage.contains("database is locked") || 
                                errorMessage.contains("database locked"))) {
                                if (attempt < maxRetries - 1) {
                                    // Wait before retry with exponential backoff
                                    try {
                                        Thread.sleep(retryDelay * (attempt + 1));
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Platform.runLater(() -> {
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
                    
                    // If we get here, all retries failed
                    throw lastException != null ? lastException : new RuntimeException("Failed after " + maxRetries + " attempts");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("SQLITE_BUSY")) {
                            showError("Database is busy. Please wait a moment and try again.");
                        } else {
                            showError("Error saving strand: " + errorMsg);
                        }
                    });
                }
            }).start();
        });
    }
    
    @FXML
    private void handleAddSection() {
        showSectionDialog(null);
    }
    
    private void handleEditSection(Section section) {
        showSectionDialog(section);
    }
    
    private void handleDeleteSection(Section section) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Deactivate Section");
        confirmAlert.setHeaderText("Deactivate Section: " + section.getName());
        confirmAlert.setContentText("Are you sure you want to deactivate this section? This will set it as inactive.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sectionService.deleteSection(section.getId());
                    showSuccess("Section " + section.getName() + " has been deactivated successfully.");
                    refreshData();
                } catch (IllegalStateException e) {
                    showError(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error deactivating section: " + e.getMessage());
                }
            }
        });
    }
    
    private void showSectionDialog(Section section) {
        if (sectionService == null || strandService == null) {
            showError("Services are not available. Please refresh the page.");
            return;
        }
        
        Dialog<Section> dialog = new Dialog<>();
        dialog.setTitle(section == null ? "Add Section" : "Edit Section");
        dialog.setHeaderText(section == null ? "Add New Section" : "Edit Section: " + section.getName());
        
        // Set dialog owner and modality
        Window ownerWindow = null;
        if (sectionsTable != null && sectionsTable.getScene() != null) {
            ownerWindow = sectionsTable.getScene().getWindow();
        }
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
            dialog.initModality(Modality.WINDOW_MODAL);
        } else {
            dialog.initModality(Modality.APPLICATION_MODAL);
        }
        
        TextField nameField = new TextField();
        nameField.setPromptText("Section Name (e.g., ABM-11A)");
        
        ComboBox<String> strandComboBox = new ComboBox<>();
        try {
            // When editing, show all strands; when adding, show only active strands
            java.util.List<Strand> strands = (section == null) ? 
                strandService.getActiveStrands() : strandService.getAllStrands();
            for (Strand strand : strands) {
                strandComboBox.getItems().add(strand.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading strands: " + e.getMessage());
            return;
        }
        strandComboBox.setPromptText("Select Strand");
        
        ComboBox<Integer> gradeLevelComboBox = new ComboBox<>();
        gradeLevelComboBox.getItems().addAll(11, 12);
        gradeLevelComboBox.setPromptText("Select Grade Level");
        
        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity (optional)");
        
        if (section != null) {
            nameField.setText(section.getName());
            strandComboBox.setValue(section.getStrand());
            gradeLevelComboBox.setValue(section.getGradeLevel());
            if (section.getCapacity() != null) {
                capacityField.setText(String.valueOf(section.getCapacity()));
            }
        }
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Section Name *:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Strand *:"), 0, 1);
        grid.add(strandComboBox, 1, 1);
        grid.add(new Label("Grade Level *:"), 0, 2);
        grid.add(gradeLevelComboBox, 1, 2);
        grid.add(new Label("Capacity:"), 0, 3);
        grid.add(capacityField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue == null || newValue.trim().isEmpty() || 
                    strandComboBox.getValue() == null || 
                    gradeLevelComboBox.getValue() == null);
        });
        
        strandComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(nameField.getText() == null || nameField.getText().trim().isEmpty() || 
                    newValue == null || 
                    gradeLevelComboBox.getValue() == null);
        });
        
        gradeLevelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(nameField.getText() == null || nameField.getText().trim().isEmpty() || 
                    strandComboBox.getValue() == null || 
                    newValue == null);
        });
        
        // Request focus on name field when dialog is shown
        dialog.setOnShown(e -> nameField.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Section result = section != null ? section : new Section();
                result.setName(nameField.getText().trim());
                result.setStrand(strandComboBox.getValue());
                result.setGradeLevel(gradeLevelComboBox.getValue());
                
                if (!capacityField.getText().trim().isEmpty()) {
                    try {
                        result.setCapacity(Integer.parseInt(capacityField.getText().trim()));
                    } catch (NumberFormatException e) {
                        // Invalid capacity, leave as null
                    }
                }
                
                if (section == null) {
                    result.setIsActive(true);
                }
                
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            // Run database operation in background thread to prevent UI blocking
            new Thread(() -> {
                try {
                    if (sectionService == null) {
                        Platform.runLater(() -> {
                            showError("Section service is not available. Please refresh the page.");
                        });
                        return;
                    }
                    
                    // Use TransactionTemplate to ensure proper transaction management in background thread
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                    transactionTemplate.setTimeout(30); // 30 seconds timeout
                    
                    // Retry logic for SQLite busy errors
                    int maxRetries = 3;
                    int retryDelay = 100; // milliseconds
                    Exception lastException = null;
                    
                    for (int attempt = 0; attempt < maxRetries; attempt++) {
                        try {
                            if (section == null) {
                                transactionTemplate.execute(status -> {
                                    sectionService.createSection(result.getName(), result.getStrand(), result.getGradeLevel(), result.getCapacity());
                                    return null;
                                });
                                Platform.runLater(() -> {
                                    showSuccess("Section " + result.getName() + " has been created successfully.");
                                    refreshData();
                                });
                                return; // Success, exit retry loop
                            } else {
                                transactionTemplate.execute(status -> {
                                    sectionService.updateSection(section.getId(), result.getName(), 
                                        result.getStrand(), result.getGradeLevel(), result.getCapacity());
                                    return null;
                                });
                                Platform.runLater(() -> {
                                    showSuccess("Section " + result.getName() + " has been updated successfully.");
                                    refreshData();
                                });
                                return; // Success, exit retry loop
                            }
                        } catch (DataAccessException e) {
                            lastException = e;
                            String errorMessage = e.getMessage();
                            // Check if it's a SQLite busy error
                            if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                                errorMessage.contains("database is locked") || 
                                errorMessage.contains("database locked"))) {
                                if (attempt < maxRetries - 1) {
                                    // Wait before retry with exponential backoff
                                    try {
                                        Thread.sleep(retryDelay * (attempt + 1));
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Platform.runLater(() -> {
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
                    
                    // If we get here, all retries failed
                    throw lastException != null ? lastException : new RuntimeException("Failed after " + maxRetries + " attempts");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("SQLITE_BUSY")) {
                            showError("Database is busy. Please wait a moment and try again.");
                        } else {
                            showError("Error saving section: " + errorMsg);
                        }
                    });
                }
            }).start();
        });
    }
    
    private void updateStrandCount() {
        if (filteredStrandList != null && strandCountLabel != null) {
            int count = filteredStrandList.size();
            strandCountLabel.setText("Total Strands: " + count);
        }
    }
    
    private void updateSectionCount() {
        if (filteredSectionList != null && sectionCountLabel != null) {
            int count = filteredSectionList.size();
            sectionCountLabel.setText("Total Sections: " + count);
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void handleActivateStrand(Strand strand) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Activate Strand");
        confirmAlert.setHeaderText("Activate Strand: " + strand.getName());
        confirmAlert.setContentText("Are you sure you want to activate this strand?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    strandService.activateStrand(strand.getId());
                    showSuccess("Strand " + strand.getName() + " has been activated successfully.");
                    refreshData();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error activating strand: " + e.getMessage());
                }
            }
        });
    }
    
    private void handleActivateSection(Section section) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Activate Section");
        confirmAlert.setHeaderText("Activate Section: " + section.getName());
        confirmAlert.setContentText("Are you sure you want to activate this section?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sectionService.activateSection(section.getId());
                    showSuccess("Section " + section.getName() + " has been activated successfully.");
                    refreshData();
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error activating section: " + e.getMessage());
                }
            }
        });
    }
    
    private void handleDeletePermanentlyStrand(Strand strand) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Permanently");
        confirmAlert.setHeaderText("Delete Permanently: " + strand.getName());
        confirmAlert.setContentText("WARNING: This action cannot be undone!\n\n" +
                "This will permanently delete the strand from the database.\n\n" +
                "Are you absolutely sure you want to proceed?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Run database operation in background thread
                new Thread(() -> {
                    try {
                        // Use TransactionTemplate to ensure proper transaction management
                        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                        transactionTemplate.setTimeout(30);
                        
                        // Retry logic for SQLite busy errors
                        int maxRetries = 3;
                        int retryDelay = 100;
                        Exception lastException = null;
                        
                        for (int attempt = 0; attempt < maxRetries; attempt++) {
                            try {
                                transactionTemplate.execute(status -> {
                                    strandService.deletePermanently(strand.getId());
                                    return null;
                                });
                                Platform.runLater(() -> {
                                    showSuccess("Strand " + strand.getName() + " has been permanently deleted.");
                                    refreshData();
                                });
                                return; // Success
                            } catch (DataAccessException e) {
                                lastException = e;
                                String errorMessage = e.getMessage();
                                if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                                    errorMessage.contains("database is locked") || 
                                    errorMessage.contains("database locked"))) {
                                    if (attempt < maxRetries - 1) {
                                        try {
                                            Thread.sleep(retryDelay * (attempt + 1));
                                        } catch (InterruptedException ie) {
                                            Thread.currentThread().interrupt();
                                            Platform.runLater(() -> {
                                                showError("Operation was interrupted. Please try again.");
                                            });
                                            return;
                                        }
                                        continue;
                                    }
                                }
                                throw e;
                            }
                        }
                        
                        throw lastException != null ? lastException : new RuntimeException("Failed after " + maxRetries + " attempts");
                        
                    } catch (IllegalStateException e) {
                        // This is the validation error for enrolled students
                        Platform.runLater(() -> {
                            showError(e.getMessage());
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            String errorMsg = e.getMessage();
                            if (errorMsg != null && errorMsg.contains("SQLITE_BUSY")) {
                                showError("Database is busy. Please wait a moment and try again.");
                            } else {
                                showError("Error deleting strand permanently: " + errorMsg);
                            }
                        });
                    }
                }).start();
            }
        });
    }
    
    private void handleDeletePermanentlySection(Section section) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Permanently");
        confirmAlert.setHeaderText("Delete Permanently: " + section.getName());
        confirmAlert.setContentText("WARNING: This action cannot be undone!\n\n" +
                "This will permanently delete the section from the database.\n\n" +
                "Are you absolutely sure you want to proceed?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Run database operation in background thread
                new Thread(() -> {
                    try {
                        // Use TransactionTemplate to ensure proper transaction management
                        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                        transactionTemplate.setTimeout(30);
                        
                        // Retry logic for SQLite busy errors
                        int maxRetries = 3;
                        int retryDelay = 100;
                        Exception lastException = null;
                        
                        for (int attempt = 0; attempt < maxRetries; attempt++) {
                            try {
                                transactionTemplate.execute(status -> {
                                    sectionService.deletePermanently(section.getId());
                                    return null;
                                });
                                Platform.runLater(() -> {
                                    showSuccess("Section " + section.getName() + " has been permanently deleted.");
                                    refreshData();
                                });
                                return; // Success
                            } catch (DataAccessException e) {
                                lastException = e;
                                String errorMessage = e.getMessage();
                                if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                                    errorMessage.contains("database is locked") || 
                                    errorMessage.contains("database locked"))) {
                                    if (attempt < maxRetries - 1) {
                                        try {
                                            Thread.sleep(retryDelay * (attempt + 1));
                                        } catch (InterruptedException ie) {
                                            Thread.currentThread().interrupt();
                                            Platform.runLater(() -> {
                                                showError("Operation was interrupted. Please try again.");
                                            });
                                            return;
                                        }
                                        continue;
                                    }
                                }
                                throw e;
                            }
                        }
                        
                        throw lastException != null ? lastException : new RuntimeException("Failed after " + maxRetries + " attempts");
                        
                    } catch (IllegalStateException e) {
                        // This is the validation error for enrolled students
                        Platform.runLater(() -> {
                            showError(e.getMessage());
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            String errorMsg = e.getMessage();
                            if (errorMsg != null && errorMsg.contains("SQLITE_BUSY")) {
                                showError("Database is busy. Please wait a moment and try again.");
                            } else {
                                showError("Error deleting section permanently: " + errorMsg);
                            }
                        });
                    }
                }).start();
            }
        });
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
