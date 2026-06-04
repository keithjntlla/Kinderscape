package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.Section;
import com.enrollment.system.service.SchoolYearService;
import com.enrollment.system.service.SectionService;
import com.enrollment.system.service.StudentService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class ReEnrollmentControllerUI implements Initializable {
    
    @FXML
    private TableView<StudentDto> studentsTable;
    
    @FXML
    private TableColumn<StudentDto, Boolean> selectColumn;
    
    @FXML
    private TableColumn<StudentDto, String> nameColumn;
    
    @FXML
    private TableColumn<StudentDto, Integer> gradeLevelColumn;
    
    @FXML
    private TableColumn<StudentDto, String> strandColumn;
    
    @FXML
    private TableColumn<StudentDto, String> enrollmentStatusColumn;
    
    @FXML
    private TableColumn<StudentDto, String> schoolYearColumn;
    
    @FXML
    private ComboBox<Integer> gradeLevelCombo;
    
    @FXML
    private ComboBox<String> strandCombo;
    
    @FXML
    private ComboBox<Section> sectionCombo;
    
    @FXML
    private Button reEnrollButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label studentCountLabel;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private SchoolYearService schoolYearService;
    
    @Autowired
    private SectionService sectionService;
    
    private ObservableList<StudentDto> studentList;
    private FilteredList<StudentDto> filteredList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupComboBoxes();
        loadEligibleStudents();
    }
    
    private void setupTableColumns() {
        // Select column with checkboxes
        selectColumn.setCellValueFactory(param -> {
            StudentDto student = param.getValue();
            return new javafx.beans.property.SimpleBooleanProperty(
                student.getSelectedForReEnrollment() != null && student.getSelectedForReEnrollment());
        });
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        gradeLevelColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevel"));
        strandColumn.setCellValueFactory(new PropertyValueFactory<>("strand"));
        enrollmentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("enrollmentStatus"));
        schoolYearColumn.setCellValueFactory(new PropertyValueFactory<>("schoolYear"));
    }
    
    private void setupComboBoxes() {
        gradeLevelCombo.setItems(FXCollections.observableArrayList(11, 12));
        gradeLevelCombo.setValue(12); // Default to Grade 12
        
        // Load strands
        try {
            List<String> strands = sectionService.getAllSections().stream()
                .map(Section::getStrand)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            strandCombo.setItems(FXCollections.observableArrayList(strands));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Update sections when strand changes
        strandCombo.setOnAction(e -> updateSections());
        gradeLevelCombo.setOnAction(e -> updateSections());
    }
    
    private void updateSections() {
        String selectedStrand = strandCombo.getValue();
        Integer selectedGrade = gradeLevelCombo.getValue();
        
        if (selectedStrand != null && selectedGrade != null) {
            try {
                List<Section> sections = sectionService.getAllSections().stream()
                    .filter(s -> selectedStrand.equals(s.getStrand()) && selectedGrade.equals(s.getGradeLevel()))
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .collect(Collectors.toList());
                
                sectionCombo.setItems(FXCollections.observableArrayList(sections));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Autowired
    private com.enrollment.system.repository.StudentRepository studentRepository;
    
    private void loadEligibleStudents() {
        new Thread(() -> {
            try {
                // Get previous school year (the one that ended before current started)
                com.enrollment.system.model.SchoolYear previousSchoolYear = schoolYearService.getPreviousSchoolYearEntity();
                
                // If no previous school year, show empty list
                if (previousSchoolYear == null) {
                    Platform.runLater(() -> {
                        studentList = FXCollections.observableArrayList();
                        filteredList = new FilteredList<>(studentList, p -> true);
                        SortedList<StudentDto> sortedList = new SortedList<>(filteredList);
                        sortedList.comparatorProperty().bind(studentsTable.comparatorProperty());
                        studentsTable.setItems(sortedList);
                        updateCount();
                    });
                    return;
                }
                
                // Get all students from repository
                List<com.enrollment.system.model.Student> allStudentsEntity = studentRepository.findAll();
                
                // Filter eligible students:
                // 1. From previous school year
                // 2. Grade 11 (will be promoted to Grade 12) OR Grade 12 (failed, will remain in Grade 12)
                // 3. Not archived
                List<StudentDto> eligibleStudents = allStudentsEntity.stream()
                    .filter(student -> {
                        if (Boolean.TRUE.equals(student.getIsArchived())) {
                            return false;
                        }
                        
                        // Must have a school year assigned
                        if (student.getSchoolYear() == null) {
                            return false;
                        }
                        
                        // Must be from previous school year
                        if (!student.getSchoolYear().getId().equals(previousSchoolYear.getId())) {
                            return false;
                        }
                        
                        // Must be Grade 11 or Grade 12
                        Integer gradeLevel = student.getGradeLevel();
                        return gradeLevel != null && (gradeLevel == 11 || gradeLevel == 12);
                    })
                    .map(StudentDto::fromStudent)
                    .collect(Collectors.toList());
                
                Platform.runLater(() -> {
                    try {
                        studentList = FXCollections.observableArrayList(eligibleStudents);
                        filteredList = new FilteredList<>(studentList, p -> true);
                        SortedList<StudentDto> sortedList = new SortedList<>(filteredList);
                        sortedList.comparatorProperty().bind(studentsTable.comparatorProperty());
                        
                        studentsTable.setItems(sortedList);
                        updateCount();
                        
                        // Set default grade level based on first selected student's current grade
                        // If Grade 11, default to 12 (promotion)
                        // If Grade 12, default to 12 (repeating)
                        if (!eligibleStudents.isEmpty()) {
                            // Check if all students are same grade level
                            boolean allGrade11 = eligibleStudents.stream()
                                .allMatch(s -> s.getGradeLevel() != null && s.getGradeLevel() == 11);
                            boolean allGrade12 = eligibleStudents.stream()
                                .allMatch(s -> s.getGradeLevel() != null && s.getGradeLevel() == 12);
                            
                            if (allGrade11) {
                                gradeLevelCombo.setValue(12); // Promote Grade 11 to 12
                            } else if (allGrade12) {
                                gradeLevelCombo.setValue(12); // Keep Grade 12 as 12
                            } else {
                                // Mixed grades - default to 12
                                gradeLevelCombo.setValue(12);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error displaying students: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error loading eligible students: " + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void handleReEnroll() {
        List<StudentDto> selectedStudents = studentList.stream()
            .filter(s -> Boolean.TRUE.equals(s.getSelectedForReEnrollment()))
            .collect(Collectors.toList());
        
        if (selectedStudents.isEmpty()) {
            showError("Please select at least one student to re-enroll.");
            return;
        }
        
        Integer newGradeLevel = gradeLevelCombo.getValue();
        Section selectedSection = sectionCombo.getValue();
        
        if (newGradeLevel == null) {
            showError("Please select a grade level.");
            return;
        }
        
        // Build confirmation message with promotion details
        StringBuilder confirmMessage = new StringBuilder();
        confirmMessage.append("Re-enroll ").append(selectedStudents.size()).append(" student(s)?\n\n");
        
        long grade11Count = selectedStudents.stream()
            .filter(s -> s.getGradeLevel() != null && s.getGradeLevel() == 11)
            .count();
        long grade12Count = selectedStudents.stream()
            .filter(s -> s.getGradeLevel() != null && s.getGradeLevel() == 12)
            .count();
        
        if (grade11Count > 0) {
            confirmMessage.append("• ").append(grade11Count).append(" Grade 11 student(s) will be promoted to Grade 12\n");
        }
        if (grade12Count > 0) {
            confirmMessage.append("• ").append(grade12Count).append(" Grade 12 student(s) will remain in Grade 12\n");
        }
        confirmMessage.append("\nStudents will be enrolled in the current school year.");
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Re-Enrollment");
        confirmAlert.setHeaderText("Confirm Re-Enrollment");
        confirmAlert.setContentText(confirmMessage.toString());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                final Integer finalGradeLevel = newGradeLevel;
                final Section finalSection = selectedSection;
                
                new Thread(() -> {
                    try {
                        final int[] successCount = {0};
                        final int[] failCount = {0};
                        final List<String> errors = new ArrayList<>();
                        
                        for (StudentDto student : selectedStudents) {
                            try {
                                // Determine new grade level:
                                // Grade 11 → Grade 12 (promotion)
                                // Grade 12 → Grade 12 (repeating)
                                Integer studentCurrentGrade = student.getGradeLevel();
                                Integer targetGrade = finalGradeLevel;
                                
                                // If student is Grade 11, promote to 12
                                if (studentCurrentGrade != null && studentCurrentGrade == 11) {
                                    targetGrade = 12;
                                }
                                // If student is Grade 12, keep at 12
                                else if (studentCurrentGrade != null && studentCurrentGrade == 12) {
                                    targetGrade = 12;
                                }
                                
                                // Update student
                                student.setGradeLevel(targetGrade);
                                if (finalSection != null) {
                                    student.setSectionId(finalSection.getId());
                                }
                                student.setEnrollmentStatus("Enrolled");
                                
                                // Set current school year
                                com.enrollment.system.model.SchoolYear currentSchoolYear = 
                                    schoolYearService.getCurrentSchoolYearEntity();
                                student.setSchoolYearId(currentSchoolYear.getId());
                                
                                studentService.updateStudent(student.getId(), student);
                                successCount[0]++;
                            } catch (Exception e) {
                                failCount[0]++;
                                errors.add(student.getName() + ": " + e.getMessage());
                            }
                        }
                        
                        final int finalSuccessCount = successCount[0];
                        final int finalFailCount = failCount[0];
                        final List<String> finalErrors = new ArrayList<>(errors);
                        
                        Platform.runLater(() -> {
                            String message = String.format(
                                "Re-enrollment completed!\n\nSuccess: %d\nFailed: %d",
                                finalSuccessCount, finalFailCount
                            );
                            
                            if (!finalErrors.isEmpty()) {
                                message += "\n\nErrors:\n" + String.join("\n", finalErrors);
                            }
                            
                            if (finalFailCount == 0) {
                                showSuccess(message);
                            } else {
                                showError(message);
                            }
                            
                            loadEligibleStudents();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showError("Error during re-enrollment: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    @FXML
    private void handleRefresh() {
        loadEligibleStudents();
    }
    
    private void updateCount() {
        if (studentList != null && studentCountLabel != null) {
            studentCountLabel.setText("Eligible Students: " + studentList.size());
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

