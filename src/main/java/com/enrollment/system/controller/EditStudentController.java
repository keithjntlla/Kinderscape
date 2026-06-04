package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.Section;
import com.enrollment.system.service.SectionService;
import com.enrollment.system.service.StudentService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class EditStudentController {
    
    @FXML
    private TextField nameField;
    
    @FXML
    private DatePicker birthdatePicker;
    
    @FXML
    private TextField ageField;
    
    @FXML
    private ComboBox<String> sexComboBox;
    
    @FXML
    private TextField addressField;
    
    @FXML
    private TextField contactNumberField;
    
    @FXML
    private TextField parentGuardianNameField;
    
    @FXML
    private TextField parentGuardianContactField;
    
    @FXML
    private ComboBox<String> parentGuardianRelationshipComboBox;
    
    @FXML
    private ComboBox<Integer> gradeLevelComboBox;
    
    @FXML
    private ComboBox<String> strandComboBox;
    
    @FXML
    private ComboBox<Section> sectionComboBox;
    
    @FXML
    private TextField previousSchoolField;
    
    @FXML
    private TextField gwaField;
    
    @FXML
    private TextField lrnField;
    
    @FXML
    private ComboBox<String> enrollmentStatusComboBox;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Button updateButton;
    
    @FXML
    private Button cancelButton;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private SectionService sectionService;
    
    private Long studentId;
    
    @FXML
    public void initialize() {
        // Initialize Sex ComboBox
        sexComboBox.getItems().addAll("Male", "Female");
        
        // Initialize Parent/Guardian Relationship ComboBox
        parentGuardianRelationshipComboBox.getItems().addAll("Father", "Mother", "Guardian", "Other");
        
        // Initialize Grade Level ComboBox
        gradeLevelComboBox.getItems().addAll(11, 12);
        
        // Initialize Strand ComboBox
        strandComboBox.getItems().addAll("ABM", "HUMSS", "STEM", "GAS", "TVL");
        
        // Initialize Enrollment Status ComboBox (only Enrolled and Pending - others are for archiving)
        enrollmentStatusComboBox.getItems().addAll("Enrolled", "Pending");
        
        // Add listener to enrollment status to toggle section requirement and clear section if needed
        enrollmentStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSectionRequirement();
            // Clear section selection if status is not "Enrolled"
            if (!"Enrolled".equals(newValue) && sectionComboBox.getValue() != null) {
                sectionComboBox.setValue(null);
            }
        });
        
        // Add listener to grade level to update sections
        gradeLevelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSections();
        });
        
        // Add listener to birthdate picker to auto-calculate age
        birthdatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int age = calculateAge(newValue);
                ageField.setText(String.valueOf(age));
            }
        });
        
        // Add listener to age field to validate
        ageField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                ageField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        
        // Add listener to GWA field to validate decimal
        gwaField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                gwaField.setText(oldValue);
            }
        });
        
        // Set DatePicker to disable dates that would make student 15 or younger
        birthdatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null) {
                    int age = calculateAge(date);
                    if (age < 16) {
                        setDisable(true);
                        setStyle("-fx-background-color: #ffcccc;");
                    }
                }
            }
        });
    }
    
    public void setStudent(StudentDto student) {
        if (student == null) {
            return;
        }
        
        this.studentId = student.getId();
        
        // Populate form fields with student data
        nameField.setText(student.getName() != null ? student.getName() : "");
        birthdatePicker.setValue(student.getBirthdate());
        
        if (student.getAge() != null) {
            ageField.setText(String.valueOf(student.getAge()));
        }
        
        if (student.getSex() != null) {
            sexComboBox.setValue(student.getSex());
        }
        
        addressField.setText(student.getAddress() != null ? student.getAddress() : "");
        contactNumberField.setText(student.getContactNumber() != null ? student.getContactNumber() : "");
        parentGuardianNameField.setText(student.getParentGuardianName() != null ? student.getParentGuardianName() : "");
        parentGuardianContactField.setText(student.getParentGuardianContact() != null ? student.getParentGuardianContact() : "");
        
        if (student.getParentGuardianRelationship() != null) {
            parentGuardianRelationshipComboBox.setValue(student.getParentGuardianRelationship());
        }
        
        if (student.getGradeLevel() != null) {
            gradeLevelComboBox.setValue(student.getGradeLevel());
        }
        
        if (student.getStrand() != null) {
            strandComboBox.setValue(student.getStrand());
        }
        
        // Update sections based on strand and grade level
        updateSections();
        
        // Set section if student has one (only if enrolled)
        if (student.getSectionId() != null && "Enrolled".equals(student.getEnrollmentStatus())) {
            sectionComboBox.getItems().stream()
                    .filter(section -> section.getId().equals(student.getSectionId()))
                    .findFirst()
                    .ifPresent(sectionComboBox::setValue);
        } else if (!"Enrolled".equals(student.getEnrollmentStatus())) {
            // Clear section if student is not enrolled
            sectionComboBox.setValue(null);
        }
        
        // Update section requirement state
        updateSectionRequirement();
        
        previousSchoolField.setText(student.getPreviousSchool() != null ? student.getPreviousSchool() : "");
        
        if (student.getGwa() != null) {
            gwaField.setText(String.valueOf(student.getGwa()));
        }
        
        lrnField.setText(student.getLrn() != null ? student.getLrn() : "");
        
        if (student.getEnrollmentStatus() != null) {
            enrollmentStatusComboBox.setValue(student.getEnrollmentStatus());
        }
    }
    
    @FXML
    private void handleUpdate() {
        // Clear previous errors
        errorLabel.setVisible(false);
        errorLabel.setText("");
        
        // Validate required fields
        if (nameField.getText().trim().isEmpty()) {
            showError("Name is required.");
            return;
        }
        
        if (sexComboBox.getValue() == null || sexComboBox.getValue().isEmpty()) {
            showError("Sex is required.");
            return;
        }
        
        if (gradeLevelComboBox.getValue() == null) {
            showError("Grade Level is required.");
            return;
        }
        
        if (enrollmentStatusComboBox.getValue() == null || enrollmentStatusComboBox.getValue().isEmpty()) {
            showError("Enrollment Status is required.");
            return;
        }
        
        // Validate section assignment rules
        String enrollmentStatus = enrollmentStatusComboBox.getValue();
        if ("Enrolled".equals(enrollmentStatus) && sectionComboBox.getValue() == null) {
            showError("Enrolled students must be assigned to a section.");
            return;
        }
        
        // Validate: Non-enrolled students cannot have a section
        if (!"Enrolled".equals(enrollmentStatus) && sectionComboBox.getValue() != null) {
            showError("Sections can only be assigned to enrolled students. Please change the enrollment status to 'Enrolled' or remove the section assignment.");
            return;
        }
        
        if (studentId == null) {
            showError("Student ID is missing. Cannot update.");
            return;
        }
        
        // Validate birthdate is provided
        if (birthdatePicker.getValue() == null) {
            showError("Birthdate is required.");
            highlightField(birthdatePicker);
            return;
        }
        
        // Calculate and validate age
        int age;
        if (!ageField.getText().trim().isEmpty()) {
            try {
                age = Integer.parseInt(ageField.getText().trim());
            } catch (NumberFormatException e) {
                showError("Age must be a valid number.");
                highlightField(ageField);
                return;
            }
        } else {
            age = calculateAge(birthdatePicker.getValue());
        }
        
        // Validate age is at least 16
        if (age < 16) {
            showError("Student age must be at least 16 years old.");
            highlightField(ageField);
            highlightField(birthdatePicker);
            return;
        }
        
        // Recalculate age from birthdate to ensure consistency
        int calculatedAge = calculateAge(birthdatePicker.getValue());
        if (calculatedAge < 16) {
            showError("Student age must be at least 16 years old. Please select a valid birthdate.");
            highlightField(birthdatePicker);
            return;
        }
        
        try {
            // Create StudentDto with updated data
            StudentDto studentDto = new StudentDto();
            studentDto.setName(nameField.getText().trim());
            studentDto.setBirthdate(birthdatePicker.getValue());
            studentDto.setAge(calculatedAge); // Use calculated age from birthdate for consistency
            
            studentDto.setSex(sexComboBox.getValue());
            studentDto.setAddress(addressField.getText().trim());
            studentDto.setContactNumber(contactNumberField.getText().trim());
            studentDto.setParentGuardianName(parentGuardianNameField.getText().trim());
            studentDto.setParentGuardianContact(parentGuardianContactField.getText().trim());
            studentDto.setParentGuardianRelationship(parentGuardianRelationshipComboBox.getValue());
            studentDto.setGradeLevel(gradeLevelComboBox.getValue());
            studentDto.setStrand(strandComboBox.getValue());
            
            // Set section ID if section is selected
            if (sectionComboBox.getValue() != null) {
                studentDto.setSectionId(sectionComboBox.getValue().getId());
            }
            
            studentDto.setPreviousSchool(previousSchoolField.getText().trim());
            
            // Set GWA
            if (!gwaField.getText().trim().isEmpty()) {
                try {
                    studentDto.setGwa(Double.parseDouble(gwaField.getText().trim()));
                } catch (NumberFormatException e) {
                    showError("GWA must be a valid number.");
                    return;
                }
            }
            
            studentDto.setLrn(lrnField.getText().trim());
            studentDto.setEnrollmentStatus(enrollmentStatusComboBox.getValue());
            
            // Disable update button and show loading state
            updateButton.setDisable(true);
            updateButton.setText("Updating...");
            
            // Update student in background thread to avoid blocking UI
            new Thread(() -> {
                try {
                    // Update student (this is a blocking database operation)
                    StudentDto updatedStudent = studentService.updateStudent(studentId, studentDto);
                    
                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        updateButton.setDisable(false);
                        updateButton.setText("Update Student");
                        
                        // Show success message
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Student Updated Successfully");
                        alert.setContentText("Student " + updatedStudent.getName() + " has been updated successfully.");
                        alert.showAndWait();
                        
                        // Close the form
                        Stage stage = (Stage) updateButton.getScene().getWindow();
                        stage.close();
                    });
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    // Update UI on JavaFX thread to show error
                    Platform.runLater(() -> {
                        updateButton.setDisable(false);
                        updateButton.setText("Update Student");
                        showError("Error updating student: " + e.getMessage());
                    });
                }
            }).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error preparing update: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    @FXML
    private void handleStrandChange() {
        updateSections();
    }
    
    private void updateSections() {
        String selectedStrand = strandComboBox.getValue();
        Integer selectedGradeLevel = gradeLevelComboBox.getValue();
        
        Section currentSelection = sectionComboBox.getValue();
        sectionComboBox.getItems().clear();
        
        if (selectedStrand != null && selectedGradeLevel != null) {
            java.util.List<Section> sections = sectionService.getActiveSectionsByStrandAndGradeLevel(selectedStrand, selectedGradeLevel);
            sectionComboBox.getItems().addAll(sections);
            
            // Set cell factory to display section names
            sectionComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<Section>() {
                @Override
                protected void updateItem(Section section, boolean empty) {
                    super.updateItem(section, empty);
                    if (empty || section == null) {
                        setText(null);
                    } else {
                        setText(section.getName());
                    }
                }
            });
            
            // Set button cell factory
            sectionComboBox.setButtonCell(new javafx.scene.control.ListCell<Section>() {
                @Override
                protected void updateItem(Section section, boolean empty) {
                    super.updateItem(section, empty);
                    if (empty || section == null) {
                        setText(null);
                    } else {
                        setText(section.getName());
                    }
                }
            });
            
            // Try to restore previous selection if it's still valid
            if (currentSelection != null) {
                sectionComboBox.getItems().stream()
                        .filter(section -> section.getId().equals(currentSelection.getId()))
                        .findFirst()
                        .ifPresent(sectionComboBox::setValue);
            }
        }
        
        updateSectionRequirement();
    }
    
    private void updateSectionRequirement() {
        String enrollmentStatus = enrollmentStatusComboBox.getValue();
        boolean isRequired = "Enrolled".equals(enrollmentStatus);
        
        // Enable/disable section ComboBox based on enrollment status
        sectionComboBox.setDisable(!isRequired);
        
        // Clear section if enrollment status is not "Enrolled"
        if (!isRequired && sectionComboBox.getValue() != null) {
            sectionComboBox.setValue(null);
        }
    }
    
    private int calculateAge(LocalDate birthdate) {
        if (birthdate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - birthdate.getYear() - 
               (LocalDate.now().getDayOfYear() < birthdate.getDayOfYear() ? 1 : 0);
    }
    
    private void highlightField(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
        }
    }
    
    private void highlightField(DatePicker field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
        }
    }
    
    private void highlightComboBox(ComboBox<?> comboBox) {
        if (comboBox != null) {
            comboBox.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
        }
    }
}

