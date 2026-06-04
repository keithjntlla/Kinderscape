package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.service.StudentService;
import com.enrollment.system.service.SemesterService;
import com.enrollment.system.dto.SemesterDto;
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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class ViewStudentsController implements Initializable {
    
    @FXML
    private TableView<StudentDto> studentsTable;
    
    @FXML
    private TableColumn<StudentDto, Long> idColumn;
    
    @FXML
    private TableColumn<StudentDto, String> rowNumberColumn;
    
    @FXML
    private TableColumn<StudentDto, String> nameColumn;
    
    @FXML
    private TableColumn<StudentDto, Integer> ageColumn;
    
    @FXML
    private TableColumn<StudentDto, String> sexColumn;
    
    @FXML
    private TableColumn<StudentDto, Integer> gradeLevelColumn;
    
    @FXML
    private TableColumn<StudentDto, String> strandColumn;
    
    @FXML
    private TableColumn<StudentDto, String> sectionColumn;
    
    @FXML
    private TableColumn<StudentDto, String> semesterColumn;
    
    @FXML
    private TableColumn<StudentDto, String> lrnColumn;
    
    @FXML
    private TableColumn<StudentDto, String> contactColumn;
    
    @FXML
    private TableColumn<StudentDto, String> parentGuardianColumn;
    
    @FXML
    private TableColumn<StudentDto, String> enrollmentStatusColumn;
    
    @FXML
    private TableColumn<StudentDto, String> actionsColumn;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<Integer> gradeFilterComboBox;
    
    @FXML
    private ComboBox<String> strandFilterComboBox;
    
    @FXML
    private ComboBox<String> enrollmentStatusFilterComboBox;
    
    @FXML
    private ComboBox<String> semesterFilterComboBox;
    
    @FXML
    private ComboBox<String> sectionFilterComboBox;
    
    @FXML
    private Label studentCountLabel;
    
    
    @FXML
    private Button refreshButton;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired(required = false)
    private SemesterService semesterService;
    
    private ObservableList<StudentDto> studentList;
    private FilteredList<StudentDto> filteredList;
    
    // Semester mapping: displayName -> semesterId (for filtering)
    private Map<String, Long> semesterFilterMap = new HashMap<>();
    // Additional mapping: displayName -> list of all semester IDs (for same display name across grade levels)
    private Map<String, List<Long>> semesterIdsByDisplayNameMap = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize filter comboboxes
        gradeFilterComboBox.getItems().addAll(null, 11, 12);
        strandFilterComboBox.getItems().addAll(null, "ABM", "HUMSS", "STEM", "GAS", "TVL");
        enrollmentStatusFilterComboBox.getItems().addAll(null, "Enrolled", "Pending");
        sectionFilterComboBox.getItems().add(null); // Will be populated dynamically
        
        // Load semesters for filter dropdown
        loadSemestersForFilter();
        
        // Initialize table columns with null-safe cell value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Row number column - displays sequential numbers (1, 2, 3, etc.)
        rowNumberColumn.setCellFactory(column -> new TableCell<StudentDto, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    // Get the index from the table's items (filtered/sorted list)
                    int index = getIndex();
                    setText(String.valueOf(index + 1));
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        
        nameColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getName();
            return new SimpleStringProperty(name != null ? name : "");
        });
        
        ageColumn.setCellValueFactory(cellData -> {
            Integer age = cellData.getValue().getAge();
            return new SimpleIntegerProperty(age != null ? age : 0).asObject();
        });
        
        sexColumn.setCellValueFactory(cellData -> {
            String sex = cellData.getValue().getSex();
            return new SimpleStringProperty(sex != null ? sex : "");
        });
        
        gradeLevelColumn.setCellValueFactory(cellData -> {
            Integer grade = cellData.getValue().getGradeLevel();
            return new SimpleIntegerProperty(grade != null ? grade : 0).asObject();
        });
        
        strandColumn.setCellValueFactory(cellData -> {
            String strand = cellData.getValue().getStrand();
            return new SimpleStringProperty(strand != null ? strand : "N/A");
        });
        
        sectionColumn.setCellValueFactory(cellData -> {
            String sectionName = cellData.getValue().getSectionName();
            return new SimpleStringProperty(sectionName != null ? sectionName : "N/A");
        });
        
        // Semester column - show semester for enrolled students, N/A for others
        semesterColumn.setCellValueFactory(cellData -> {
            StudentDto student = cellData.getValue();
            String enrollmentStatus = student.getEnrollmentStatus();
            if ("Enrolled".equals(enrollmentStatus)) {
                String semesterDisplay = student.getSemesterDisplayName();
                if (semesterDisplay != null && !semesterDisplay.isEmpty()) {
                    return new SimpleStringProperty(semesterDisplay);
                } else {
                    String semesterName = student.getSemesterName();
                    return new SimpleStringProperty(semesterName != null ? semesterName : "N/A");
                }
            } else {
                return new SimpleStringProperty("N/A");
            }
        });
        
        lrnColumn.setCellValueFactory(cellData -> {
            String lrn = cellData.getValue().getLrn();
            return new SimpleStringProperty(lrn != null ? lrn : "");
        });
        
        contactColumn.setCellValueFactory(cellData -> {
            String contact = cellData.getValue().getContactNumber();
            return new SimpleStringProperty(contact != null ? contact : "");
        });
        
        enrollmentStatusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getEnrollmentStatus();
            return new SimpleStringProperty(status != null ? status : "");
        });
        
        // Parent/Guardian column - custom cell value factory
        parentGuardianColumn.setCellValueFactory(cellData -> {
            StudentDto student = cellData.getValue();
            String parentInfo = student.getParentGuardianName();
            if (parentInfo == null || parentInfo.isEmpty()) {
                return new SimpleStringProperty("N/A");
            }
            return new SimpleStringProperty(parentInfo);
        });
        
        // Actions column - custom cell factory for buttons
        actionsColumn.setCellFactory(column -> new TableCell<StudentDto, String>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Archive to Logs");
            private final HBox hbox = new HBox(5, editButton, deleteButton);
            
            {
                editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                
                editButton.setOnAction(e -> {
                    StudentDto student = getTableView().getItems().get(getIndex());
                    handleEdit(student);
                });
                
                deleteButton.setOnAction(e -> {
                    StudentDto student = getTableView().getItems().get(getIndex());
                    handleDelete(student);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
        
        // Ensure table is properly initialized with proper placeholder
        if (studentsTable != null) {
            Label emptyPlaceholder = new Label("No students found.");
            emptyPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            studentsTable.setPlaceholder(emptyPlaceholder);
        }
        
        // Load semesters first, then students to ensure semester map is ready
        Platform.runLater(() -> {
            // Load semesters first so the map is ready when students are loaded
            loadSemestersForFilter();
            // Small delay to ensure semester map is populated before loading students
            Platform.runLater(() -> {
                loadStudents();
            });
        });
    }
    
    // Public method to refresh data (can be called from outside)
    public void refreshData() {
        loadStudents();
    }
    
    private void loadStudents() {
        // Show loading placeholder while loading
        if (studentsTable != null) {
            Label loadingPlaceholder = new Label("Loading students...");
            loadingPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            studentsTable.setPlaceholder(loadingPlaceholder);
        }
        
        Platform.runLater(() -> {
            try {
                // Load students from service
                java.util.List<StudentDto> students = studentService.getAllStudents();
                
                // Create observable list
                studentList = FXCollections.observableArrayList(students);
                
                // Create filtered list
                filteredList = new FilteredList<>(studentList, p -> true);
                
                // Create sorted list
                SortedList<StudentDto> sortedList = new SortedList<>(filteredList);
                sortedList.comparatorProperty().bind(studentsTable.comparatorProperty());
                
                // Set table items
                studentsTable.setItems(sortedList);
                
                // Update section filter options based on loaded students
                updateSectionFilterOptions(students);
                
                // Don't apply filters immediately - let user set filters manually
                // This avoids issues with semester map not being ready
                
                // Update placeholder to show "No students found" when empty
                Label emptyPlaceholder = new Label("No students found.");
                emptyPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
                studentsTable.setPlaceholder(emptyPlaceholder);
                
                // Update count
                updateStudentCount();
                
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error loading students: " + e.getMessage());
                // Set empty list on error
                studentList = FXCollections.observableArrayList();
                filteredList = new FilteredList<>(studentList, p -> true);
                studentsTable.setItems(new SortedList<>(filteredList));
                
                // Set empty placeholder
                Label emptyPlaceholder = new Label("No students found.");
                emptyPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
                studentsTable.setPlaceholder(emptyPlaceholder);
                
                updateStudentCount();
            }
        });
    }
    
    @FXML
    private void handleRefresh() {
        loadStudents();
        loadSemestersForFilter(); // Reload semesters in case current school year changed
        searchField.clear();
        gradeFilterComboBox.setValue(null);
        strandFilterComboBox.setValue(null);
        enrollmentStatusFilterComboBox.setValue(null);
        semesterFilterComboBox.setValue(null);
        sectionFilterComboBox.setValue(null);
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }
    
    @FXML
    private void handleFilter() {
        applyFilters();
    }
    
    private void applyFilters() {
        if (filteredList == null) {
            return;
        }
        
        String searchText = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        Integer gradeFilter = gradeFilterComboBox != null ? gradeFilterComboBox.getValue() : null;
        String strandFilter = strandFilterComboBox != null ? strandFilterComboBox.getValue() : null;
        String enrollmentStatusFilter = enrollmentStatusFilterComboBox != null ? enrollmentStatusFilterComboBox.getValue() : null;
        String semesterFilter = semesterFilterComboBox != null ? semesterFilterComboBox.getValue() : null;
        String sectionFilter = sectionFilterComboBox != null ? sectionFilterComboBox.getValue() : null;
        
        // Check if semester filter is set and if map has the value
        // If not in map, clear the filter to avoid breaking other filters
        if (semesterFilter != null && !semesterFilter.isEmpty()) {
            if (!semesterFilterMap.containsKey(semesterFilter)) {
                System.err.println("ViewStudentsController: Semester filter '" + semesterFilter + "' not in map!");
                System.err.println("ViewStudentsController: Available keys: " + semesterFilterMap.keySet());
                // Don't apply semester filter if not in map - allow other filters to work
                semesterFilter = null;
            }
        }
        
        // Create final variable for use in lambda
        final String finalSemesterFilter = semesterFilter;
        
        filteredList.setPredicate(student -> {
            if (student == null) {
                return false;
            }
            
            // Search filter - must match if search text is provided
            boolean matchesSearch = true;
            if (!searchText.isEmpty()) {
                matchesSearch = false;
                // Check name
                if (student.getName() != null && student.getName().toLowerCase().contains(searchText)) {
                    matchesSearch = true;
                }
                // Check LRN
                if (!matchesSearch && student.getLrn() != null && student.getLrn().toLowerCase().contains(searchText)) {
                    matchesSearch = true;
                }
                // Check contact number
                if (!matchesSearch && student.getContactNumber() != null && student.getContactNumber().contains(searchText)) {
                    matchesSearch = true;
                }
                // Check section name
                if (!matchesSearch && student.getSectionName() != null && student.getSectionName().toLowerCase().contains(searchText)) {
                    matchesSearch = true;
                }
            }
            
            if (!matchesSearch) {
                return false;
            }
            
            // Grade filter - must match if filter is set
            if (gradeFilter != null) {
                if (student.getGradeLevel() == null || !student.getGradeLevel().equals(gradeFilter)) {
                    return false;
                }
            }
            
            // Strand filter - must match if filter is set
            if (strandFilter != null && !strandFilter.isEmpty()) {
                if (student.getStrand() == null || !student.getStrand().equals(strandFilter)) {
                    return false;
                }
            }
            
            // Enrollment Status filter - must match if filter is set
            if (enrollmentStatusFilter != null && !enrollmentStatusFilter.isEmpty()) {
                if (student.getEnrollmentStatus() == null || !student.getEnrollmentStatus().equals(enrollmentStatusFilter)) {
                    return false;
                }
            }
            
            // Section filter - must match if filter is set
            if (sectionFilter != null && !sectionFilter.isEmpty()) {
                String studentSection = student.getSectionName();
                // Handle "N/A" case - if filter is set and student has no section, exclude
                if (studentSection == null || studentSection.isEmpty() || "N/A".equals(studentSection)) {
                    return false;
                }
                if (!studentSection.equals(sectionFilter)) {
                    return false;
                }
            }
            
            // Semester filter - must match if filter is set
            if (finalSemesterFilter != null && !finalSemesterFilter.isEmpty()) {
                // Get all semester IDs for this display name (handles multiple grade levels with same display name)
                List<Long> selectedSemesterIds = semesterIdsByDisplayNameMap.get(finalSemesterFilter);
                if (selectedSemesterIds != null && !selectedSemesterIds.isEmpty()) {
                    // Only enrolled students have semesters
                    // If student is enrolled, check if semester matches
                    if ("Enrolled".equals(student.getEnrollmentStatus())) {
                        // Student is enrolled, so check if semester matches
                        Long studentSemesterId = student.getSemesterId();
                        if (studentSemesterId == null) {
                            // Student is enrolled but has no semester - exclude
                            return false;
                        }
                        // Check if student's semester ID matches any of the selected semester IDs
                        // This handles cases where multiple grade levels have the same display name
                        if (!selectedSemesterIds.contains(studentSemesterId)) {
                            return false;
                        }
                    } else {
                        // Student is not enrolled (e.g., "Pending")
                        // Non-enrolled students don't have semesters, so exclude them when semester filter is active
                        return false;
                    }
                }
                // If semesterIds not found in map, skip semester filtering (don't break other filters)
            }
            
            // All filters passed
            return true;
        });
        
        updateStudentCount();
    }
    
    private void updateSectionFilterOptions(List<StudentDto> students) {
        if (sectionFilterComboBox == null) {
            return;
        }
        
        // Get unique section names from students
        java.util.Set<String> sectionNames = new java.util.HashSet<>();
        for (StudentDto student : students) {
            String sectionName = student.getSectionName();
            if (sectionName != null && !sectionName.isEmpty() && !"N/A".equals(sectionName)) {
                sectionNames.add(sectionName);
            }
        }
        
        // Sort section names
        java.util.List<String> sortedSections = new java.util.ArrayList<>(sectionNames);
        java.util.Collections.sort(sortedSections);
        
        // Update combo box
        String currentValue = sectionFilterComboBox.getValue();
        sectionFilterComboBox.getItems().clear();
        sectionFilterComboBox.getItems().add(null); // Add "All Sections" option
        sectionFilterComboBox.getItems().addAll(sortedSections);
        
        // Restore previous selection if it still exists
        if (currentValue != null && sortedSections.contains(currentValue)) {
            sectionFilterComboBox.setValue(currentValue);
        }
    }
    
    private void loadSemestersForFilter() {
        if (semesterService == null || semesterFilterComboBox == null) {
            System.err.println("Warning: SemesterService or semesterFilterComboBox is null in ViewStudentsController");
            return;
        }
        
        new Thread(() -> {
            try {
                // Only load semesters from CURRENT school year for View Students filter
                List<SemesterDto> semesters = semesterService.getSemestersForCurrentSchoolYear();
                
                System.out.println("ViewStudentsController: Loaded " + semesters.size() + " semesters for current school year filter");
                
                Platform.runLater(() -> {
                    try {
                        semesterFilterMap.clear();
                        semesterIdsByDisplayNameMap.clear();
                        semesterFilterComboBox.getItems().clear();
                        semesterFilterComboBox.getItems().add(null); // Add "All Semesters" option
                        
                        if (semesters.isEmpty()) {
                            System.err.println("Warning: No semesters found for current school year. Check if semesters exist and school year is set as current.");
                        }
                        
                        // Group semesters by display name (since multiple grade levels can have same display name)
                        // For filtering, we'll match any semester with the same display name
                        Map<String, List<Long>> semesterIdsByDisplayName = new HashMap<>();
                        for (SemesterDto semester : semesters) {
                            String displayName = semester.getDisplayName();
                            if (displayName != null && !displayName.isEmpty()) {
                                semesterIdsByDisplayName.computeIfAbsent(displayName, k -> new ArrayList<>()).add(semester.getId());
                            }
                        }
                        
                        // Add unique display names to combo box and store all IDs for each display name
                        for (Map.Entry<String, List<Long>> entry : semesterIdsByDisplayName.entrySet()) {
                            String displayName = entry.getKey();
                            List<Long> semesterIds = entry.getValue();
                            semesterFilterComboBox.getItems().add(displayName);
                            // Store the first ID (they're all equivalent for filtering purposes - same school year and semester number)
                            // But we need to check against all IDs when filtering
                            semesterFilterMap.put(displayName, semesterIds.get(0)); // Store first ID as primary
                            // Also store all IDs for this display name in a separate map for proper matching
                            semesterIdsByDisplayNameMap.put(displayName, semesterIds);
                            System.out.println("ViewStudentsController: Added semester to filter: " + displayName + " (IDs: " + semesterIds + ")");
                        }
                        
                        // Debug: Print all semester mappings
                        System.out.println("ViewStudentsController: Semester filter map contents:");
                        for (Map.Entry<String, Long> entry : semesterFilterMap.entrySet()) {
                            System.out.println("  '" + entry.getKey() + "' -> " + entry.getValue());
                        }
                        
                        // Set cell factory for proper display
                        semesterFilterComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(null);
                                } else {
                                    setText(item);
                                }
                            }
                        });
                        
                        // Set button cell factory
                        semesterFilterComboBox.setButtonCell(new javafx.scene.control.ListCell<String>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText("All Semesters");
                                } else {
                                    setText(item);
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Error populating semester filter: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    System.err.println("Error loading semesters for filter: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void updateStudentCount() {
        if (filteredList != null && studentCountLabel != null) {
            int count = filteredList.size();
            studentCountLabel.setText("Total Students: " + count);
        }
    }
    
    private void handleEdit(StudentDto student) {
        try {
            // Load Edit Student FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/EditStudent.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent editStudentRoot = loader.load();
            
            // Get the controller and set student data
            EditStudentController controller = loader.getController();
            controller.setStudent(student);
            
            // Create new stage for Edit Student form
            Stage editStudentStage = new Stage();
            editStudentStage.setTitle("Edit Student - Kinderscape");
            editStudentStage.setScene(new Scene(editStudentRoot));
            editStudentStage.setWidth(1200);
            editStudentStage.setHeight(750);
            editStudentStage.setResizable(false);
            
            // Set owner to View Students stage
            Stage viewStudentsStage = (Stage) studentsTable.getScene().getWindow();
            editStudentStage.initOwner(viewStudentsStage);
            
            // Refresh data when window is closed
            editStudentStage.setOnHidden(e -> refreshData());
            
            // Show Edit Student form
            editStudentStage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading edit form: " + e.getMessage());
        }
    }
    
    private void handleDelete(StudentDto student) {
        // Create custom dialog for archive reason
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Archive Student");
        dialog.setHeaderText("Archive Student: " + student.getName());
        dialog.setContentText("Please select the reason for archiving:");
        
        // Create ComboBox for archive reasons
        ComboBox<String> reasonComboBox = new ComboBox<>();
        reasonComboBox.getItems().addAll("DROPPED OUT", "GRADUATED", "TRANSFERRED TO ANOTHER SCHOOL");
        reasonComboBox.setPrefWidth(300);
        reasonComboBox.setPromptText("Select reason...");
        
        // Set button types
        ButtonType archiveButtonType = new ButtonType("Archive", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(archiveButtonType, ButtonType.CANCEL);
        
        // Add ComboBox to dialog
        dialog.getDialogPane().setContent(reasonComboBox);
        
        // Enable/disable archive button based on selection
        dialog.getDialogPane().lookupButton(archiveButtonType).setDisable(true);
        reasonComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            dialog.getDialogPane().lookupButton(archiveButtonType).setDisable(newValue == null);
        });
        
        // Convert result when archive button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == archiveButtonType) {
                return reasonComboBox.getValue();
            }
            return null;
        });
        
        // Show dialog and process result
        dialog.showAndWait().ifPresent(reason -> {
            if (reason != null && !reason.isEmpty()) {
                // Archive student in background thread to avoid blocking UI
                new Thread(() -> {
                    try {
                        // Archive student (this is a blocking database operation)
                        studentService.archiveStudent(student.getId(), reason);
                        
                        // Update UI on JavaFX thread
                        Platform.runLater(() -> {
                            // Show success message
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Success");
                            successAlert.setHeaderText("Student Archived");
                            successAlert.setContentText("Student " + student.getName() + " has been archived successfully.\nReason: " + reason);
                            successAlert.showAndWait();
                            
                            // Refresh the table
                            refreshData();
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Update UI on JavaFX thread to show error
                        Platform.runLater(() -> {
                            showError("Error archiving student: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

