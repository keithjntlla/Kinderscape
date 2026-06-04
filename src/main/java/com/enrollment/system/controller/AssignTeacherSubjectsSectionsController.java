package com.enrollment.system.controller;

import com.enrollment.system.dto.UserDto;
import com.enrollment.system.model.Subject;
import com.enrollment.system.model.Section;
import com.enrollment.system.model.Strand;
import com.enrollment.system.service.TeacherService;
import com.enrollment.system.service.SubjectService;
import com.enrollment.system.service.SectionService;
import com.enrollment.system.service.StrandService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AssignTeacherSubjectsSectionsController implements Initializable {
    
    // Assignment record for table display
    public static class AssignmentRecord {
        private Subject subject;
        private Section section;
        
        public AssignmentRecord(Subject subject, Section section) {
            this.subject = subject;
            this.section = section;
        }
        
        public Subject getSubject() { return subject; }
        public Section getSection() { return section; }
        public String getSubjectName() { return subject != null ? subject.getName() : ""; }
        public String getSectionName() { return section != null ? section.getName() : ""; }
        public String getStrand() { return section != null ? section.getStrand() : ""; }
        public Integer getGrade() { return section != null ? section.getGradeLevel() : null; }
    }
    
    @FXML
    private Label teacherNameLabel;
    
    @FXML
    private TableView<AssignmentRecord> assignmentsTable;
    
    @FXML
    private TableColumn<AssignmentRecord, String> subjectColumn;
    
    @FXML
    private TableColumn<AssignmentRecord, String> sectionColumn;
    
    @FXML
    private TableColumn<AssignmentRecord, String> strandColumn;
    
    @FXML
    private TableColumn<AssignmentRecord, Integer> gradeColumn;
    
    @FXML
    private TableColumn<AssignmentRecord, String> actionsColumn;
    
    @FXML
    private Label assignmentsCountLabel;
    
    @FXML
    private ComboBox<Subject> subjectComboBox;
    
    @FXML
    private ComboBox<Section> sectionComboBox;
    
    @FXML
    private ComboBox<String> strandComboBox;
    
    @FXML
    private ComboBox<Integer> gradeComboBox;
    
    @FXML
    private Button addSubjectButton;
    
    @FXML
    private Button editSubjectButton;
    
    @FXML
    private Button deleteSubjectButton;
    
    @FXML
    private Button assignButton;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button clearAllButton;
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private SectionService sectionService;
    
    @Autowired
    private StrandService strandService;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    private UserDto teacher;
    private ObservableList<Subject> allSubjects;
    private ObservableList<Section> allSections;
    private ObservableList<String> allStrands;
    private ObservableList<AssignmentRecord> assignments;
    
    // Debouncing and synchronization for save operations
    private java.util.Timer saveTimer;
    private final Object saveLock = new Object();
    private volatile boolean isSaving = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize table columns
        subjectColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSubjectName()));
        sectionColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSectionName()));
        strandColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStrand()));
        gradeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getGrade()).asObject());
        
        // Actions column with remove button
        actionsColumn.setCellFactory(column -> new TableCell<AssignmentRecord, String>() {
            private final Button removeButton = new Button("Remove");
            
            {
                removeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand;");
                removeButton.setOnAction(e -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        AssignmentRecord record = getTableView().getItems().get(index);
                        if (record != null) {
                            handleRemoveAssignment(record, index);
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        });
        
        // Set up button actions
        addSubjectButton.setOnAction(e -> handleAddSubject());
        editSubjectButton.setOnAction(e -> handleEditSubject());
        deleteSubjectButton.setOnAction(e -> handleDeleteSelectedSubject());
        assignButton.setOnAction(e -> handleAssign());
        cancelButton.setOnAction(e -> handleCancel());
        if (clearAllButton != null) {
            clearAllButton.setOnAction(e -> handleClearAllAssignments());
        }
        
        // Set up combo box cell factories for Subject
        if (subjectComboBox != null) {
            subjectComboBox.setCellFactory(listView -> new ListCell<Subject>() {
                @Override
                protected void updateItem(Subject subject, boolean empty) {
                    super.updateItem(subject, empty);
                    if (empty || subject == null) {
                        setText(null);
                    } else {
                        String display = subject.getName() != null ? subject.getName() : "";
                        if (subject.getGradeLevel() != null) {
                            display += " (Grade " + subject.getGradeLevel() + ")";
                        }
                        setText(display);
                    }
                }
            });
            
            subjectComboBox.setButtonCell(new ListCell<Subject>() {
                @Override
                protected void updateItem(Subject subject, boolean empty) {
                    super.updateItem(subject, empty);
                    if (empty || subject == null) {
                        setText(null);
                    } else {
                        String display = subject.getName() != null ? subject.getName() : "";
                        if (subject.getGradeLevel() != null) {
                            display += " (Grade " + subject.getGradeLevel() + ")";
                        }
                        setText(display);
                    }
                }
            });
        }
        
        // Set up combo box cell factories for Section
        if (sectionComboBox != null) {
            sectionComboBox.setCellFactory(listView -> new ListCell<Section>() {
                @Override
                protected void updateItem(Section section, boolean empty) {
                    super.updateItem(section, empty);
                    if (empty || section == null) {
                        setText(null);
                    } else {
                        String name = section.getName() != null ? section.getName() : "";
                        String strand = section.getStrand() != null ? section.getStrand() : "";
                        Integer grade = section.getGradeLevel();
                        setText(name + " (" + strand + " - Grade " + grade + ")");
                    }
                }
            });
            
            sectionComboBox.setButtonCell(new ListCell<Section>() {
                @Override
                protected void updateItem(Section section, boolean empty) {
                    super.updateItem(section, empty);
                    if (empty || section == null) {
                        setText(null);
                    } else {
                        String name = section.getName() != null ? section.getName() : "";
                        String strand = section.getStrand() != null ? section.getStrand() : "";
                        Integer grade = section.getGradeLevel();
                        setText(name + " (" + strand + " - Grade " + grade + ")");
                    }
                }
            });
        }
        
        // Initialize grade ComboBox with values 11 and 12
        if (gradeComboBox != null) {
            gradeComboBox.setItems(FXCollections.observableArrayList(11, 12));
        }
        
        // Update sections when strand or grade changes
        strandComboBox.setOnAction(e -> updateSectionsByStrandAndGrade());
        gradeComboBox.setOnAction(e -> {
            updateSectionsByStrandAndGrade();
            updateSubjectsByGrade(); // Filter subjects by selected grade
        });
        
        // Update grade ComboBox when section changes (auto-select matching grade)
        sectionComboBox.setOnAction(e -> {
            updateGradeFromSection();
            updateSubjectsByGrade(); // Filter subjects when section changes
        });
        
        // Validate subject-section grade match when subject is selected
        subjectComboBox.setOnAction(e -> {
            Subject selectedSubject = subjectComboBox.getValue();
            Section selectedSection = sectionComboBox.getValue();
            
            if (selectedSubject != null && selectedSection != null) {
                Integer subjectGrade = selectedSubject.getGradeLevel();
                Integer sectionGrade = selectedSection.getGradeLevel();
                
                if (subjectGrade != null && sectionGrade != null && !subjectGrade.equals(sectionGrade)) {
                    // Clear subject selection and show warning
                    subjectComboBox.setValue(null);
                    showError("Grade mismatch: Selected subject is for Grade " + subjectGrade + 
                             " but selected section is for Grade " + sectionGrade + 
                             ". Please select a subject that matches the section's grade level.");
                }
            }
        });
        
        // Initialize assignments list
        assignments = FXCollections.observableArrayList();
        assignmentsTable.setItems(assignments);
        
        // Initialize empty lists to prevent null pointer exceptions
        allSubjects = FXCollections.observableArrayList();
        allSections = FXCollections.observableArrayList();
        allStrands = FXCollections.observableArrayList();
        
        // Load data after a short delay to ensure UI is ready
        Platform.runLater(() -> {
            // Ensure subjects have proper isActive status first
            try {
                subjectService.ensureAllSubjectsHaveActiveStatus();
            } catch (Exception e) {
                System.err.println("Warning: Could not ensure subject active status: " + e.getMessage());
            }
            
            loadSubjects();
            loadSections();
            loadStrands();
        });
    }
    
    public void setTeacher(UserDto teacher) {
        this.teacher = teacher;
        if (teacherNameLabel != null && teacher != null) {
            teacherNameLabel.setText("Teacher: " + teacher.getFullName() + " (" + teacher.getUsername() + ")");
        }
        
        // Load teacher's current assignments
        if (teacher != null) {
            // Check if there are too many assignments and warn user
            new Thread(() -> {
                try {
                    long totalAssignmentCount = teacherService.getTotalAssignmentCount(teacher.getId());
                    if (totalAssignmentCount > 8) {
                        Platform.runLater(() -> {
                            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                            warningAlert.setTitle("Too Many Assignments");
                            warningAlert.setHeaderText("Assignment Limit Exceeded");
                            warningAlert.setContentText("This teacher currently has " + totalAssignmentCount + " total assignments (subject-section pairs), " +
                                                       "which exceeds the maximum limit of 8.\n\n" +
                                                       "Please use 'Clear All' to remove all assignments, then reassign (maximum 8 total assignments).");
                            warningAlert.showAndWait();
                        });
                    }
                } catch (Exception e) {
                    // Ignore - just load assignments normally
                }
                Platform.runLater(() -> loadTeacherAssignments());
            }).start();
        }
    }
    
    private void loadSubjects() {
        // Check if service is injected
        if (subjectService == null) {
            System.err.println("ERROR: subjectService is null! Spring injection may have failed.");
            Platform.runLater(() -> {
                showError("Subject service not available. Please restart the application.");
            });
            return;
        }
        
        // Run on background thread to avoid blocking UI
        new Thread(() -> {
            try {
                System.out.println("Loading subjects from database...");
                
                // Try to get all subjects first, then filter active ones
                // This handles cases where isActive might be NULL in SQLite
                List<Subject> allSubjectsList = subjectService.getAllSubjects();
                System.out.println("Found " + allSubjectsList.size() + " total subjects in database");
                
                // Filter active subjects (handle NULL isActive values)
                List<Subject> subjects = allSubjectsList.stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                    .collect(Collectors.toList());
                
                System.out.println("Found " + subjects.size() + " active subjects after filtering");
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    try {
                        if (subjects == null || subjects.isEmpty()) {
                            System.out.println("WARNING: No active subjects found in database!");
                            allSubjects = FXCollections.observableArrayList();
                            
                            // Show helpful message to user
                            System.out.println("INFO: You can add subjects using the 'Add Subject' button");
                        } else {
                            allSubjects = FXCollections.observableArrayList(subjects);
                            
                            // Sort by creation date descending (newest first), then by grade level, then by name
                            allSubjects.sort((s1, s2) -> {
                                // First, sort by creation date (newest first)
                                if (s1.getCreatedAt() != null && s2.getCreatedAt() != null) {
                                    int dateCompare = s2.getCreatedAt().compareTo(s1.getCreatedAt());
                                    if (dateCompare != 0) return dateCompare;
                                } else if (s1.getCreatedAt() != null) {
                                    return -1; // s1 has date, s2 doesn't - s1 comes first
                                } else if (s2.getCreatedAt() != null) {
                                    return 1; // s2 has date, s1 doesn't - s2 comes first
                                }
                                
                                // If dates are equal or both null, sort by grade level
                                int gradeCompare = Integer.compare(
                                    s1.getGradeLevel() != null ? s1.getGradeLevel() : 0,
                                    s2.getGradeLevel() != null ? s2.getGradeLevel() : 0
                                );
                                if (gradeCompare != 0) return gradeCompare;
                                
                                // Finally, sort by name
                                String name1 = s1.getName() != null ? s1.getName() : "";
                                String name2 = s2.getName() != null ? s2.getName() : "";
                                return name1.compareToIgnoreCase(name2);
                            });
                        }
                        
                        if (subjectComboBox != null) {
                            subjectComboBox.setItems(allSubjects);
                            System.out.println("Set " + allSubjects.size() + " subjects into dropdown ComboBox");
                            System.out.println("Subject ComboBox items count: " + subjectComboBox.getItems().size());
                            
                            if (allSubjects.isEmpty()) {
                                System.out.println("WARNING: No subjects found in database!");
                            } else {
                                // Print sample subjects for debugging
                                System.out.println("Sample subjects loaded:");
                                int count = 0;
                                for (Subject s : allSubjects) {
                                    if (count >= 5) break;
                                    System.out.println("  - " + s.getName() + " (Grade " + s.getGradeLevel() + ")");
                                    count++;
                                }
                            }
                        } else {
                            System.out.println("ERROR: subjectComboBox is null!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Exception in loadSubjects Platform.runLater: " + e.getMessage());
                        showError("Error updating subjects dropdown: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Exception in loadSubjects background thread: " + e.getMessage());
                Platform.runLater(() -> showError("Error loading subjects: " + e.getMessage()));
            }
        }).start();
    }
    
    private void loadSections() {
        // Run on background thread to avoid blocking UI
        new Thread(() -> {
            try {
                System.out.println("Loading sections from database...");
                List<Section> sections = sectionService.getActiveSections();
                System.out.println("Found " + sections.size() + " active sections in database");
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    try {
                        if (sections == null || sections.isEmpty()) {
                            System.out.println("WARNING: No sections returned from service!");
                            allSections = FXCollections.observableArrayList();
                        } else {
                            allSections = FXCollections.observableArrayList(sections);
                            
                            // Sort by strand, then grade, then name
                            allSections.sort((s1, s2) -> {
                                String strand1 = s1.getStrand() != null ? s1.getStrand() : "";
                                String strand2 = s2.getStrand() != null ? s2.getStrand() : "";
                                int strandCompare = strand1.compareToIgnoreCase(strand2);
                                if (strandCompare != 0) return strandCompare;
                                
                                int gradeCompare = Integer.compare(
                                    s1.getGradeLevel() != null ? s1.getGradeLevel() : 0,
                                    s2.getGradeLevel() != null ? s2.getGradeLevel() : 0
                                );
                                if (gradeCompare != 0) return gradeCompare;
                                
                                String name1 = s1.getName() != null ? s1.getName() : "";
                                String name2 = s2.getName() != null ? s2.getName() : "";
                                return name1.compareToIgnoreCase(name2);
                            });
                        }
                        
                        // Initially show all sections
                        if (sectionComboBox != null) {
                            sectionComboBox.setItems(allSections);
                            System.out.println("Set " + allSections.size() + " sections into dropdown ComboBox");
                            System.out.println("Section ComboBox items count: " + sectionComboBox.getItems().size());
                            
                            if (allSections.isEmpty()) {
                                System.out.println("WARNING: No sections found in database!");
                            } else {
                                // Print first few sections for debugging
                                System.out.println("Sample sections loaded:");
                                for (int i = 0; i < Math.min(3, allSections.size()); i++) {
                                    Section s = allSections.get(i);
                                    System.out.println("  - " + s.getName() + " (" + s.getStrand() + " - Grade " + s.getGradeLevel() + ")");
                                }
                            }
                        } else {
                            System.out.println("ERROR: sectionComboBox is null!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Exception in loadSections Platform.runLater: " + e.getMessage());
                        showError("Error updating sections dropdown: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Exception in loadSections background thread: " + e.getMessage());
                Platform.runLater(() -> showError("Error loading sections: " + e.getMessage()));
            }
        }).start();
    }
    
    private void loadStrands() {
        // Run on background thread to avoid blocking UI
        new Thread(() -> {
            try {
                System.out.println("Loading strands from database...");
                List<Strand> strands = strandService.getActiveStrands();
                System.out.println("Found " + strands.size() + " active strands in database");
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    try {
                        if (strands == null || strands.isEmpty()) {
                            System.out.println("WARNING: No strands returned from service!");
                            allStrands = FXCollections.observableArrayList();
                        } else {
                            allStrands = FXCollections.observableArrayList(
                                strands.stream()
                                    .map(Strand::getName)
                                    .filter(name -> name != null && !name.trim().isEmpty())
                                    .sorted()
                                    .collect(Collectors.toList())
                            );
                        }
                        
                        if (strandComboBox != null) {
                            strandComboBox.setItems(allStrands);
                            System.out.println("Set " + allStrands.size() + " strands into dropdown ComboBox");
                            System.out.println("Strand ComboBox items count: " + strandComboBox.getItems().size());
                            
                            if (allStrands.isEmpty()) {
                                System.out.println("WARNING: No strands found in database!");
                            } else {
                                // Print all strands for debugging
                                System.out.println("Strands loaded:");
                                for (String strand : allStrands) {
                                    System.out.println("  - " + strand);
                                }
                            }
                        } else {
                            System.out.println("ERROR: strandComboBox is null!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("Exception in loadStrands Platform.runLater: " + e.getMessage());
                        showError("Error updating strands dropdown: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Exception in loadStrands background thread: " + e.getMessage());
                Platform.runLater(() -> showError("Error loading strands: " + e.getMessage()));
            }
        }).start();
    }
    
    private void updateSectionsByStrandAndGrade() {
        if (allSections == null) {
            return; // Sections not loaded yet
        }
        
        String selectedStrand = strandComboBox.getValue();
        Integer selectedGrade = gradeComboBox.getValue();
        
        // Filter sections by strand and grade
        ObservableList<Section> filtered = FXCollections.observableArrayList(
            allSections.stream()
                .filter(s -> {
                    boolean matchesStrand = selectedStrand == null || selectedStrand.isEmpty() || 
                                          (s.getStrand() != null && selectedStrand.equals(s.getStrand()));
                    boolean matchesGrade = selectedGrade == null || 
                                         (s.getGradeLevel() != null && selectedGrade.equals(s.getGradeLevel()));
                    return matchesStrand && matchesGrade;
                })
                .collect(Collectors.toList())
        );
        sectionComboBox.setItems(filtered);
    }
    
    private void updateGradeFromSection() {
        Section selectedSection = sectionComboBox.getValue();
        if (selectedSection != null && selectedSection.getGradeLevel() != null) {
            // Auto-select the grade in the ComboBox
            gradeComboBox.setValue(selectedSection.getGradeLevel());
        }
    }
    
    private void updateSubjectsByGrade() {
        if (allSubjects == null || allSubjects.isEmpty()) {
            return; // Subjects not loaded yet
        }
        
        Integer selectedGrade = gradeComboBox.getValue();
        
        // Filter subjects by grade level
        ObservableList<Subject> filtered;
        if (selectedGrade != null) {
            // Only show subjects for the selected grade
            filtered = FXCollections.observableArrayList(
                allSubjects.stream()
                    .filter(s -> s.getGradeLevel() != null && s.getGradeLevel().equals(selectedGrade))
                    .collect(Collectors.toList())
            );
        } else {
            // If no grade selected, show all subjects
            filtered = allSubjects;
        }
        
        // Preserve current selection if it's still valid
        Subject currentSelection = subjectComboBox.getValue();
        subjectComboBox.setItems(filtered);
        
        // Restore selection if it's still in the filtered list
        if (currentSelection != null && filtered.contains(currentSelection)) {
            subjectComboBox.setValue(currentSelection);
        } else {
            subjectComboBox.setValue(null);
        }
    }
    
    private void loadTeacherAssignments() {
        if (teacher == null) {
            return;
        }
        
        try {
            // Load ONLY actual assignments from TeacherAssignment table - NO backward compatibility
            java.util.Map<Long, List<Section>> subjectSectionMap = teacherService.getSubjectSectionMap(teacher.getId());
            
            assignments.clear();
            
            // Only load from actual assignments - no fallback to old ManyToMany relationships
            if (!subjectSectionMap.isEmpty()) {
                // Load all subjects if not already loaded
                if (allSubjects == null || allSubjects.isEmpty()) {
                    loadSubjects();
                    // Wait a bit for subjects to load
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                for (Map.Entry<Long, List<Section>> entry : subjectSectionMap.entrySet()) {
                    Long subjectId = entry.getKey();
                    List<Section> sections = entry.getValue();
                    
                    // Find the subject from allSubjects or load it
                    Subject subject = null;
                    if (allSubjects != null && !allSubjects.isEmpty()) {
                        subject = allSubjects.stream()
                            .filter(s -> s.getId().equals(subjectId))
                            .findFirst()
                            .orElse(null);
                    }
                    
                    // If not found in allSubjects, load directly
                    if (subject == null) {
                        try {
                            subject = subjectService.getSubjectRepository().findById(subjectId).orElse(null);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    
                    if (subject != null) {
                        for (Section section : sections) {
                            assignments.add(new AssignmentRecord(subject, section));
                        }
                    }
                }
            }
            
            updateAssignmentsCount();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading teacher assignments: " + e.getMessage());
        }
    }
    
    private void updateAssignmentsCount() {
        int count = assignments.size();
        assignmentsCountLabel.setText("Total Assignments: " + count);
        
        // Enable/disable Clear All button based on assignment count
        if (clearAllButton != null) {
            clearAllButton.setDisable(count == 0);
        }
    }
    
    private void handleAddSubject() {
        // Check if service is available
        if (subjectService == null) {
            showError("Subject service not available. Please restart the application.");
            return;
        }
        
        // Create simple dialog to add new subject
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add New Subject");
        dialog.setHeaderText("Enter subject details");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Subject Name");
        ComboBox<Integer> gradeLevelCombo = new ComboBox<>(FXCollections.observableArrayList(11, 12));
        gradeLevelCombo.setValue(11);
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description (optional)");
        descriptionArea.setPrefRowCount(3);
        
        grid.add(new Label("Subject Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Grade Level:"), 0, 1);
        grid.add(gradeLevelCombo, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionArea, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Disable add button initially
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        // Enable add button when name is entered
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        Platform.runLater(() -> nameField.requestFocus());
        
        // Convert result when add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return nameField.getText().trim() + "|" + gradeLevelCombo.getValue() + "|" + descriptionArea.getText().trim();
            }
            return null;
        });
        
        // Handle result
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|", 3);
            String subjectName = parts[0];
            Integer gradeLevel = Integer.parseInt(parts[1]);
            String description = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            
            if (subjectName.isEmpty()) {
                showError("Subject name is required");
                return;
            }
            
            // Run in background thread with proper transaction management
            new Thread(() -> {
                try {
                    // Use TransactionTemplate to ensure proper transaction management in background thread
                    // This is critical - @Transactional doesn't work in background threads without this
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                    transactionTemplate.setTimeout(30);
                    
                    // Execute subject creation directly within transaction (like DataInitializer does)
                    // This avoids nested transaction issues and ensures @PrePersist fires
                    Subject newSubject = transactionTemplate.execute(status -> {
                        // Validate
                        if (subjectName == null || subjectName.trim().isEmpty()) {
                            throw new RuntimeException("Subject name is required");
                        }
                        if (gradeLevel == null || (gradeLevel != 11 && gradeLevel != 12)) {
                            throw new RuntimeException("Grade level must be 11 or 12");
                        }
                        
                        // Check for duplicates
                        if (subjectService.getSubjectRepository().existsByNameAndGradeLevel(subjectName.trim(), gradeLevel)) {
                            throw new RuntimeException("Subject '" + subjectName + "' already exists for Grade " + gradeLevel);
                        }
                        
                        // Create subject exactly like DataInitializer - let @PrePersist handle timestamps
                        Subject subject = new Subject();
                        subject.setName(subjectName.trim());
                        subject.setGradeLevel(gradeLevel);
                        subject.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
                        subject.setIsActive(true);
                        subject.setIsCustom(true); // Manually added subjects are custom
                        
                        // Save - @PrePersist will set timestamps automatically
                        Subject saved = subjectService.getSubjectRepository().save(subject);
                        
                        // Verify timestamps were set
                        if (saved.getCreatedAt() == null) {
                            // Fallback: set timestamps manually if @PrePersist didn't fire
                            java.time.LocalDateTime now = java.time.LocalDateTime.now();
                            saved.setCreatedAt(now);
                            saved.setUpdatedAt(now);
                            saved = subjectService.getSubjectRepository().save(saved);
                        }
                        
                        return saved;
                    });
                    
                    // Verify subject was created
                    if (newSubject == null || newSubject.getId() == null) {
                        throw new RuntimeException("Subject creation failed - no subject returned");
                    }
                    
                    final Subject createdSubject = newSubject;
                    
                    // Refresh dropdown on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            // Reload subjects from database
                            List<Subject> allSubjectsList = subjectService.getAllSubjects();
                            List<Subject> updatedSubjects = allSubjectsList.stream()
                                .filter(s -> s.getIsActive() != null && s.getIsActive())
                                .collect(Collectors.toList());
                            
                            allSubjects = FXCollections.observableArrayList(updatedSubjects);
                            
                            // Sort by creation date descending (newest first), then by grade level, then by name
                            allSubjects.sort((s1, s2) -> {
                                // First, sort by creation date (newest first)
                                if (s1.getCreatedAt() != null && s2.getCreatedAt() != null) {
                                    int dateCompare = s2.getCreatedAt().compareTo(s1.getCreatedAt());
                                    if (dateCompare != 0) return dateCompare;
                                } else if (s1.getCreatedAt() != null) {
                                    return -1; // s1 has date, s2 doesn't - s1 comes first
                                } else if (s2.getCreatedAt() != null) {
                                    return 1; // s2 has date, s1 doesn't - s2 comes first
                                }
                                
                                // If dates are equal or both null, sort by grade level
                                int gradeCompare = Integer.compare(
                                    s1.getGradeLevel() != null ? s1.getGradeLevel() : 0,
                                    s2.getGradeLevel() != null ? s2.getGradeLevel() : 0
                                );
                                if (gradeCompare != 0) return gradeCompare;
                                
                                // Finally, sort by name
                                String name1 = s1.getName() != null ? s1.getName() : "";
                                String name2 = s2.getName() != null ? s2.getName() : "";
                                return name1.compareToIgnoreCase(name2);
                            });
                            
                            // Update ComboBox - refresh list but don't auto-select
                            if (subjectComboBox != null) {
                                subjectComboBox.setItems(allSubjects);
                                subjectComboBox.setValue(null); // Clear selection to show "Select a subject"
                                System.out.println("✓ Subject added successfully: " + createdSubject.getName());
                            }
                            
                            showSuccess("Subject '" + createdSubject.getName() + "' added successfully!");
                            
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showError("Error refreshing subjects dropdown: " + ex.getMessage());
                        }
                    });
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errorMessage = ex.getMessage();
                    Platform.runLater(() -> {
                        if (errorMessage != null && errorMessage.contains("already exists")) {
                            showError("Subject '" + subjectName + "' already exists for Grade " + gradeLevel);
                        } else if (errorMessage != null && errorMessage.contains("NOT NULL")) {
                            showError("Error adding subject: Database constraint violation. Please check all required fields are provided.");
                        } else {
                            showError("Error adding subject: " + (errorMessage != null ? errorMessage : "Unknown error occurred"));
                        }
                    });
                }
            }).start();
        });
    }
    
    private void handleAssign() {
        if (teacher == null) {
            showError("No teacher selected");
            return;
        }
        
        // Prevent assigning while a save is in progress
        if (assignButton.isDisable() && "Saving...".equals(assignButton.getText())) {
            showError("Please wait for the current save operation to complete before assigning another subject.");
            return;
        }
        
        Subject selectedSubject = subjectComboBox.getValue();
        Section selectedSection = sectionComboBox.getValue();
        
        if (selectedSubject == null) {
            showError("Please select a subject");
            return;
        }
        
        if (selectedSection == null) {
            showError("Please select a section");
            return;
        }
        
        // Validate grade level match: Subject grade must match section grade
        Integer subjectGrade = selectedSubject.getGradeLevel();
        Integer sectionGrade = selectedSection.getGradeLevel();
        
        if (subjectGrade == null || sectionGrade == null) {
            showError("Both subject and section must have a grade level assigned");
            return;
        }
        
        if (!subjectGrade.equals(sectionGrade)) {
            showError("Grade level mismatch: Subject is for Grade " + subjectGrade + 
                     " but Section is for Grade " + sectionGrade + 
                     ". Subjects can only be assigned to sections with the same grade level.");
            return;
        }
        
        // Validate that entities have IDs (required for database operations)
        if (selectedSubject.getId() == null) {
            showError("Invalid subject: Subject does not have an ID. Please select a valid subject from the list.");
            return;
        }
        
        if (selectedSection.getId() == null) {
            showError("Invalid section: Section does not have an ID. Please select a valid section from the list.");
            return;
        }
        
        // Check if this exact assignment (same subject + same section) already exists for this teacher
        boolean exactAssignmentExists = assignments.stream()
            .anyMatch(a -> a.getSubject().getId().equals(selectedSubject.getId()) &&
                          a.getSection().getId().equals(selectedSection.getId()));
        
        if (exactAssignmentExists) {
            showError("This subject is already assigned to this section. " +
                     "You cannot assign the same subject to the same section twice.");
            return;
        }
        
        // Check if this subject-section combination is already assigned to a different teacher
        try {
            java.util.Optional<com.enrollment.system.model.TeacherAssignment> conflictingAssignment = 
                teacherService.findConflictingAssignment(selectedSubject.getId(), selectedSection.getId(), teacher.getId());
            
            if (conflictingAssignment.isPresent()) {
                com.enrollment.system.model.TeacherAssignment conflict = conflictingAssignment.get();
                String conflictingTeacherName = conflict.getTeacher() != null ? conflict.getTeacher().getFullName() : "Unknown Teacher";
                showError("This subject and section is already assigned to a different teacher: " + conflictingTeacherName + 
                         ".\n\nYou cannot assign the same subject-section combination to multiple teachers.");
                return;
            }
        } catch (Exception e) {
            // If check fails, log but don't block - allow the assignment to proceed
            System.err.println("Warning: Could not check for conflicting assignments: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Check maximum 8 total assignments (subject-section pairs) before adding
        // Always check against database first to get the most current count
        long currentTotalAssignmentCount = 0;
        try {
            currentTotalAssignmentCount = teacherService.getTotalAssignmentCount(teacher.getId());
        } catch (Exception e) {
            // If database check fails, fall back to local list check
            System.err.println("Warning: Could not get total assignment count from database: " + e.getMessage());
            currentTotalAssignmentCount = assignments.size();
        }
        
        // Check if adding this assignment would exceed the limit of 8
        // We need to check both database count and local list to account for unsaved assignments
        long totalAfterAdding = currentTotalAssignmentCount;
        
        // Check if this exact assignment already exists in database
        boolean existsInDb = false;
        try {
            Map<Long, List<Section>> dbAssignments = teacherService.getSubjectSectionMap(teacher.getId());
            List<Section> sectionsForSubject = dbAssignments.get(selectedSubject.getId());
            if (sectionsForSubject != null) {
                existsInDb = sectionsForSubject.stream()
                    .anyMatch(s -> s.getId().equals(selectedSection.getId()));
            }
        } catch (Exception e) {
            // If we can't check database, assume it doesn't exist
        }
        
        // If this assignment doesn't exist in database, it will be a new assignment
        if (!existsInDb) {
            totalAfterAdding = currentTotalAssignmentCount + 1;
        }
        
        // Also check local list to account for assignments added but not yet saved
        long localCount = assignments.size();
        if (localCount > currentTotalAssignmentCount) {
            // There are unsaved assignments in the local list
            totalAfterAdding = localCount + 1; // +1 for the new assignment we're about to add
        }
        
        if (totalAfterAdding > 8) {
            showError("A teacher can have a maximum of 8 total assignments (subject-section pairs). " +
                     "Current assignments: " + Math.max(currentTotalAssignmentCount, localCount) + 
                     ". Cannot add more. " +
                     "(Note: You can assign the same subject to multiple sections, but the total number of assignments cannot exceed 8)");
            return;
        }
        
        // Validate the entities one more time before creating the record
        if (selectedSubject.getId() == null) {
            showError("Cannot assign: Subject does not have a valid ID. Please select a different subject.");
            return;
        }
        if (selectedSection.getId() == null) {
            showError("Cannot assign: Section does not have a valid ID. Please select a different section.");
            return;
        }
        
        // Add to local list first (optimistic update)
        AssignmentRecord newRecord = new AssignmentRecord(selectedSubject, selectedSection);
        
        // Validate the record was created properly
        if (newRecord.getSubject() == null || newRecord.getSection() == null) {
            showError("Error creating assignment record. Please try again.");
            return;
        }
        
        assignments.add(newRecord);
        updateAssignmentsCount();
        
        // Clear selections
        subjectComboBox.setValue(null);
        sectionComboBox.setValue(null);
        strandComboBox.setValue(null);
        gradeComboBox.setValue(null);
        
        // Save to database in background with debouncing
        scheduleSave();
    }
    
    private void handleRemoveAssignment(AssignmentRecord record, int index) {
        if (record == null) {
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Remove Assignment");
        confirmAlert.setHeaderText("Remove Assignment");
        confirmAlert.setContentText("Are you sure you want to remove this assignment?\n\n" +
                                   "Subject: " + record.getSubjectName() + "\n" +
                                   "Section: " + record.getSectionName());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Remove by index to ensure proper removal
                if (index >= 0 && index < assignments.size()) {
                    assignments.remove(index);
                } else {
                    // Fallback: remove by matching IDs
                    assignments.removeIf(a -> 
                        a.getSubject().getId().equals(record.getSubject().getId()) &&
                        a.getSection().getId().equals(record.getSection().getId())
                    );
                }
                updateAssignmentsCount();
                scheduleSave();
            }
        });
    }
    
    private void handleClearAllAssignments() {
        if (assignments == null || assignments.isEmpty()) {
            showError("No assignments to clear.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear All Assignments");
        confirmAlert.setHeaderText("Clear All Assignments");
        confirmAlert.setContentText("Are you sure you want to remove ALL assigned subjects for this teacher?\n\n" +
                                   "This will remove " + assignments.size() + " assignment(s).\n" +
                                   "This action cannot be undone.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Clear all assignments
                assignments.clear();
                updateAssignmentsCount();
                scheduleSave();
            }
        });
    }
    
    /**
     * Schedules a save operation with debouncing to batch rapid changes.
     * This prevents multiple concurrent database operations when users add/remove quickly.
     */
    private void scheduleSave() {
        if (teacher == null) {
            return;
        }
        
        // Cancel any pending save timer
        synchronized (saveLock) {
            if (saveTimer != null) {
                saveTimer.cancel();
                saveTimer = null;
            }
            
            // Create new timer for debounced save (500ms delay)
            saveTimer = new java.util.Timer(true); // daemon thread
            saveTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    synchronized (saveLock) {
                        saveTimer = null;
                    }
                    saveAssignments();
                }
            }, 500); // Wait 500ms after last change before saving
        }
    }
    
    /**
     * Performs the actual save operation with synchronization to prevent concurrent saves.
     */
    private void saveAssignments() {
        if (teacher == null) {
            return;
        }
        
        // Synchronize to prevent concurrent saves
        synchronized (saveLock) {
            // Check if already saving
            if (isSaving) {
                // If already saving, reschedule for later
                scheduleSave();
                return;
            }
            
            isSaving = true;
        }
        
        // Disable assign button to prevent concurrent saves
        Platform.runLater(() -> {
            assignButton.setDisable(true);
            assignButton.setText("Saving...");
        });
        
        // Run database operation in background thread
        new Thread(() -> {
            try {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                transactionTemplate.setTimeout(15); // Reduced timeout for faster failure detection
                
                int maxRetries = 3; // Reduced retries since we have better locking now
                int baseRetryDelay = 100; // Start with shorter delay
                Exception lastException = null;
                
                // Capture current assignments list to save
                final List<AssignmentRecord> assignmentsToSave = new ArrayList<>(assignments);
                
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        transactionTemplate.execute(status -> {
                            // Validate capacity limit before saving - check total assignments (not unique subjects)
                            if (assignmentsToSave.size() > 8) {
                                throw new RuntimeException("A teacher can have a maximum of 8 total assignments (subject-section pairs). Found: " + assignmentsToSave.size());
                            }
                            
                            // Save actual assignments (subject-section pairs) - now uses incremental updates
                            teacherService.saveTeacherAssignments(teacher.getId(), assignmentsToSave);
                            
                            return null;
                        });
                        
                        lastException = null;
                        break;
                        
                    } catch (DataAccessException e) {
                        lastException = e;
                        String errorMessage = e.getMessage();
                        
                        if (errorMessage != null && (errorMessage.contains("SQLITE_BUSY") || 
                            errorMessage.contains("database is locked") || 
                            errorMessage.contains("database locked") ||
                            errorMessage.contains("SQLITE_BUSY_SNAPSHOT") ||
                            errorMessage.contains("SQL_BUSY"))) {
                            
                            if (attempt < maxRetries - 1) {
                                try {
                                    // Exponential backoff: 100ms, 200ms, 400ms
                                    Thread.sleep(baseRetryDelay * (1 << attempt));
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    Platform.runLater(() -> {
                                        assignButton.setDisable(false);
                                        assignButton.setText("Assign");
                                        showError("Operation was interrupted. Please try again.");
                                    });
                                    synchronized (saveLock) {
                                        isSaving = false;
                                    }
                                    return;
                                }
                                continue;
                            }
                        }
                        
                        throw e;
                    }
                }
                
                if (lastException != null) {
                    throw lastException;
                }
                
                Platform.runLater(() -> {
                    // Reload assignments from database first to ensure sync
                    loadTeacherAssignments();
                    
                    // Re-enable assign button after reload completes
                    assignButton.setDisable(false);
                    assignButton.setText("Assign");
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                String errorMessage = e.getMessage();
                final String detailedError;
                
                // Provide more helpful error messages
                if (errorMessage != null) {
                    if (errorMessage.contains("maximum of 8")) {
                        detailedError = errorMessage + "\n\nPlease remove some assignments before adding new ones.";
                    } else if (errorMessage.contains("not found")) {
                        detailedError = "Error: " + errorMessage + "\n\nPlease ensure all subjects and sections exist in the database.";
                    } else if (errorMessage.contains("null")) {
                        detailedError = "Error: Invalid assignment data. " + errorMessage + "\n\nPlease try removing and re-adding the assignment.";
                    } else if (errorMessage.contains("SQLITE") || errorMessage.contains("SQL_BUSY") || errorMessage.contains("database")) {
                        detailedError = "Database error: " + errorMessage + "\n\nPlease try again. If the problem persists, restart the application.";
                    } else {
                        detailedError = "Error saving assignments: " + errorMessage;
                    }
                } else {
                    detailedError = "An unknown error occurred while saving assignments. Please try again.";
                }
                
                Platform.runLater(() -> {
                    // Reload assignments to sync with database state
                    loadTeacherAssignments();
                    
                    assignButton.setDisable(false);
                    assignButton.setText("Assign");
                    showError(detailedError);
                });
            } finally {
                // Always release the lock
                synchronized (saveLock) {
                    isSaving = false;
                }
            }
        }).start();
    }
    
    private void handleDeleteSelectedSubject() {
        Subject selectedSubject = subjectComboBox.getValue();
        if (selectedSubject == null) {
            showError("Please select a subject to delete.");
            return;
        }
        
        handleDeleteSubject(selectedSubject);
    }
    
    private void handleDeleteSubject(Subject subject) {
        if (subject == null) {
            return;
        }
        
        // Confirm deletion - all subjects can be deleted
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Subject");
        confirmAlert.setHeaderText("Delete Subject");
        confirmAlert.setContentText("Are you sure you want to delete this subject?\n\n" +
                                   "Subject: " + subject.getName() + " (Grade " + subject.getGradeLevel() + ")\n\n" +
                                   "This action cannot be undone.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Delete in background thread
                new Thread(() -> {
                    try {
                        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                        transactionTemplate.setTimeout(30);
                        
                        transactionTemplate.execute(status -> {
                            subjectService.deleteSubject(subject.getId());
                            return null;
                        });
                        
                        // Refresh dropdown on JavaFX thread
                        Platform.runLater(() -> {
                            try {
                                // Reload subjects
                                List<Subject> allSubjectsList = subjectService.getAllSubjects();
                                List<Subject> updatedSubjects = allSubjectsList.stream()
                                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                                    .collect(Collectors.toList());
                                
                                allSubjects = FXCollections.observableArrayList(updatedSubjects);
                                
                                // Sort by creation date descending (newest first), then by grade level, then by name
                                allSubjects.sort((s1, s2) -> {
                                    // First, sort by creation date (newest first)
                                    if (s1.getCreatedAt() != null && s2.getCreatedAt() != null) {
                                        int dateCompare = s2.getCreatedAt().compareTo(s1.getCreatedAt());
                                        if (dateCompare != 0) return dateCompare;
                                    } else if (s1.getCreatedAt() != null) {
                                        return -1; // s1 has date, s2 doesn't - s1 comes first
                                    } else if (s2.getCreatedAt() != null) {
                                        return 1; // s2 has date, s1 doesn't - s2 comes first
                                    }
                                    
                                    // If dates are equal or both null, sort by grade level
                                    int gradeCompare = Integer.compare(
                                        s1.getGradeLevel() != null ? s1.getGradeLevel() : 0,
                                        s2.getGradeLevel() != null ? s2.getGradeLevel() : 0
                                    );
                                    if (gradeCompare != 0) return gradeCompare;
                                    
                                    // Finally, sort by name
                                    String name1 = s1.getName() != null ? s1.getName() : "";
                                    String name2 = s2.getName() != null ? s2.getName() : "";
                                    return name1.compareToIgnoreCase(name2);
                                });
                                
                                // Update ComboBox
                                if (subjectComboBox != null) {
                                    subjectComboBox.setItems(allSubjects);
                                    subjectComboBox.setValue(null); // Clear selection
                                }
                                
                                showSuccess("Subject '" + subject.getName() + "' deleted successfully!");
                                
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                showError("Error refreshing subjects dropdown: " + ex.getMessage());
                            }
                        });
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            showError("Error deleting subject: " + ex.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    
    private void handleEditSubject() {
        Subject selectedSubject = subjectComboBox.getValue();
        if (selectedSubject == null) {
            showError("Please select a subject to edit.");
            return;
        }
        
        // Check if service is available
        if (subjectService == null) {
            showError("Subject service not available. Please restart the application.");
            return;
        }
        
        // Create dialog to edit subject
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Subject");
        dialog.setHeaderText("Edit subject details");
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(selectedSubject.getName());
        nameField.setPromptText("Subject Name");
        ComboBox<Integer> gradeLevelCombo = new ComboBox<>(FXCollections.observableArrayList(11, 12));
        gradeLevelCombo.setValue(selectedSubject.getGradeLevel());
        TextArea descriptionArea = new TextArea(selectedSubject.getDescription() != null ? selectedSubject.getDescription() : "");
        descriptionArea.setPromptText("Description (optional)");
        descriptionArea.setPrefRowCount(3);
        
        grid.add(new Label("Subject Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Grade Level:"), 0, 1);
        grid.add(gradeLevelCombo, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionArea, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Disable save button initially
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        // Enable save button when name is entered
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveButton.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        Platform.runLater(() -> nameField.requestFocus());
        
        // Convert result when save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return nameField.getText().trim() + "|" + gradeLevelCombo.getValue() + "|" + descriptionArea.getText().trim();
            }
            return null;
        });
        
        // Handle result
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|", 3);
            String subjectName = parts[0];
            Integer gradeLevel = Integer.parseInt(parts[1]);
            String description = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
            
            if (subjectName.isEmpty()) {
                showError("Subject name is required");
                return;
            }
            
            // Update in background thread
            new Thread(() -> {
                try {
                    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                    transactionTemplate.setTimeout(30);
                    
                    Subject updatedSubject = transactionTemplate.execute(status -> {
                        return subjectService.updateSubject(selectedSubject.getId(), subjectName, gradeLevel, description);
                    });
                    
                    if (updatedSubject == null) {
                        throw new RuntimeException("Subject update failed");
                    }
                    
                    final Subject finalUpdatedSubject = updatedSubject;
                    
                    // Refresh dropdown on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            // Reload subjects
                            List<Subject> allSubjectsList = subjectService.getAllSubjects();
                            List<Subject> updatedSubjects = allSubjectsList.stream()
                                .filter(s -> s.getIsActive() != null && s.getIsActive())
                                .collect(Collectors.toList());
                            
                            allSubjects = FXCollections.observableArrayList(updatedSubjects);
                            
                            // Sort by creation date descending (newest first), then by grade level, then by name
                            allSubjects.sort((s1, s2) -> {
                                // First, sort by creation date (newest first)
                                if (s1.getCreatedAt() != null && s2.getCreatedAt() != null) {
                                    int dateCompare = s2.getCreatedAt().compareTo(s1.getCreatedAt());
                                    if (dateCompare != 0) return dateCompare;
                                } else if (s1.getCreatedAt() != null) {
                                    return -1; // s1 has date, s2 doesn't - s1 comes first
                                } else if (s2.getCreatedAt() != null) {
                                    return 1; // s2 has date, s1 doesn't - s2 comes first
                                }
                                
                                // If dates are equal or both null, sort by grade level
                                int gradeCompare = Integer.compare(
                                    s1.getGradeLevel() != null ? s1.getGradeLevel() : 0,
                                    s2.getGradeLevel() != null ? s2.getGradeLevel() : 0
                                );
                                if (gradeCompare != 0) return gradeCompare;
                                
                                // Finally, sort by name
                                String name1 = s1.getName() != null ? s1.getName() : "";
                                String name2 = s2.getName() != null ? s2.getName() : "";
                                return name1.compareToIgnoreCase(name2);
                            });
                            
                            // Update ComboBox
                            if (subjectComboBox != null) {
                                subjectComboBox.setItems(allSubjects);
                                
                                // Select the updated subject
                                Subject subjectToSelect = allSubjects.stream()
                                    .filter(s -> s.getId() != null && s.getId().equals(finalUpdatedSubject.getId()))
                                    .findFirst()
                                    .orElse(null);
                                
                                if (subjectToSelect != null) {
                                    subjectComboBox.setValue(subjectToSelect);
                                }
                            }
                            
                            showSuccess("Subject '" + finalUpdatedSubject.getName() + "' updated successfully!");
                            
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showError("Error refreshing subjects dropdown: " + ex.getMessage());
                        }
                    });
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errorMessage = ex.getMessage();
                    Platform.runLater(() -> {
                        if (errorMessage != null && errorMessage.contains("already exists")) {
                            showError("Subject '" + subjectName + "' already exists for Grade " + gradeLevel);
                        } else {
                            showError("Error updating subject: " + (errorMessage != null ? errorMessage : "Unknown error occurred"));
                        }
                    });
                }
            }).start();
        });
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
