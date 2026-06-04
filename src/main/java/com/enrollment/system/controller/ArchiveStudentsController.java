package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.service.StudentService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Component
public class ArchiveStudentsController implements Initializable {
    
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
    private TableColumn<StudentDto, String> archiveReasonColumn;
    
    @FXML
    private TableColumn<StudentDto, String> archivedAtColumn;
    
    @FXML
    private TableColumn<StudentDto, String> actionsColumn;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> reasonFilterComboBox;
    
    @FXML
    private Label studentCountLabel;
    
    @FXML
    private Button refreshButton;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private ObservableList<StudentDto> studentList;
    private FilteredList<StudentDto> filteredList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize filter combobox
        reasonFilterComboBox.getItems().addAll(null, "DROPPED OUT", "GRADUATED", "TRANSFERRED TO ANOTHER SCHOOL");
        
        // Initialize table columns with null-safe cell value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        // Row number column
        rowNumberColumn.setCellFactory(column -> new TableCell<StudentDto, String>() {
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
        
        archiveReasonColumn.setCellValueFactory(cellData -> {
            String reason = cellData.getValue().getArchiveReason();
            return new SimpleStringProperty(reason != null ? reason : "");
        });
        
        archivedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime archivedAt = cellData.getValue().getArchivedAt();
            if (archivedAt != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                return new SimpleStringProperty(archivedAt.format(formatter));
            }
            return new SimpleStringProperty("");
        });
        
        // Actions column - re-enroll button
        actionsColumn.setCellFactory(column -> new TableCell<StudentDto, String>() {
            private final Button reEnrollButton = new Button("Re-enroll");
            private final HBox hbox = new HBox(5, reEnrollButton);
            
            {
                reEnrollButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                
                reEnrollButton.setOnAction(e -> {
                    StudentDto student = getTableView().getItems().get(getIndex());
                    handleReEnroll(student);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    StudentDto student = getTableView().getItems().get(getIndex());
                    // Disable re-enroll button for graduated students
                    String archiveReason = student.getArchiveReason();
                    boolean isGraduated = archiveReason != null && "GRADUATED".equalsIgnoreCase(archiveReason.trim());
                    reEnrollButton.setDisable(isGraduated);
                    if (isGraduated) {
                        reEnrollButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: default;");
                        reEnrollButton.setTooltip(new Tooltip("Graduated students cannot be re-enrolled"));
                    } else {
                        reEnrollButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                        reEnrollButton.setTooltip(null);
                    }
                    hbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
        
        // Ensure table is properly initialized
        if (studentsTable != null) {
            studentsTable.setPlaceholder(new Label("Loading archived students..."));
        }
        
        // Load students after a short delay to ensure UI is ready
        Platform.runLater(() -> {
            loadStudents();
        });
    }
    
    // Public method to refresh data (can be called from outside)
    public void refreshData() {
        loadStudents();
    }
    
    private void loadStudents() {
        try {
            // Load archived students from service
            java.util.List<StudentDto> students = studentService.getArchivedStudents();
            
            Platform.runLater(() -> {
                try {
                    // Create observable list
                    studentList = FXCollections.observableArrayList(students);
                    
                    // Create filtered list
                    filteredList = new FilteredList<>(studentList, p -> true);
                    
                    // Create sorted list
                    SortedList<StudentDto> sortedList = new SortedList<>(filteredList);
                    sortedList.comparatorProperty().bind(studentsTable.comparatorProperty());
                    
                    // Set table items
                    studentsTable.setItems(sortedList);
                    
                    // Update count
                    updateStudentCount();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Error displaying archived students: " + e.getMessage());
                    // Set empty list on error
                    studentList = FXCollections.observableArrayList();
                    filteredList = new FilteredList<>(studentList, p -> true);
                    studentsTable.setItems(new SortedList<>(filteredList));
                    updateStudentCount();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showError("Error loading archived students: " + e.getMessage() + "\n\nNote: The database may need to be updated. Please restart the application.");
                // Set empty list on error
                studentList = FXCollections.observableArrayList();
                filteredList = new FilteredList<>(studentList, p -> true);
                studentsTable.setItems(new SortedList<>(filteredList));
                updateStudentCount();
            });
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadStudents();
        searchField.clear();
        reasonFilterComboBox.setValue(null);
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
        String searchText = searchField.getText().toLowerCase();
        String reasonFilter = reasonFilterComboBox.getValue();
        
        filteredList.setPredicate(student -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                    (student.getName() != null && student.getName().toLowerCase().contains(searchText)) ||
                    (student.getLrn() != null && student.getLrn().toLowerCase().contains(searchText)) ||
                    (student.getContactNumber() != null && student.getContactNumber().contains(searchText));
            
            // Reason filter
            boolean matchesReason = reasonFilter == null || 
                    (student.getArchiveReason() != null && student.getArchiveReason().equals(reasonFilter));
            
            return matchesSearch && matchesReason;
        });
        
        updateStudentCount();
    }
    
    private void updateStudentCount() {
        if (filteredList != null && studentCountLabel != null) {
            int count = filteredList.size();
            studentCountLabel.setText("Total Archived Students: " + count);
        }
    }
    
    private void handleReEnroll(StudentDto student) {
        try {
            // Check if student is graduated - prevent re-enrollment
            String archiveReason = student.getArchiveReason();
            if (archiveReason != null && "GRADUATED".equalsIgnoreCase(archiveReason.trim())) {
                showError("Graduated students cannot be re-enrolled. Student \"" + student.getName() + "\" has already graduated.");
                return;
            }
            
            // Load Add Student FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/FXML/AddStudent.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            javafx.scene.Parent addStudentRoot = loader.load();
            
            // Get the controller
            AddStudentController addStudentController = loader.getController();
            
            // Set up the controller for re-enrollment from archived student
            if (addStudentController != null) {
                addStudentController.setupReEnrollmentFromArchived(student);
            }
            
            // Create new stage for Add Student form
            javafx.stage.Stage addStudentStage = new javafx.stage.Stage();
            addStudentStage.setTitle("Re-Enroll Student - Kinderscape");
            addStudentStage.setScene(new javafx.scene.Scene(addStudentRoot));
            addStudentStage.setResizable(true);
            addStudentStage.setWidth(1200);
            addStudentStage.setHeight(750);
            addStudentStage.setResizable(false);
            
            // Set owner to archive students stage
            javafx.stage.Stage archiveStage = (javafx.stage.Stage) studentsTable.getScene().getWindow();
            addStudentStage.initOwner(archiveStage);
            
            // Show Add Student form
            addStudentStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error opening re-enrollment form: " + e.getMessage());
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

