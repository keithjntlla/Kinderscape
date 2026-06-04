package com.enrollment.system.controller;

import com.enrollment.system.model.Subject;
import com.enrollment.system.service.SubjectService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class SubjectManagementController implements Initializable {
    
    @FXML
    private TableView<Subject> subjectsTable;
    
    @FXML
    private TableColumn<Subject, Long> subjectIdColumn;
    
    @FXML
    private TableColumn<Subject, String> subjectNameColumn;
    
    @FXML
    private TableColumn<Subject, Integer> subjectGradeColumn;
    
    @FXML
    private TableColumn<Subject, String> subjectTypeColumn;
    
    @FXML
    private TableColumn<Subject, String> subjectStrandColumn;
    
    @FXML
    private TableColumn<Subject, String> subjectStatusColumn;
    
    @FXML
    private ComboBox<Integer> gradeFilter;
    
    @FXML
    private ComboBox<String> subjectTypeFilter;
    
    @FXML
    private ComboBox<String> strandFilter;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button clearFiltersButton;
    
    @FXML
    private Label subjectCountLabel;
    
    @Autowired
    private SubjectService subjectService;
    
    private ObservableList<Subject> subjectList;
    private FilteredList<Subject> filteredSubjectList;
    private SortedList<Subject> sortedSubjectList;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        
        Platform.runLater(() -> {
            loadSubjects();
        });
    }
    
    private void setupTable() {
        // Setup columns
        subjectIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        subjectNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        subjectGradeColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevel"));
        
        // Custom cell factories for type, strand, and status
        subjectTypeColumn.setCellValueFactory(cellData -> {
            String type = cellData.getValue().getSubjectType();
            // Convert SPECIALIZED to MAJOR for display, default to CORE if null
            if (type == null || type.isEmpty()) {
                return new SimpleStringProperty("CORE");
            } else if ("SPECIALIZED".equals(type)) {
                return new SimpleStringProperty("MAJOR");
            }
            return new SimpleStringProperty(type);
        });
        
        subjectStrandColumn.setCellValueFactory(cellData -> {
            String strand = cellData.getValue().getStrand();
            // Display "ALL STRAND" for core subjects (null or empty strand), never show N/A
            if (strand == null || strand.isEmpty()) {
                return new SimpleStringProperty("ALL STRAND");
            }
            return new SimpleStringProperty(strand);
        });
        
        subjectStatusColumn.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            return new SimpleStringProperty(
                isActive != null && isActive ? "Active" : "Inactive"
            );
        });
        
        // Make table columns sortable
        subjectsTable.setSortPolicy(tableView -> {
            // Allow sorting
            return true;
        });
    }
    
    private void setupFilters() {
        // Setup grade filter
        if (gradeFilter != null) {
            gradeFilter.getItems().addAll(null, 11, 12);
            gradeFilter.setValue(null);
        }
        
        // Setup subject type filter
        if (subjectTypeFilter != null) {
            subjectTypeFilter.getItems().addAll(null, "CORE", "MAJOR");
            subjectTypeFilter.setValue(null);
        }
        
        // Setup strand filter (ALL STRAND is not a filter option, only a display value)
        if (strandFilter != null) {
            strandFilter.getItems().addAll(null, "STEM", "ABM", "HUMSS", "GAS", "TVL-ICT");
            strandFilter.setValue(null);
        }
    }
    
    private void loadSubjects() {
        new Thread(() -> {
            try {
                // Only get active subjects
                java.util.List<Subject> allSubjects = subjectService.getAllSubjects();
                java.util.List<Subject> activeSubjects = allSubjects.stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                    .collect(java.util.stream.Collectors.toList());
                
                Platform.runLater(() -> {
                    subjectList = FXCollections.observableArrayList(activeSubjects);
                    
                    // Create filtered list
                    filteredSubjectList = new FilteredList<>(subjectList, p -> true);
                    
                    // Create sorted list with custom comparator for better organization
                    sortedSubjectList = new SortedList<>(filteredSubjectList, (s1, s2) -> {
                        // First sort by grade level
                        int gradeCompare = Integer.compare(
                            s1.getGradeLevel() != null ? s1.getGradeLevel() : 0,
                            s2.getGradeLevel() != null ? s2.getGradeLevel() : 0
                        );
                        if (gradeCompare != 0) return gradeCompare;
                        
                        // Then by subject type (CORE before MAJOR)
                        String type1 = s1.getSubjectType() != null ? s1.getSubjectType() : "";
                        String type2 = s2.getSubjectType() != null ? s2.getSubjectType() : "";
                        // Convert SPECIALIZED to MAJOR for comparison
                        if ("SPECIALIZED".equals(type1)) type1 = "MAJOR";
                        if ("SPECIALIZED".equals(type2)) type2 = "MAJOR";
                        int typeCompare = type1.compareTo(type2);
                        if (typeCompare != 0) {
                            // CORE should come before MAJOR
                            if ("CORE".equals(type1)) return -1;
                            if ("CORE".equals(type2)) return 1;
                            return typeCompare;
                        }
                        
                        // Then by strand (ALL STRAND first, then alphabetically)
                        String strand1 = s1.getStrand() != null ? s1.getStrand() : "ALL STRAND";
                        String strand2 = s2.getStrand() != null ? s2.getStrand() : "ALL STRAND";
                        if ("ALL STRAND".equals(strand1) && !"ALL STRAND".equals(strand2)) return -1;
                        if ("ALL STRAND".equals(strand2) && !"ALL STRAND".equals(strand1)) return 1;
                        int strandCompare = strand1.compareTo(strand2);
                        if (strandCompare != 0) return strandCompare;
                        
                        // Finally by name
                        String name1 = s1.getName() != null ? s1.getName() : "";
                        String name2 = s2.getName() != null ? s2.getName() : "";
                        return name1.compareToIgnoreCase(name2);
                    });
                    
                    sortedSubjectList.comparatorProperty().bind(subjectsTable.comparatorProperty());
                    
                    // Set table items
                    subjectsTable.setItems(sortedSubjectList);
                    
                    // Update count
                    updateSubjectCount();
                    
                    // Apply initial filters
                    applyFilters();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error loading subjects: " + e.getMessage());
                });
            }
        }).start();
    }
    
    @FXML
    private void handleFilterChange() {
        applyFilters();
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }
    
    @FXML
    private void handleClearFilters() {
        if (gradeFilter != null) {
            gradeFilter.setValue(null);
        }
        if (subjectTypeFilter != null) {
            subjectTypeFilter.setValue(null);
        }
        if (strandFilter != null) {
            strandFilter.setValue(null);
        }
        if (searchField != null) {
            searchField.clear();
        }
        applyFilters();
    }
    
    @FXML
    private void handleRefresh() {
        loadSubjects();
    }
    
    private void applyFilters() {
        if (filteredSubjectList == null) {
            return;
        }
        
        filteredSubjectList.setPredicate(subject -> {
            // Grade filter
            Integer selectedGrade = gradeFilter != null ? gradeFilter.getValue() : null;
            if (selectedGrade != null && !selectedGrade.equals(subject.getGradeLevel())) {
                return false;
            }
            
            // Subject type filter (handle both MAJOR and SPECIALIZED in database)
            String selectedType = subjectTypeFilter != null ? subjectTypeFilter.getValue() : null;
            if (selectedType != null) {
                String subjectType = subject.getSubjectType();
                // Convert SPECIALIZED to MAJOR for comparison
                if ("SPECIALIZED".equals(subjectType)) {
                    subjectType = "MAJOR";
                }
                if (!selectedType.equals(subjectType)) {
                    return false;
                }
            }
            
            // Strand filter (handle ALL STRAND - subjects with null/empty strand)
            String selectedStrand = strandFilter != null ? strandFilter.getValue() : null;
            if (selectedStrand != null) {
                String subjectStrand = subject.getStrand();
                // If subject has null/empty strand, it's "ALL STRAND" but not filterable by strand
                // Only filter by specific strands (STEM, ABM, etc.)
                if (subjectStrand == null || subjectStrand.isEmpty()) {
                    return false; // Core subjects (ALL STRAND) don't match specific strand filters
                }
                if (!selectedStrand.equals(subjectStrand)) {
                    return false;
                }
            }
            
            // Search filter
            String searchText = searchField != null ? searchField.getText() : null;
            if (searchText != null && !searchText.trim().isEmpty()) {
                String searchLower = searchText.toLowerCase();
                String subjectName = subject.getName() != null ? subject.getName().toLowerCase() : "";
                if (!subjectName.contains(searchLower)) {
                    return false;
                }
            }
            
            return true;
        });
        
        updateSubjectCount();
    }
    
    private void updateSubjectCount() {
        if (filteredSubjectList != null && subjectCountLabel != null) {
            int count = filteredSubjectList.size();
            subjectCountLabel.setText("Total Subjects: " + count);
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

