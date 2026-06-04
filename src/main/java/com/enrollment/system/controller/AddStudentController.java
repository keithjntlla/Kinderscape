package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.Section;
import com.enrollment.system.model.Strand;
import com.enrollment.system.model.Student;
import com.enrollment.system.service.SectionService;
import com.enrollment.system.service.StrandService;
import com.enrollment.system.service.StudentService;
import com.enrollment.system.service.SchoolYearService;
import com.enrollment.system.service.SemesterService;
import com.enrollment.system.dto.SemesterDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AddStudentController {
    
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
    private ComboBox<String> semesterComboBox;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    // New UI elements for choice screen and re-enrollment
    @FXML
    private VBox choiceScreen;
    
    @FXML
    private VBox studentSelectionScreen;
    
    @FXML
    private ScrollPane formScrollPane;
    
    @FXML
    private VBox formContainer;
    
    @FXML
    private Button btnAddNewStudent;
    
    @FXML
    private Button btnReEnrollStudent;
    
    @FXML
    private TableView<StudentDto> studentSelectionTable;
    
    @FXML
    private TableColumn<StudentDto, String> selectNameColumn;
    
    @FXML
    private TableColumn<StudentDto, Integer> selectGradeColumn;
    
    @FXML
    private TableColumn<StudentDto, String> selectStrandColumn;
    
    @FXML
    private TableColumn<StudentDto, String> selectSectionColumn;
    
    @FXML
    private Button btnCancelSelection;
    
    @FXML
    private Button btnSelectStudent;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button btnClearSearch;
    
    @FXML
    private ComboBox<Integer> filterGradeCombo;
    
    @FXML
    private ComboBox<String> filterStrandCombo;
    
    @FXML
    private ComboBox<String> filterSectionCombo;
    
    @FXML
    private Button btnClearFilters;
    
    @FXML
    private Label resultsCountLabel;
    
    @FXML
    private Label formTitleLabel;
    
    @FXML
    private Label modeIndicatorLabel;
    
    @FXML
    private VBox reEnrollmentReasonSection;
    
    @FXML
    private TextArea reEnrollmentReasonField;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private SectionService sectionService;
    
    @Autowired
    private StrandService strandService;
    
    @Autowired(required = false)
    private SchoolYearService schoolYearService;
    
    @Autowired(required = false)
    private com.enrollment.system.repository.StudentRepository studentRepository;
    
    @Autowired(required = false)
    private SemesterService semesterService;
    
    // Semester mapping: displayName -> semesterId
    private Map<String, Long> semesterMap = new HashMap<>();
    // School year mapping: semesterId -> schoolYearId
    private Map<Long, Long> semesterToSchoolYearMap = new HashMap<>();
    // Store all loaded semesters for filtering
    private List<SemesterDto> allSemesters = new ArrayList<>();
    
    // Mode tracking
    private enum Mode { NEW_STUDENT, RE_ENROLL }
    private Mode currentMode = Mode.NEW_STUDENT;
    private StudentDto selectedStudentForReEnrollment = null;
    private ObservableList<StudentDto> eligibleStudentsList;
    private FilteredList<StudentDto> filteredStudentsList;
    
    @FXML
    public void initialize() {
        // Show choice screen initially, hide form and selection screen
        if (choiceScreen != null) {
            choiceScreen.setVisible(true);
            choiceScreen.setManaged(true);
        }
        if (formScrollPane != null) {
            formScrollPane.setVisible(false);
            formScrollPane.setManaged(false);
        }
        if (formContainer != null) {
            formContainer.setVisible(true);
            formContainer.setManaged(true);
        }
        if (studentSelectionScreen != null) {
            studentSelectionScreen.setVisible(false);
            studentSelectionScreen.setManaged(false);
        }
        
        // Initialize student selection table
        if (studentSelectionTable != null && selectNameColumn != null) {
            selectNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            selectGradeColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevel"));
            selectStrandColumn.setCellValueFactory(new PropertyValueFactory<>("strand"));
            if (selectSectionColumn != null) {
                selectSectionColumn.setCellValueFactory(new PropertyValueFactory<>("sectionName"));
            }
            studentSelectionTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }
        
        // Initialize filter combo boxes
        if (filterGradeCombo != null) {
            filterGradeCombo.getItems().addAll(null, 11, 12);
        }
        
        // Setup search and filter listeners
        setupSearchAndFilters();
        
        // Initialize Sex ComboBox
        if (sexComboBox != null) {
            sexComboBox.getItems().addAll("Male", "Female");
        }
        
        // Initialize Parent/Guardian Relationship ComboBox
        if (parentGuardianRelationshipComboBox != null) {
            parentGuardianRelationshipComboBox.getItems().addAll("Father", "Mother", "Guardian", "Other");
        }
        
        // Initialize Grade Level ComboBox
        if (gradeLevelComboBox != null) {
            gradeLevelComboBox.getItems().addAll(11, 12);
        }
        
        // Initialize Strand ComboBox - Load active strands from service
        loadActiveStrands();
        
        // Add listener to strand to update sections when strand changes
        strandComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSections();
        });
        
        // Initialize Enrollment Status ComboBox (only Enrolled and Pending)
        enrollmentStatusComboBox.getItems().addAll("Enrolled", "Pending");
        
        // Set default values
        enrollmentStatusComboBox.setValue("Pending");
        
        // Load semesters for dropdown (will be filtered by grade level)
        loadSemesters();
        
        // Add listener to enrollment status to toggle section requirement and clear section if needed
        enrollmentStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSectionRequirement();
            // Clear section selection if status is not "Enrolled"
            if (!"Enrolled".equals(newValue) && sectionComboBox.getValue() != null) {
                sectionComboBox.setValue(null);
            }
        });
        
        // Add listener to grade level to update sections AND filter semesters
        gradeLevelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSections();
            updateSemestersByGradeLevel(newValue);
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
        
        // Add listener to GWA field to validate decimal and range
        gwaField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                gwaField.setText(oldValue);
            } else if (!newValue.isEmpty()) {
                try {
                    double gwaValue = Double.parseDouble(newValue);
                    if (gwaValue < 75.0 || gwaValue > 100.0) {
                        gwaField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                    } else {
                        gwaField.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    // Invalid format, will be handled on focus lost
                }
            } else {
                gwaField.setStyle("");
            }
        });
        
        // Validate GWA on focus lost
        gwaField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // When focus is lost
                String text = gwaField.getText().trim();
                if (!text.isEmpty()) {
                    try {
                        double gwaValue = Double.parseDouble(text);
                        if (gwaValue < 75.0 || gwaValue > 100.0) {
                            gwaField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                        } else {
                            gwaField.setStyle("");
                        }
                    } catch (NumberFormatException e) {
                        gwaField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                    }
                }
            }
        });
        
        // Limit LRN to 12 digits only
        lrnField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 12) {
                lrnField.setText(oldValue);
            } else if (!newValue.matches("^\\d*$")) {
                // Only allow digits
                lrnField.setText(oldValue);
            }
        });
        
        // Limit contact number fields to 11 digits
        contactNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 11) {
                contactNumberField.setText(oldValue);
            } else if (!newValue.matches("^\\d*$")) {
                // Only allow digits
                contactNumberField.setText(oldValue);
            }
        });
        
        parentGuardianContactField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 11) {
                parentGuardianContactField.setText(oldValue);
            } else if (!newValue.matches("^\\d*$")) {
                // Only allow digits
                parentGuardianContactField.setText(oldValue);
            }
        });
        
        // Add listener to contact number fields to validate on focus lost
        contactNumberField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // When focus is lost
                String text = contactNumberField.getText().trim();
                if (!text.isEmpty() && !isValidPhilippineContactNumber(text)) {
                    contactNumberField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                } else {
                    contactNumberField.setStyle("");
                }
            }
        });
        
        parentGuardianContactField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // When focus is lost
                String text = parentGuardianContactField.getText().trim();
                if (!text.isEmpty() && !isValidPhilippineContactNumber(text)) {
                    parentGuardianContactField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                } else {
                    parentGuardianContactField.setStyle("");
                }
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
        
        // Add listener to previous school field to validate abbreviations
        previousSchoolField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // When focus is lost
                String text = previousSchoolField.getText().trim();
                if (!text.isEmpty() && isAllCapitalAbbreviation(text)) {
                    previousSchoolField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
                } else {
                    previousSchoolField.setStyle("");
                }
            }
        });
    }
    
    private void loadActiveStrands() {
        try {
            java.util.List<Strand> activeStrands = strandService.getActiveStrands();
            strandComboBox.getItems().clear();
            for (Strand strand : activeStrands) {
                strandComboBox.getItems().add(strand.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to hardcoded values if service fails
            strandComboBox.getItems().addAll("ABM", "HUMSS", "STEM", "GAS", "TVL");
        }
    }
    
    private boolean isValidPhilippineContactNumber(String contactNumber) {
        // Philippine format: 09XXXXXXXXX (exactly 11 digits starting with 09)
        return contactNumber.matches("^09\\d{9}$") && contactNumber.length() == 11;
    }
    
    @FXML
    private void handleStrandChange() {
        updateSections();
    }
    
    private void updateSections() {
        String selectedStrand = strandComboBox.getValue();
        Integer selectedGradeLevel = gradeLevelComboBox.getValue();
        
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
    
    @FXML
    private void handleSave() {
        // Clear previous errors and field styles
        errorLabel.setVisible(false);
        errorLabel.setText("");
        clearFieldStyles();
        
        boolean hasErrors = false;
        
        // Validate required fields
        if (nameField.getText().trim().isEmpty()) {
            highlightField(nameField);
            hasErrors = true;
        }
        
        if (birthdatePicker.getValue() == null) {
            highlightField(birthdatePicker);
            hasErrors = true;
        }
        
        // Validate age
        int age = 0;
        if (birthdatePicker.getValue() != null) {
            age = calculateAge(birthdatePicker.getValue());
            // Update age field if it's empty or different
            if (ageField.getText().trim().isEmpty() || 
                (!ageField.getText().trim().isEmpty() && 
                 Integer.parseInt(ageField.getText().trim()) != age)) {
                ageField.setText(String.valueOf(age));
            }
        } else if (!ageField.getText().trim().isEmpty()) {
            try {
                age = Integer.parseInt(ageField.getText().trim());
            } catch (NumberFormatException e) {
                highlightField(ageField);
                hasErrors = true;
            }
        }
        
        if (age < 16) {
            highlightField(ageField);
            if (birthdatePicker.getValue() != null) {
                highlightField(birthdatePicker);
            }
            hasErrors = true;
        }
        
        if (sexComboBox.getValue() == null || sexComboBox.getValue().isEmpty()) {
            highlightComboBox(sexComboBox);
            hasErrors = true;
        }
        
        if (addressField.getText().trim().isEmpty()) {
            highlightField(addressField);
            hasErrors = true;
        }
        
        // Validate contact number (Philippine format)
        String contactNumber = contactNumberField.getText().trim();
        if (contactNumber.isEmpty()) {
            highlightField(contactNumberField);
            hasErrors = true;
        } else if (contactNumber.length() != 11) {
            highlightField(contactNumberField);
            hasErrors = true;
        } else if (!isValidPhilippineContactNumber(contactNumber)) {
            highlightField(contactNumberField);
            hasErrors = true;
        }
        
        // Validate parent/guardian information
        if (parentGuardianNameField.getText().trim().isEmpty()) {
            highlightField(parentGuardianNameField);
            hasErrors = true;
        }
        
        String parentContact = parentGuardianContactField.getText().trim();
        if (parentContact.isEmpty()) {
            highlightField(parentGuardianContactField);
            hasErrors = true;
        } else if (parentContact.length() != 11) {
            highlightField(parentGuardianContactField);
            hasErrors = true;
        } else if (!isValidPhilippineContactNumber(parentContact)) {
            highlightField(parentGuardianContactField);
            hasErrors = true;
        }
        
        // Validate: Student and parent contact numbers must be different
        if (!contactNumber.isEmpty() && !parentContact.isEmpty() && contactNumber.equals(parentContact)) {
            highlightField(contactNumberField);
            highlightField(parentGuardianContactField);
            hasErrors = true;
        }
        
        if (parentGuardianRelationshipComboBox.getValue() == null || parentGuardianRelationshipComboBox.getValue().isEmpty()) {
            highlightComboBox(parentGuardianRelationshipComboBox);
            hasErrors = true;
        }
        
        if (gradeLevelComboBox.getValue() == null) {
            highlightComboBox(gradeLevelComboBox);
            hasErrors = true;
        }
        
        if (strandComboBox.getValue() == null || strandComboBox.getValue().isEmpty()) {
            highlightComboBox(strandComboBox);
            hasErrors = true;
        }
        
        // Validate previous school (must not be all-capital abbreviations)
        String previousSchool = previousSchoolField.getText().trim();
        if (previousSchool.isEmpty()) {
            highlightField(previousSchoolField);
            hasErrors = true;
        } else if (isAllCapitalAbbreviation(previousSchool)) {
            highlightField(previousSchoolField);
            hasErrors = true;
        }
        
        // Validate GWA
        double gwa = 0.0;
        if (gwaField.getText().trim().isEmpty()) {
            highlightField(gwaField);
            hasErrors = true;
        } else {
            try {
                gwa = Double.parseDouble(gwaField.getText().trim());
                if (gwa < 75.0 || gwa > 100.0) {
                    highlightField(gwaField);
                    hasErrors = true;
                }
            } catch (NumberFormatException e) {
                highlightField(gwaField);
                hasErrors = true;
            }
        }
        
        // Validate LRN (must be exactly 12 digits)
        String lrn = lrnField.getText().trim();
        if (lrn.isEmpty()) {
            highlightField(lrnField);
            hasErrors = true;
        } else if (lrn.length() != 12) {
            highlightField(lrnField);
            hasErrors = true;
        } else if (!lrn.matches("^\\d{12}$")) {
            highlightField(lrnField);
            hasErrors = true;
        }
        
        if (enrollmentStatusComboBox.getValue() == null || enrollmentStatusComboBox.getValue().isEmpty()) {
            highlightComboBox(enrollmentStatusComboBox);
            hasErrors = true;
        }
        
        // Validate semester selection
        if (semesterComboBox.getValue() == null || semesterComboBox.getValue().isEmpty()) {
            highlightComboBox(semesterComboBox);
            hasErrors = true;
        }
        
        // Show error message if validation failed
        if (hasErrors) {
            StringBuilder errorMessage = new StringBuilder("Please correct the following errors:\n\n");
            
            if (nameField.getText().trim().isEmpty()) {
                errorMessage.append("• Name is required.\n");
            }
            if (birthdatePicker.getValue() == null) {
                errorMessage.append("• Birthdate is required.\n");
            }
            if (age < 16 && birthdatePicker.getValue() != null) {
                errorMessage.append("• Student age must be at least 16 years old.\n");
            }
            if (sexComboBox.getValue() == null || sexComboBox.getValue().isEmpty()) {
                errorMessage.append("• Sex is required.\n");
            }
            if (addressField.getText().trim().isEmpty()) {
                errorMessage.append("• Address is required.\n");
            }
            if (contactNumber.isEmpty()) {
                errorMessage.append("• Contact Number is required.\n");
            } else if (contactNumber.length() != 11) {
                errorMessage.append("• Contact Number must be exactly 11 digits.\n");
            } else if (!isValidPhilippineContactNumber(contactNumber)) {
                errorMessage.append("• Contact Number must follow Philippine format (09XXXXXXXXX - 11 digits starting with 09).\n");
            }
            if (parentGuardianNameField.getText().trim().isEmpty()) {
                errorMessage.append("• Parent/Guardian Name is required.\n");
            }
            if (parentContact.isEmpty()) {
                errorMessage.append("• Parent/Guardian Contact Number is required.\n");
            } else if (parentContact.length() != 11) {
                errorMessage.append("• Parent/Guardian Contact Number must be exactly 11 digits.\n");
            } else if (!isValidPhilippineContactNumber(parentContact)) {
                errorMessage.append("• Parent/Guardian Contact Number must follow Philippine format (09XXXXXXXXX - 11 digits starting with 09).\n");
            }
            if (!contactNumber.isEmpty() && !parentContact.isEmpty() && contactNumber.equals(parentContact)) {
                errorMessage.append("• Student contact number cannot be the same as parent/guardian contact number.\n");
            }
            if (parentGuardianRelationshipComboBox.getValue() == null || parentGuardianRelationshipComboBox.getValue().isEmpty()) {
                errorMessage.append("• Relationship is required.\n");
            }
            if (gradeLevelComboBox.getValue() == null) {
                errorMessage.append("• Grade Level is required.\n");
            }
            if (strandComboBox.getValue() == null || strandComboBox.getValue().isEmpty()) {
                errorMessage.append("• Strand is required.\n");
            }
            if (previousSchool.isEmpty()) {
                errorMessage.append("• Previous School is required.\n");
            } else if (isAllCapitalAbbreviation(previousSchool)) {
                errorMessage.append("• Previous School must be spelled out in full. Abbreviations like SNHS are not allowed.\n");
            }
            if (gwaField.getText().trim().isEmpty()) {
                errorMessage.append("• GWA is required.\n");
            } else {
                try {
                    double gwaCheck = Double.parseDouble(gwaField.getText().trim());
                    if (gwaCheck < 75.0 || gwaCheck > 100.0) {
                        errorMessage.append("• GWA must be between 75.00 and 100.00.\n");
                    }
                } catch (NumberFormatException e) {
                    errorMessage.append("• GWA must be a valid number.\n");
                }
            }
            if (lrn.isEmpty()) {
                errorMessage.append("• LRN is required.\n");
            } else if (lrn.length() != 12) {
                errorMessage.append("• LRN must be exactly 12 digits.\n");
            } else if (!lrn.matches("^\\d{12}$")) {
                errorMessage.append("• LRN must contain only numbers.\n");
            }
            if (enrollmentStatusComboBox.getValue() == null || enrollmentStatusComboBox.getValue().isEmpty()) {
                errorMessage.append("• Enrollment Status is required.\n");
            }
            if (semesterComboBox.getValue() == null || semesterComboBox.getValue().isEmpty()) {
                errorMessage.append("• School Year & Semester is required.\n");
            }
            
            showError(errorMessage.toString());
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
        
        // Additional validation for re-enrollment mode
        if (currentMode == Mode.RE_ENROLL && selectedStudentForReEnrollment != null) {
            Integer currentGrade = selectedStudentForReEnrollment.getGradeLevel();
            Integer newGrade = gradeLevelComboBox.getValue();
            Long currentSectionId = selectedStudentForReEnrollment.getSectionId();
            Long newSectionId = sectionComboBox.getValue() != null ? sectionComboBox.getValue().getId() : null;
            
            // Validate Grade 12 re-enrollment: Must have repeater reason
            if (currentGrade != null && currentGrade == 12 && newGrade != null && newGrade == 12) {
                String reason = reEnrollmentReasonField != null ? reEnrollmentReasonField.getText().trim() : "";
                if (reason.isEmpty()) {
                    showError("Re-enrollment reason is required for Grade 12 students. Grade 12 students can only re-enroll if they are being marked as a repeater. Please provide a reason (e.g., Repeater, Failed subjects, etc.).");
                    return;
                }
            }
            
            // Validate Grade 11 re-enrollment: Cannot re-enroll to same grade and section without reason
            if (currentGrade != null && currentGrade == 11 && newGrade != null && newGrade == 11) {
                // Check if trying to re-enroll to the same section
                if (currentSectionId != null && newSectionId != null && currentSectionId.equals(newSectionId)) {
                    String reason = reEnrollmentReasonField != null ? reEnrollmentReasonField.getText().trim() : "";
                    if (reason.isEmpty()) {
                        showError("Re-enrollment reason is required. Grade 11 students cannot re-enroll into the same grade and section without a validated reason. Please provide a reason for this re-enrollment.");
                        return;
                    }
                }
            }
            
            // Validate Grade 11 can only be promoted to Grade 12 (unless staying in Grade 11 with reason)
            if (currentGrade != null && currentGrade == 11 && newGrade != null && newGrade != 11 && newGrade != 12) {
                showError("Grade 11 students can only be promoted to Grade 12 or re-enroll in Grade 11 with a valid reason.");
                return;
            }
        }
        
        // Disable save button and show loading state
        saveButton.setDisable(true);
        saveButton.setText("Saving...");
        
        // Create StudentDto with all validated data
        StudentDto studentDto = new StudentDto();
        studentDto.setName(nameField.getText().trim());
        studentDto.setBirthdate(birthdatePicker.getValue());
        studentDto.setAge(age);
        studentDto.setSex(sexComboBox.getValue());
        studentDto.setAddress(addressField.getText().trim());
        studentDto.setContactNumber(contactNumber);
        studentDto.setParentGuardianName(parentGuardianNameField.getText().trim());
        studentDto.setParentGuardianContact(parentContact);
        studentDto.setParentGuardianRelationship(parentGuardianRelationshipComboBox.getValue());
        studentDto.setGradeLevel(gradeLevelComboBox.getValue());
        studentDto.setStrand(strandComboBox.getValue());
        
        // Set section ID if section is selected (only for Enrolled students)
        if (sectionComboBox.getValue() != null && "Enrolled".equals(enrollmentStatus)) {
            studentDto.setSectionId(sectionComboBox.getValue().getId());
        } else {
            studentDto.setSectionId(null);
        }
        
        studentDto.setPreviousSchool(previousSchool);
        studentDto.setGwa(gwa);
        studentDto.setLrn(lrn);
        studentDto.setEnrollmentStatus(enrollmentStatus);
        
        // Set semester ID and school year ID from selection
        // Match semester based on display name AND grade level to handle duplicates
        String selectedSemesterDisplay = semesterComboBox.getValue();
        Integer selectedGradeLevel = gradeLevelComboBox.getValue();
        
        if (selectedSemesterDisplay != null && !selectedSemesterDisplay.isEmpty() && selectedGradeLevel != null) {
            // Find semester matching both display name and grade level
            SemesterDto matchedSemester = allSemesters.stream()
                .filter(s -> selectedSemesterDisplay.equals(s.getDisplayName()) &&
                            s.getGradeLevel() != null &&
                            s.getGradeLevel().equals(selectedGradeLevel))
                .findFirst()
                .orElse(null);
            
            if (matchedSemester != null) {
                Long semesterId = matchedSemester.getId();
                studentDto.setSemesterId(semesterId);
                
                // Set school year ID from semester mapping
                if (semesterToSchoolYearMap.containsKey(semesterId)) {
                    studentDto.setSchoolYearId(semesterToSchoolYearMap.get(semesterId));
                } else if (matchedSemester.getSchoolYearId() != null) {
                    studentDto.setSchoolYearId(matchedSemester.getSchoolYearId());
                }
            } else {
                studentDto.setSemesterId(null);
            }
        } else {
            studentDto.setSemesterId(null);
        }
        
        // Set re-enrollment reason if in re-enrollment mode (for Grade 12 or Grade 11 staying in same grade/section)
        if (currentMode == Mode.RE_ENROLL && selectedStudentForReEnrollment != null) {
            Integer currentGrade = selectedStudentForReEnrollment.getGradeLevel();
            Integer newGrade = gradeLevelComboBox.getValue();
            Long currentSectionId = selectedStudentForReEnrollment.getSectionId();
            Long newSectionId = sectionComboBox.getValue() != null ? sectionComboBox.getValue().getId() : null;
            
            String reason = reEnrollmentReasonField != null ? reEnrollmentReasonField.getText().trim() : "";
            
            // Set reason for Grade 12 re-enrollment (always required)
            if (currentGrade != null && currentGrade == 12 && newGrade != null && newGrade == 12) {
                studentDto.setReEnrollmentReason(reason);
            }
            // Set reason for Grade 11 re-enrolling to same grade and section
            else if (currentGrade != null && currentGrade == 11 && newGrade != null && newGrade == 11) {
                if (currentSectionId != null && newSectionId != null && currentSectionId.equals(newSectionId)) {
                    studentDto.setReEnrollmentReason(reason);
                }
            }
        }
        
        // Save or update student in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                StudentDto savedStudent;
                
                if (currentMode == Mode.RE_ENROLL && selectedStudentForReEnrollment != null) {
                    // Re-enrollment: Update existing student
                    studentDto.setId(selectedStudentForReEnrollment.getId());
                    
                    // Set current school year for re-enrollment
                    if (schoolYearService != null) {
                        com.enrollment.system.model.SchoolYear currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
                        studentDto.setSchoolYearId(currentSchoolYear.getId());
                    }
                    
                    savedStudent = studentService.updateStudent(selectedStudentForReEnrollment.getId(), studentDto);
                } else {
                    // New student: Save new student
                    savedStudent = studentService.saveStudent(studentDto);
                }
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save Student");
                    
                    // Show success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    if (currentMode == Mode.RE_ENROLL) {
                        alert.setHeaderText("Student Re-Enrolled Successfully");
                        alert.setContentText("Student " + savedStudent.getName() + " has been re-enrolled successfully.");
                    } else {
                        alert.setHeaderText("Student Added Successfully");
                        alert.setContentText("Student " + savedStudent.getName() + " has been saved successfully.");
                    }
                    alert.showAndWait();
                    
                    // Close the form
                    Stage stage = (Stage) saveButton.getScene().getWindow();
                    stage.close();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                // Update UI on JavaFX thread to show error
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save Student");
                    
                    // Check if error is related to section capacity
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("has reached its capacity")) {
                        // Show alert dialog for capacity errors with better formatting
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Section Capacity Reached");
                        alert.setHeaderText("Cannot Add Student to Selected Section");
                        alert.setContentText(errorMessage);
                        alert.getDialogPane().setPrefWidth(600);
                        alert.showAndWait();
                    } else {
                        showError("Error " + (currentMode == Mode.RE_ENROLL ? "re-enrolling" : "saving") + " student: " + errorMessage);
                    }
                });
            }
        }).start();
    }
    
    @FXML
    private void handleAddNewStudent() {
        currentMode = Mode.NEW_STUDENT;
        selectedStudentForReEnrollment = null;
        
        // Hide choice screen, show form
        if (choiceScreen != null) {
            choiceScreen.setVisible(false);
            choiceScreen.setManaged(false);
        }
        if (formScrollPane != null) {
            formScrollPane.setVisible(true);
            formScrollPane.setManaged(true);
        }
        if (formContainer != null) {
            formContainer.setVisible(true);
            formContainer.setManaged(true);
        }
        if (studentSelectionScreen != null) {
            studentSelectionScreen.setVisible(false);
            studentSelectionScreen.setManaged(false);
        }
        
        // Update form title and hide mode indicator
        if (formTitleLabel != null) {
            formTitleLabel.setText("Add New Student");
        }
        if (modeIndicatorLabel != null) {
            modeIndicatorLabel.setVisible(false);
        }
        if (reEnrollmentReasonSection != null) {
            reEnrollmentReasonSection.setVisible(false);
            reEnrollmentReasonSection.setManaged(false);
        }
        
        // Clear all fields
        clearAllFields();
    }
    
    @FXML
    private void handleReEnrollStudent() {
        currentMode = Mode.RE_ENROLL;
        
        // Hide choice screen, show student selection
        if (choiceScreen != null) {
            choiceScreen.setVisible(false);
            choiceScreen.setManaged(false);
        }
        if (studentSelectionScreen != null) {
            studentSelectionScreen.setVisible(true);
            studentSelectionScreen.setManaged(true);
        }
        if (formScrollPane != null) {
            formScrollPane.setVisible(false);
            formScrollPane.setManaged(false);
        }
        if (formContainer != null) {
            formContainer.setVisible(true);
            formContainer.setManaged(true);
        }
        
        // Load eligible students
        loadEligibleStudentsForReEnrollment();
    }
    
    @FXML
    private void handleCancelSelection() {
        // Go back to choice screen
        if (choiceScreen != null) {
            choiceScreen.setVisible(true);
            choiceScreen.setManaged(true);
        }
        if (studentSelectionScreen != null) {
            studentSelectionScreen.setVisible(false);
            studentSelectionScreen.setManaged(false);
        }
        selectedStudentForReEnrollment = null;
    }
    
    @FXML
    private void handleSelectStudent() {
        StudentDto selected = studentSelectionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student from the table.");
            alert.showAndWait();
            return;
        }
        
        selectedStudentForReEnrollment = selected;
        
        // Hide selection screen, show form
        if (studentSelectionScreen != null) {
            studentSelectionScreen.setVisible(false);
            studentSelectionScreen.setManaged(false);
        }
        if (formScrollPane != null) {
            formScrollPane.setVisible(true);
            formScrollPane.setManaged(true);
        }
        if (formContainer != null) {
            formContainer.setVisible(true);
            formContainer.setManaged(true);
        }
        
        // Update form title and show mode indicator
        if (formTitleLabel != null) {
            formTitleLabel.setText("Re-Enroll Student");
        }
        if (modeIndicatorLabel != null) {
            modeIndicatorLabel.setText("(Re-Enrollment Mode)");
            modeIndicatorLabel.setVisible(true);
        }
        
        // Populate form with selected student data
        populateFormFromStudent(selected);
    }
    
    private void loadEligibleStudentsForReEnrollment() {
        if (schoolYearService == null || studentRepository == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("School year service is not available.");
            alert.showAndWait();
            return;
        }
        
        new Thread(() -> {
            try {
                // Get previous school year
                com.enrollment.system.model.SchoolYear previousSchoolYear = schoolYearService.getPreviousSchoolYearEntity();
                
                if (previousSchoolYear == null) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("No Previous School Year");
                        alert.setHeaderText(null);
                        alert.setContentText("No previous school year found. Re-enrollment is only available for students from the previous school year.");
                        alert.showAndWait();
                        handleCancelSelection();
                    });
                    return;
                }
                
                // Get all students from repository with section loaded
                List<Student> allStudentsEntity = studentRepository.findAllWithSectionByOrderByNameAsc();
                
                // Filter eligible students (Grade 11 or Grade 12 from previous school year, not archived)
                List<StudentDto> eligibleStudents = allStudentsEntity.stream()
                    .filter(student -> {
                        if (Boolean.TRUE.equals(student.getIsArchived())) {
                            return false;
                        }
                        if (student.getSchoolYear() == null) {
                            return false;
                        }
                        if (!student.getSchoolYear().getId().equals(previousSchoolYear.getId())) {
                            return false;
                        }
                        Integer gradeLevel = student.getGradeLevel();
                        return gradeLevel != null && (gradeLevel == 11 || gradeLevel == 12);
                    })
                    .map(student -> {
                        // Ensure section is loaded before converting to DTO
                        if (student.getSection() != null) {
                            student.getSection().getName(); // Trigger lazy load
                        }
                        return StudentDto.fromStudent(student);
                    })
                    .collect(Collectors.toList());
                
                Platform.runLater(() -> {
                    eligibleStudentsList = FXCollections.observableArrayList(eligibleStudents);
                    
                    // Create filtered list
                    filteredStudentsList = new FilteredList<>(eligibleStudentsList, p -> true);
                    
                    // Create sorted list
                    SortedList<StudentDto> sortedList = new SortedList<>(filteredStudentsList);
                    sortedList.comparatorProperty().bind(studentSelectionTable.comparatorProperty());
                    
                    if (studentSelectionTable != null) {
                        studentSelectionTable.setItems(sortedList);
                    }
                    
                    // Update filter combo boxes with available values
                    updateFilterComboBoxes(eligibleStudents);
                    
                    // Update results count
                    updateResultsCount();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Error loading eligible students: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    private void populateFormFromStudent(StudentDto student) {
        // Populate all fields with student data
        if (nameField != null) nameField.setText(student.getName());
        if (birthdatePicker != null) birthdatePicker.setValue(student.getBirthdate());
        if (ageField != null && student.getAge() != null) ageField.setText(String.valueOf(student.getAge()));
        if (sexComboBox != null) sexComboBox.setValue(student.getSex());
        if (addressField != null) addressField.setText(student.getAddress());
        if (contactNumberField != null) contactNumberField.setText(student.getContactNumber());
        if (parentGuardianNameField != null) parentGuardianNameField.setText(student.getParentGuardianName());
        if (parentGuardianContactField != null) parentGuardianContactField.setText(student.getParentGuardianContact());
        if (parentGuardianRelationshipComboBox != null) parentGuardianRelationshipComboBox.setValue(student.getParentGuardianRelationship());
        
        // Set grade level - Grade 11 becomes 12, Grade 12 stays 12
        // For archived students, allow them to stay in Grade 11 if needed
        Integer currentGrade = student.getGradeLevel();
        Long currentSectionId = student.getSectionId();
        if (gradeLevelComboBox != null) {
            if (currentGrade != null && currentGrade == 11) {
                // For archived students re-enrolling, default to Grade 12 but allow Grade 11
                // The user can change it if needed
                gradeLevelComboBox.setValue(12); // Default to Grade 12 (promotion)
            } else if (currentGrade != null && currentGrade == 12) {
                gradeLevelComboBox.setValue(12); // Stay at Grade 12
                // Show re-enrollment reason field for Grade 12 (always required)
                if (reEnrollmentReasonSection != null) {
                    reEnrollmentReasonSection.setVisible(true);
                    reEnrollmentReasonSection.setManaged(true);
                }
            }
        }
        
        // Check if re-enrollment reason field should be shown
        // For Grade 12: always show (always required)
        // For Grade 11: show only if re-enrolling to same grade and section
        if (currentMode == Mode.RE_ENROLL) {
            // Add listeners to dynamically show/hide reason field based on selections
            if (gradeLevelComboBox != null) {
                gradeLevelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    checkAndShowReEnrollmentReasonField(newValue, currentSectionId);
                });
            }
            
            if (sectionComboBox != null) {
                sectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    Integer currentGradeValue = gradeLevelComboBox != null ? gradeLevelComboBox.getValue() : null;
                    Long newSectionId = newValue != null ? newValue.getId() : null;
                    checkAndShowReEnrollmentReasonField(currentGradeValue, currentSectionId, newSectionId);
                });
            }
            
            // Initial check
            checkAndShowReEnrollmentReasonField(gradeLevelComboBox != null ? gradeLevelComboBox.getValue() : null, currentSectionId);
        }
        
        if (strandComboBox != null) strandComboBox.setValue(student.getStrand());
        if (previousSchoolField != null) previousSchoolField.setText(student.getPreviousSchool());
        if (gwaField != null && student.getGwa() != null) gwaField.setText(String.valueOf(student.getGwa()));
        if (lrnField != null) lrnField.setText(student.getLrn());
        if (enrollmentStatusComboBox != null) enrollmentStatusComboBox.setValue("Enrolled");
        
        // Set semester if available
        if (semesterComboBox != null && student.getSemesterDisplayName() != null && !student.getSemesterDisplayName().isEmpty()) {
            semesterComboBox.setValue(student.getSemesterDisplayName());
        }
        
        // Update sections based on grade and strand
        updateSections();
    }
    
    /**
     * Public method to set up the form for re-enrollment from an archived student.
     * This bypasses the choice screen and directly shows the form with pre-filled data.
     * 
     * @param student The archived student to re-enroll
     */
    public void setupReEnrollmentFromArchived(StudentDto student) {
        // Set mode to re-enrollment
        currentMode = Mode.RE_ENROLL;
        selectedStudentForReEnrollment = student;
        
        // Hide choice screen and student selection screen, show form
        if (choiceScreen != null) {
            choiceScreen.setVisible(false);
            choiceScreen.setManaged(false);
        }
        if (studentSelectionScreen != null) {
            studentSelectionScreen.setVisible(false);
            studentSelectionScreen.setManaged(false);
        }
        if (formScrollPane != null) {
            formScrollPane.setVisible(true);
            formScrollPane.setManaged(true);
        }
        if (formContainer != null) {
            formContainer.setVisible(true);
            formContainer.setManaged(true);
        }
        
        // Update form title and show mode indicator
        if (formTitleLabel != null) {
            formTitleLabel.setText("Re-Enroll Student");
        }
        if (modeIndicatorLabel != null) {
            modeIndicatorLabel.setText("(Re-Enrollment Mode)");
            modeIndicatorLabel.setVisible(true);
        }
        
        // Ensure semesters are loaded before populating form
        // If semesters haven't been loaded yet, wait for them
        if (allSemesters == null || allSemesters.isEmpty()) {
            // Load semesters first, then populate form
            loadSemesters();
            // Wait a bit for semesters to load, then populate
            new Thread(() -> {
                try {
                    // Wait up to 2 seconds for semesters to load
                    int attempts = 0;
                    while ((allSemesters == null || allSemesters.isEmpty()) && attempts < 20) {
                        Thread.sleep(100);
                        attempts++;
                    }
                    Platform.runLater(() -> {
                        populateFormFromStudent(student);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> {
                        populateFormFromStudent(student);
                    });
                }
            }).start();
        } else {
            // Semesters already loaded, populate form immediately
            Platform.runLater(() -> {
                populateFormFromStudent(student);
            });
        }
    }
    
    private void checkAndShowReEnrollmentReasonField(Integer gradeLevel, Long originalSectionId) {
        Long currentSectionId = sectionComboBox != null && sectionComboBox.getValue() != null ? 
            sectionComboBox.getValue().getId() : null;
        checkAndShowReEnrollmentReasonField(gradeLevel, originalSectionId, currentSectionId);
    }
    
    private void checkAndShowReEnrollmentReasonField(Integer gradeLevel, Long originalSectionId, Long currentSectionId) {
        if (currentMode != Mode.RE_ENROLL || reEnrollmentReasonSection == null) {
            return;
        }
        
        // Show reason field if:
        // 1. Grade 12 re-enrolling to Grade 12 (always required)
        // 2. Grade 11 re-enrolling to Grade 11 AND same section
        boolean shouldShow = false;
        
        if (gradeLevel != null && gradeLevel == 12) {
            // Grade 12 always needs reason
            shouldShow = true;
        } else if (gradeLevel != null && gradeLevel == 11) {
            // Grade 11 needs reason only if re-enrolling to same section
            if (originalSectionId != null && currentSectionId != null && 
                originalSectionId.equals(currentSectionId)) {
                shouldShow = true;
            }
        }
        
        reEnrollmentReasonSection.setVisible(shouldShow);
        reEnrollmentReasonSection.setManaged(shouldShow);
    }
    
    private void clearAllFields() {
        if (nameField != null) nameField.clear();
        if (birthdatePicker != null) birthdatePicker.setValue(null);
        if (ageField != null) ageField.clear();
        if (sexComboBox != null) sexComboBox.setValue(null);
        if (addressField != null) addressField.clear();
        if (contactNumberField != null) contactNumberField.clear();
        if (parentGuardianNameField != null) parentGuardianNameField.clear();
        if (parentGuardianContactField != null) parentGuardianContactField.clear();
        if (parentGuardianRelationshipComboBox != null) parentGuardianRelationshipComboBox.setValue(null);
        if (gradeLevelComboBox != null) gradeLevelComboBox.setValue(null);
        if (strandComboBox != null) strandComboBox.setValue(null);
        if (sectionComboBox != null) sectionComboBox.setValue(null);
        if (previousSchoolField != null) previousSchoolField.clear();
        if (gwaField != null) gwaField.clear();
        if (lrnField != null) lrnField.clear();
        if (enrollmentStatusComboBox != null) enrollmentStatusComboBox.setValue("Pending");
        if (semesterComboBox != null) semesterComboBox.setValue(null);
        if (reEnrollmentReasonField != null) reEnrollmentReasonField.clear();
    }
    
    private void loadSemesters() {
        if (semesterService == null || semesterComboBox == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                // Load ALL semesters from ALL school years (for Add Student page)
                allSemesters = semesterService.getAllSemestersForDropdown();
                
                System.out.println("AddStudentController: Loaded " + allSemesters.size() + " semesters for dropdown");
                
                Platform.runLater(() -> {
                    // Filter semesters based on selected grade level (if any)
                    updateSemestersByGradeLevel(gradeLevelComboBox.getValue());
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    System.err.println("Error loading semesters: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void updateSemestersByGradeLevel(Integer gradeLevel) {
        if (semesterComboBox == null || allSemesters == null || allSemesters.isEmpty()) {
            return;
        }
        
        try {
            String currentSelection = semesterComboBox.getValue();
            semesterMap.clear();
            semesterToSchoolYearMap.clear();
            semesterComboBox.getItems().clear();
            
            // Filter semesters by grade level if grade level is selected
            List<SemesterDto> filteredSemesters = allSemesters;
            if (gradeLevel != null) {
                filteredSemesters = allSemesters.stream()
                    .filter(s -> s.getGradeLevel() != null && s.getGradeLevel().equals(gradeLevel))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Use a Set to track unique display names to avoid duplicates
            java.util.Set<String> seenDisplayNames = new java.util.HashSet<>();
            
            for (SemesterDto semester : filteredSemesters) {
                String displayName = semester.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    // Only add if we haven't seen this display name before
                    // This handles the case where multiple semesters have the same display name
                    // (e.g., same school year + semester number but different grade levels)
                    if (!seenDisplayNames.contains(displayName)) {
                        semesterComboBox.getItems().add(displayName);
                        seenDisplayNames.add(displayName);
                        // Store the first semester ID for this display name
                        // When saving, we'll match it to the correct semester based on grade level
                        semesterMap.put(displayName, semester.getId());
                        if (semester.getSchoolYearId() != null) {
                            semesterToSchoolYearMap.put(semester.getId(), semester.getSchoolYearId());
                        }
                        System.out.println("AddStudentController: Added semester: " + displayName + " (Grade " + semester.getGradeLevel() + ")");
                    }
                }
            }
            
            // Clear selection if current selection is no longer valid
            if (currentSelection != null && !semesterComboBox.getItems().contains(currentSelection)) {
                semesterComboBox.setValue(null);
            }
            
            // Set cell factory for proper display
            semesterComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
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
            semesterComboBox.setButtonCell(new javafx.scene.control.ListCell<String>() {
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
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error populating semester dropdown: " + e.getMessage());
        }
    }
    
    private void setupSearchAndFilters() {
        // Search field listener
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
        
        // Filter combo box listeners
        if (filterGradeCombo != null) {
            filterGradeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
        
        if (filterStrandCombo != null) {
            filterStrandCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
        
        if (filterSectionCombo != null) {
            filterSectionCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
    }
    
    private void applyFilters() {
        if (filteredStudentsList == null) {
            return;
        }
        
        filteredStudentsList.setPredicate(student -> {
            // Search filter - search across all student information (numbers and letters)
            String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
            if (!searchText.isEmpty()) {
                boolean matchesSearch = 
                    // Name
                    (student.getName() != null && student.getName().toLowerCase().contains(searchText)) ||
                    // Grade level (as string - works for both "11" and "12")
                    (student.getGradeLevel() != null && String.valueOf(student.getGradeLevel()).contains(searchText)) ||
                    // Strand
                    (student.getStrand() != null && student.getStrand().toLowerCase().contains(searchText)) ||
                    // Section name
                    (student.getSectionName() != null && student.getSectionName().toLowerCase().contains(searchText)) ||
                    // LRN
                    (student.getLrn() != null && student.getLrn().toLowerCase().contains(searchText)) ||
                    // Contact number
                    (student.getContactNumber() != null && student.getContactNumber().contains(searchText)) ||
                    // Address
                    (student.getAddress() != null && student.getAddress().toLowerCase().contains(searchText)) ||
                    // Previous school
                    (student.getPreviousSchool() != null && student.getPreviousSchool().toLowerCase().contains(searchText)) ||
                    // GWA (as string - works for numbers like "85.5")
                    (student.getGwa() != null && String.valueOf(student.getGwa()).contains(searchText)) ||
                    // Sex
                    (student.getSex() != null && student.getSex().toLowerCase().contains(searchText)) ||
                    // Parent/Guardian name
                    (student.getParentGuardianName() != null && student.getParentGuardianName().toLowerCase().contains(searchText)) ||
                    // Parent/Guardian contact
                    (student.getParentGuardianContact() != null && student.getParentGuardianContact().contains(searchText));
                if (!matchesSearch) {
                    return false;
                }
            }
            
            // Grade level filter
            Integer filterGrade = filterGradeCombo != null ? filterGradeCombo.getValue() : null;
            if (filterGrade != null) {
                if (student.getGradeLevel() == null || !student.getGradeLevel().equals(filterGrade)) {
                    return false;
                }
            }
            
            // Strand filter
            String filterStrand = filterStrandCombo != null ? filterStrandCombo.getValue() : null;
            if (filterStrand != null && !filterStrand.isEmpty()) {
                if (student.getStrand() == null || !student.getStrand().equals(filterStrand)) {
                    return false;
                }
            }
            
            // Section filter
            String filterSection = filterSectionCombo != null ? filterSectionCombo.getValue() : null;
            if (filterSection != null && !filterSection.isEmpty()) {
                if (student.getSectionName() == null || !student.getSectionName().equals(filterSection)) {
                    return false;
                }
            }
            
            return true;
        });
        
        updateResultsCount();
    }
    
    private void updateFilterComboBoxes(List<StudentDto> students) {
        if (students == null || students.isEmpty()) {
            return;
        }
        
        // Update strand combo box
        if (filterStrandCombo != null) {
            List<String> strands = students.stream()
                .map(StudentDto::getStrand)
                .filter(strand -> strand != null && !strand.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            String currentValue = filterStrandCombo.getValue();
            filterStrandCombo.getItems().clear();
            filterStrandCombo.getItems().add(null); // Add "All" option
            filterStrandCombo.getItems().addAll(strands);
            if (currentValue != null && strands.contains(currentValue)) {
                filterStrandCombo.setValue(currentValue);
            }
        }
        
        // Update section combo box
        if (filterSectionCombo != null) {
            List<String> sections = students.stream()
                .map(StudentDto::getSectionName)
                .filter(section -> section != null && !section.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            String currentValue = filterSectionCombo.getValue();
            filterSectionCombo.getItems().clear();
            filterSectionCombo.getItems().add(null); // Add "All" option
            filterSectionCombo.getItems().addAll(sections);
            if (currentValue != null && sections.contains(currentValue)) {
                filterSectionCombo.setValue(currentValue);
            }
        }
    }
    
    private void updateResultsCount() {
        if (resultsCountLabel == null || filteredStudentsList == null || eligibleStudentsList == null) {
            return;
        }
        
        int filteredCount = filteredStudentsList.size();
        int totalCount = eligibleStudentsList.size();
        
        if (filteredCount == totalCount) {
            resultsCountLabel.setText("Showing " + totalCount + " student(s)");
        } else {
            resultsCountLabel.setText("Showing " + filteredCount + " of " + totalCount + " student(s)");
        }
    }
    
    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
    }
    
    @FXML
    private void handleClearFilters() {
        if (filterGradeCombo != null) {
            filterGradeCombo.setValue(null);
        }
        if (filterStrandCombo != null) {
            filterStrandCombo.setValue(null);
        }
        if (filterSectionCombo != null) {
            filterSectionCombo.setValue(null);
        }
        if (searchField != null) {
            searchField.clear();
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
    
    private int calculateAge(LocalDate birthdate) {
        if (birthdate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - birthdate.getYear() - 
               (LocalDate.now().getDayOfYear() < birthdate.getDayOfYear() ? 1 : 0);
    }
    
    private boolean isAllCapitalAbbreviation(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        
        // Exception: STI is allowed (it's a real school name)
        if (trimmed.equalsIgnoreCase("STI")) {
            return false;
        }
        
        // Check if text is all uppercase or all lowercase and contains only letters (no spaces or special chars)
        // Also check if it's short (likely an abbreviation)
        if (trimmed.length() <= 10 && (trimmed.matches("^[A-Z]+$") || trimmed.matches("^[a-z]+$"))) {
            return true;
        }
        
        // Check if text is all uppercase or all lowercase with spaces but still looks like abbreviation (e.g., "S N H S" or "s n h s")
        if ((trimmed.matches("^[A-Z\\s]+$") || trimmed.matches("^[a-z\\s]+$")) && trimmed.length() <= 15) {
            // Count words - if more than 3 words, probably not an abbreviation
            String[] words = trimmed.split("\\s+");
            if (words.length <= 3 && trimmed.length() <= 15) {
                // Check if average word length is very short (likely abbreviation)
                int totalLength = 0;
                for (String word : words) {
                    totalLength += word.length();
                }
                double avgLength = (double) totalLength / words.length;
                if (avgLength <= 3) {
                    return true;
                }
            }
        }
        
        // Check for patterns like "SNHS", "snhs", "S.N.H.S.", "S-N-H-S", "s.n.h.s"
        if (trimmed.matches("^[A-Za-z]{2,10}([.\\-\\s][A-Za-z]{1,5}){0,5}$")) {
            // Remove separators and check if it's all letters and short
            String withoutSeparators = trimmed.replaceAll("[.\\-\\s]", "");
            if (withoutSeparators.length() <= 10 && (withoutSeparators.matches("^[A-Z]+$") || withoutSeparators.matches("^[a-z]+$"))) {
                // Exception: STI is allowed (case-insensitive)
                if (!withoutSeparators.equalsIgnoreCase("STI")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void highlightField(TextField field) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
    }
    
    private void highlightField(DatePicker field) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
    }
    
    private void highlightComboBox(ComboBox<?> comboBox) {
        comboBox.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
    }
    
    private void clearFieldStyles() {
        nameField.setStyle("");
        birthdatePicker.setStyle("");
        ageField.setStyle("");
        sexComboBox.setStyle("");
        addressField.setStyle("");
        contactNumberField.setStyle("");
        parentGuardianNameField.setStyle("");
        parentGuardianContactField.setStyle("");
        parentGuardianRelationshipComboBox.setStyle("");
        gradeLevelComboBox.setStyle("");
        strandComboBox.setStyle("");
        previousSchoolField.setStyle("");
        gwaField.setStyle("");
        lrnField.setStyle("");
        enrollmentStatusComboBox.setStyle("");
        if (semesterComboBox != null) semesterComboBox.setStyle("");
    }
}

