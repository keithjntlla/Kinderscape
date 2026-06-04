package com.enrollment.system.controller;

import com.enrollment.system.dto.SchoolYearDto;
import com.enrollment.system.service.SchoolYearService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

@Component
public class SchoolYearManagementController implements Initializable {
    
    @FXML
    private TableView<SchoolYearDto> schoolYearsTable;
    
    @FXML
    private TableColumn<SchoolYearDto, String> yearColumn;
    
    @FXML
    private TableColumn<SchoolYearDto, String> statusColumn;
    
    @FXML
    private TableColumn<SchoolYearDto, String> actionsColumn;
    
    @FXML
    private Button addSchoolYearButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label schoolYearCountLabel;
    
    @Autowired
    private SchoolYearService schoolYearService;
    
    private ObservableList<SchoolYearDto> schoolYearList;
    private FilteredList<SchoolYearDto> filteredList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadSchoolYears();
    }
    
    private void setupTableColumns() {
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        statusColumn.setCellValueFactory(cellData -> {
            Boolean isCurrent = cellData.getValue().getIsCurrent();
            return new javafx.beans.property.SimpleStringProperty(
                Boolean.TRUE.equals(isCurrent) ? "Current" : "Inactive");
        });
        
        // Actions column with buttons
        actionsColumn.setCellFactory(param -> new TableCell<SchoolYearDto, String>() {
            private final Button setCurrentButton = new Button("Set as Current");
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    SchoolYearDto schoolYear = getTableView().getItems().get(getIndex());
                    
                    HBox buttons = new HBox(5);
                    buttons.setPadding(new Insets(5));
                    
                    if (Boolean.TRUE.equals(schoolYear.getIsCurrent())) {
                        setCurrentButton.setDisable(true);
                        setCurrentButton.setText("Current");
                        setCurrentButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                    } else {
                        setCurrentButton.setDisable(false);
                        setCurrentButton.setText("Set as Current");
                        setCurrentButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        setCurrentButton.setOnAction(e -> handleSetCurrent(schoolYear));
                    }
                    
                    // Delete button
                    Button deleteButton = new Button("Delete");
                    deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    deleteButton.setOnAction(e -> handleDelete(schoolYear));
                    
                    buttons.getChildren().addAll(setCurrentButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void loadSchoolYears() {
        new Thread(() -> {
            try {
                java.util.List<SchoolYearDto> schoolYears = schoolYearService.getAllSchoolYears();
                
                Platform.runLater(() -> {
                    try {
                        schoolYearList = FXCollections.observableArrayList(schoolYears);
                        filteredList = new FilteredList<>(schoolYearList, p -> true);
                        SortedList<SchoolYearDto> sortedList = new SortedList<>(filteredList);
                        sortedList.comparatorProperty().bind(schoolYearsTable.comparatorProperty());
                        
                        schoolYearsTable.setItems(sortedList);
                        updateCount();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error displaying school years: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error loading school years: " + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void handleAddSchoolYear() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add New School Year");
        dialog.setHeaderText("Enter school year");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField yearField = new TextField();
        yearField.setPromptText("e.g., 2026-2027");
        
        grid.add(new Label("School Year:"), 0, 0);
        grid.add(yearField, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(() -> yearField.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return yearField.getText().trim();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(year -> {
            try {
                // Auto-generate dates based on year
                // Parse year (e.g., "2025-2026" -> start: June 2025, end: March 2026)
                String[] years = year.split("-");
                if (years.length == 2) {
                    int startYear = Integer.parseInt(years[0].trim());
                    int endYear = Integer.parseInt(years[1].trim());
                    
                    LocalDate startDate = LocalDate.of(startYear, 6, 1); // June 1
                    LocalDate endDate = LocalDate.of(endYear, 3, 31); // March 31
                    
                    schoolYearService.createSchoolYear(year, startDate, endDate);
                    showSuccess("School year created successfully!");
                    loadSchoolYears();
                } else {
                    showError("Invalid year format. Please use format: YYYY-YYYY (e.g., 2026-2027)");
                }
            } catch (NumberFormatException e) {
                showError("Invalid year format. Please use format: YYYY-YYYY (e.g., 2026-2027)");
            } catch (Exception e) {
                showError("Error creating school year: " + e.getMessage());
            }
        });
    }
    
    @FXML
    private void handleSetCurrent(SchoolYearDto schoolYear) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Set Current School Year");
        confirmAlert.setHeaderText("Set " + schoolYear.getYear() + " as current?");
        confirmAlert.setContentText("This will unset the current school year and set " + schoolYear.getYear() + " as the new current school year.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    schoolYearService.setCurrentSchoolYear(schoolYear.getId());
                    showSuccess("School year " + schoolYear.getYear() + " is now set as current.");
                    loadSchoolYears();
                } catch (Exception e) {
                    showError("Error setting current school year: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleDelete(SchoolYearDto schoolYear) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete School Year");
        confirmAlert.setHeaderText("Delete " + schoolYear.getYear() + "?");
        confirmAlert.setContentText("This action cannot be undone. School year can only be deleted if it has less than 2 students.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        schoolYearService.deleteSchoolYear(schoolYear.getId());
                        Platform.runLater(() -> {
                            showSuccess("School year " + schoolYear.getYear() + " has been deleted successfully.");
                            loadSchoolYears();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showError("Error deleting school year: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    @FXML
    private void handleRefresh() {
        loadSchoolYears();
    }
    
    private void updateCount() {
        if (schoolYearList != null && schoolYearCountLabel != null) {
            schoolYearCountLabel.setText("Total School Years: " + schoolYearList.size());
        }
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

