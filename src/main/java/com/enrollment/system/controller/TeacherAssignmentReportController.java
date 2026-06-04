package com.enrollment.system.controller;

import com.enrollment.system.dto.UserDto;
import com.enrollment.system.service.TeacherService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;

@Component
public class TeacherAssignmentReportController implements Initializable {
    
    @FXML
    private ListView<UserDto> teachersListView;
    
    @FXML
    private Button exportAllTeachersPdfButton;
    
    @FXML
    private Button exportSelectedTeacherPdfButton;
    
    @FXML
    private Label summaryLabel;
    
    @FXML
    private Label previewLabel;
    
    @FXML
    private Label selectedTeacherLabel;
    
    @FXML
    private TableView<AssignmentInfo> subjectsTable;
    
    @FXML
    private TableColumn<AssignmentInfo, String> subjectNameColumn;
    
    @FXML
    private TableColumn<AssignmentInfo, String> sectionColumn;
    
    @FXML
    private TableColumn<AssignmentInfo, String> strandColumn;
    
    @FXML
    private TableColumn<AssignmentInfo, Integer> gradeColumn;
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private com.enrollment.system.repository.TeacherAssignmentRepository teacherAssignmentRepository;
    
    private ObservableList<UserDto> allTeachers;
    private UserDto selectedTeacher;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadTeachers();
    }
    
    private void setupTableColumns() {
        subjectNameColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getSubjectName()));
        sectionColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getSectionName()));
        strandColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStrand()));
        gradeColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getGrade()));
    }
    
    private void loadTeachers() {
        new Thread(() -> {
            try {
                List<UserDto> teachers = teacherService.getAllTeachers();
                
                Platform.runLater(() -> {
                    allTeachers = FXCollections.observableArrayList(teachers);
                    teachersListView.setItems(allTeachers);
                    
                    // Set cell factory to display full name
                    teachersListView.setCellFactory(param -> new ListCell<UserDto>() {
                        @Override
                        protected void updateItem(UserDto item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(item.getFullName());
                                setStyle("-fx-padding: 8px; -fx-font-size: 13px;");
                            }
                        }
                    });
                    
                    // Add selection listener
                    teachersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            handleTeacherSelection(newVal);
                        }
                    });
                    
                    updateSummary();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load teachers");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    private void handleTeacherSelection(UserDto teacher) {
        selectedTeacher = teacher;
        
        if (selectedTeacher == null) {
            subjectsTable.setItems(FXCollections.observableArrayList());
            selectedTeacherLabel.setText("No teacher selected");
            previewLabel.setText("Select a teacher from the list to view details");
            return;
        }
        
        // Update selected teacher label
        selectedTeacherLabel.setText(selectedTeacher.getFullName());
        
        // Load assignments directly from TeacherAssignment table
        new Thread(() -> {
            try {
                List<com.enrollment.system.model.TeacherAssignment> assignments = 
                        teacherAssignmentRepository.findByTeacherId(selectedTeacher.getId());
                
                Platform.runLater(() -> {
                    ObservableList<AssignmentInfo> tableData = FXCollections.observableArrayList();
                    
                    if (assignments == null || assignments.isEmpty()) {
                        previewLabel.setText("Teacher: " + selectedTeacher.getFullName() + " - No assignments found");
                    } else {
                        for (com.enrollment.system.model.TeacherAssignment assignment : assignments) {
                            String subjectName = assignment.getSubject() != null ? assignment.getSubject().getName() : "N/A";
                            String sectionName = assignment.getSection() != null ? assignment.getSection().getName() : "N/A";
                            String strand = assignment.getSection() != null && assignment.getSection().getStrand() != null 
                                    ? assignment.getSection().getStrand() : "N/A";
                            Integer grade = assignment.getSection() != null ? assignment.getSection().getGradeLevel() : null;
                            
                            tableData.add(new AssignmentInfo(subjectName, sectionName, strand, grade));
                        }
                        previewLabel.setText("Teacher: " + selectedTeacher.getFullName() + " - " + assignments.size() + " assignment(s)");
                    }
                    
                    subjectsTable.setItems(tableData);
                    subjectsTable.setVisible(true);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load teacher assignments");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    @FXML
    private void exportAllTeachersToPDF() {
        if (allTeachers == null || allTeachers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Data");
            alert.setHeaderText("No teachers found");
            alert.setContentText("There are no teachers in the system.");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("All_Teachers_Report.pdf");
        
        Stage stage = (Stage) exportAllTeachersPdfButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            new Thread(() -> {
                try {
                    generateAllTeachersPDF(file);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("PDF exported successfully");
                        alert.setContentText("File saved to: " + file.getAbsolutePath());
                        alert.showAndWait();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to export PDF");
                        alert.setContentText("Error: " + e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }
    
    @FXML
    private void exportSelectedTeacherWithSubjectsToPDF() {
        if (selectedTeacher == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Teacher Selected");
            alert.setHeaderText("Please select a teacher");
            alert.setContentText("You must select a teacher from the dropdown to export their subjects.");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String fileName = "Teacher_" + selectedTeacher.getFullName().replaceAll("[^a-zA-Z0-9]", "_") + "_Subjects.pdf";
        fileChooser.setInitialFileName(fileName);
        
        Stage stage = (Stage) exportSelectedTeacherPdfButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            new Thread(() -> {
                try {
                    generateSelectedTeacherPDF(file);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("PDF exported successfully");
                        alert.setContentText("File saved to: " + file.getAbsolutePath());
                        alert.showAndWait();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to export PDF");
                        alert.setContentText("Error: " + e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }
    
    private void generateAllTeachersPDF(File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 50;
                float yPosition = 750;
                float lineHeight = 15;
                
                PDType1Font fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                
                // Title
                contentStream.beginText();
                contentStream.setFont(fontBold, 16);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("ALL TEACHERS REPORT");
                contentStream.endText();
                
                yPosition -= 25;
                
                // Date
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                contentStream.endText();
                
                yPosition -= 30;
                
                // Table headers
                float[] columnWidths = {200, 200, 100};
                String[] headers = {"Full Name", "Username", "Status"};
                
                float totalWidth = 0;
                for (float width : columnWidths) {
                    totalWidth += width;
                }
                
                float xPosition = margin;
                contentStream.setFont(fontBold, 10);
                for (int i = 0; i < headers.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    contentStream.showText(headers[i]);
                    contentStream.endText();
                    xPosition += columnWidths[i];
                }
                
                yPosition -= lineHeight;
                
                // Draw line
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(margin + totalWidth, yPosition);
                contentStream.stroke();
                
                yPosition -= 10;
                
                // Data
                contentStream.setFont(font, 9);
                for (UserDto teacher : allTeachers) {
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                        font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                        yPosition = 750;
                    }
                    
                    xPosition = margin;
                    
                    // Full Name
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    contentStream.showText(teacher.getFullName() != null ? teacher.getFullName() : "");
                    contentStream.endText();
                    xPosition += columnWidths[0];
                    
                    // Username
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    contentStream.showText(teacher.getUsername() != null ? teacher.getUsername() : "");
                    contentStream.endText();
                    xPosition += columnWidths[1];
                    
                    // Status
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, yPosition);
                    String status = (teacher.getIsActive() != null && teacher.getIsActive()) ? "Active" : "Inactive";
                    contentStream.showText(status);
                    contentStream.endText();
                    
                    yPosition -= lineHeight;
                }
                
                // Footer
                yPosition -= 20;
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Total Teachers: " + allTeachers.size());
                contentStream.endText();
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(file);
        }
    }
    
    private void generateSelectedTeacherPDF(File file) throws IOException {
        if (selectedTeacher == null) {
            throw new IllegalStateException("No teacher selected");
        }
        
        // Get all teacher assignments (subject-section pairs)
        List<com.enrollment.system.model.TeacherAssignment> assignments = 
                teacherAssignmentRepository.findByTeacherId(selectedTeacher.getId());
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 72; // 1 inch margin
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float yPosition = pageHeight - margin;
                float lineHeight = 20;
                float cellPadding = 10;
                
                PDType1Font fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                
                // Title - PERFECTLY centered
                String title = "TEACHER ASSIGNMENT REPORT";
                float titleFontSize = 20;
                contentStream.setFont(fontBold, titleFontSize);
                float titleWidth = fontBold.getStringWidth(title) / 1000 * titleFontSize;
                float titleX = (pageWidth - titleWidth) / 2;
                contentStream.beginText();
                contentStream.setFont(fontBold, titleFontSize);
                contentStream.newLineAtOffset(titleX, yPosition);
                contentStream.showText(title);
                contentStream.endText();
                
                yPosition -= 45;
                
                // Teacher Name - centered
                String teacherText = "Teacher Name: " + selectedTeacher.getFullName();
                float teacherFontSize = 12;
                contentStream.setFont(font, teacherFontSize);
                float teacherWidth = font.getStringWidth(teacherText) / 1000 * teacherFontSize;
                float teacherX = (pageWidth - teacherWidth) / 2;
                contentStream.beginText();
                contentStream.setFont(font, teacherFontSize);
                contentStream.newLineAtOffset(teacherX, yPosition);
                contentStream.showText(teacherText);
                contentStream.endText();
                
                yPosition -= 25;
                
                // Date - centered
                String dateText = "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                float dateFontSize = 12;
                contentStream.setFont(font, dateFontSize);
                float dateWidth = font.getStringWidth(dateText) / 1000 * dateFontSize;
                float dateX = (pageWidth - dateWidth) / 2;
                contentStream.beginText();
                contentStream.setFont(font, dateFontSize);
                contentStream.newLineAtOffset(dateX, yPosition);
                contentStream.showText(dateText);
                contentStream.endText();
                
                yPosition -= 40;
                
                // Table setup - centered table with proper column widths
                float[] columnWidths = {220, 130, 110, 110}; // Subject Name, Section, Grade Level, Strand
                float tableWidth = columnWidths[0] + columnWidths[1] + columnWidths[2] + columnWidths[3];
                float tableStartX = (pageWidth - tableWidth) / 2; // Center the table
                float headerHeight = lineHeight + (cellPadding * 2);
                float tableStartY = yPosition;
                
                // Draw table header with clean background
                contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                contentStream.addRect(tableStartX, tableStartY - headerHeight, tableWidth, headerHeight);
                contentStream.fill();
                contentStream.setNonStrokingColor(0, 0, 0);
                
                // Draw complete header border (all sides)
                contentStream.setLineWidth(1.5f);
                // Top border
                contentStream.moveTo(tableStartX, tableStartY);
                contentStream.lineTo(tableStartX + tableWidth, tableStartY);
                contentStream.stroke();
                // Bottom border of header
                contentStream.moveTo(tableStartX, tableStartY - headerHeight);
                contentStream.lineTo(tableStartX + tableWidth, tableStartY - headerHeight);
                contentStream.stroke();
                // Left border
                contentStream.moveTo(tableStartX, tableStartY);
                contentStream.lineTo(tableStartX, tableStartY - headerHeight);
                contentStream.stroke();
                // Right border
                contentStream.moveTo(tableStartX + tableWidth, tableStartY);
                contentStream.lineTo(tableStartX + tableWidth, tableStartY - headerHeight);
                contentStream.stroke();
                
                // Draw vertical lines between header columns
                contentStream.setLineWidth(1.0f);
                float colX = tableStartX;
                for (int i = 1; i < columnWidths.length; i++) {
                    colX += columnWidths[i - 1];
                    contentStream.moveTo(colX, tableStartY);
                    contentStream.lineTo(colX, tableStartY - headerHeight);
                    contentStream.stroke();
                }
                
                // Table headers with proper padding and centering
                String[] headers = {"Subject Name", "Section", "Grade Level", "Strand"};
                contentStream.setFont(fontBold, 11);
                
                float headerX = tableStartX + cellPadding;
                for (int i = 0; i < headers.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(headerX, tableStartY - cellPadding - 13);
                    contentStream.showText(headers[i]);
                    contentStream.endText();
                    headerX += columnWidths[i];
                }
                
                yPosition = tableStartY - headerHeight;
                
                // Table data
                if (assignments == null || assignments.isEmpty()) {
                    // No assignments message - centered
                    String noDataText = "No assigned subjects found.";
                    contentStream.setFont(font, 12);
                    float noDataWidth = font.getStringWidth(noDataText) / 1000 * 12;
                    float noDataX = (pageWidth - noDataWidth) / 2;
                    contentStream.beginText();
                    contentStream.setFont(font, 12);
                    contentStream.newLineAtOffset(noDataX, yPosition - 20);
                    contentStream.showText(noDataText);
                    contentStream.endText();
                } else {
                    contentStream.setFont(font, 10);
                    int rowNum = 0;
                    float rowHeight = lineHeight + (cellPadding * 2);
                    
                    for (com.enrollment.system.model.TeacherAssignment assignment : assignments) {
                        // Check if we need a new page
                        if (yPosition < margin + 60) {
                            // Draw bottom border of current table
                            contentStream.setLineWidth(1.5f);
                            contentStream.moveTo(tableStartX, yPosition);
                            contentStream.lineTo(tableStartX + tableWidth, yPosition);
                            contentStream.stroke();
                            
                            contentStream.close();
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                            font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                            yPosition = pageHeight - margin;
                            
                            // Redraw header on new page
                            tableStartY = yPosition;
                            contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                            contentStream.addRect(tableStartX, tableStartY - headerHeight, tableWidth, headerHeight);
                            contentStream.fill();
                            contentStream.setNonStrokingColor(0, 0, 0);
                            
                            contentStream.setLineWidth(1.5f);
                            contentStream.moveTo(tableStartX, tableStartY);
                            contentStream.lineTo(tableStartX + tableWidth, tableStartY);
                            contentStream.stroke();
                            contentStream.moveTo(tableStartX, tableStartY - headerHeight);
                            contentStream.lineTo(tableStartX + tableWidth, tableStartY - headerHeight);
                            contentStream.stroke();
                            contentStream.moveTo(tableStartX, tableStartY);
                            contentStream.lineTo(tableStartX, tableStartY - headerHeight);
                            contentStream.stroke();
                            contentStream.moveTo(tableStartX + tableWidth, tableStartY);
                            contentStream.lineTo(tableStartX + tableWidth, tableStartY - headerHeight);
                            contentStream.stroke();
                            
                            contentStream.setLineWidth(1.0f);
                            colX = tableStartX;
                            for (int i = 1; i < columnWidths.length; i++) {
                                colX += columnWidths[i - 1];
                                contentStream.moveTo(colX, tableStartY);
                                contentStream.lineTo(colX, tableStartY - headerHeight);
                                contentStream.stroke();
                            }
                            
                            headerX = tableStartX + cellPadding;
                            contentStream.setFont(fontBold, 11);
                            for (int i = 0; i < headers.length; i++) {
                                contentStream.beginText();
                                contentStream.newLineAtOffset(headerX, tableStartY - cellPadding - 13);
                                contentStream.showText(headers[i]);
                                contentStream.endText();
                                headerX += columnWidths[i];
                            }
                            
                            yPosition = tableStartY - headerHeight;
                            contentStream.setFont(font, 10);
                        }
                        
                        // Draw row background (alternating - only even rows get gray)
                        if (rowNum % 2 == 0) {
                            contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f);
                            contentStream.addRect(tableStartX, yPosition - rowHeight, tableWidth, rowHeight);
                            contentStream.fill();
                            contentStream.setNonStrokingColor(0, 0, 0);
                        }
                        
                        // Draw ALL row borders (complete rectangle for each row)
                        contentStream.setLineWidth(0.5f);
                        // Top border
                        contentStream.moveTo(tableStartX, yPosition);
                        contentStream.lineTo(tableStartX + tableWidth, yPosition);
                        contentStream.stroke();
                        // Bottom border
                        contentStream.moveTo(tableStartX, yPosition - rowHeight);
                        contentStream.lineTo(tableStartX + tableWidth, yPosition - rowHeight);
                        contentStream.stroke();
                        // Left border
                        contentStream.moveTo(tableStartX, yPosition);
                        contentStream.lineTo(tableStartX, yPosition - rowHeight);
                        contentStream.stroke();
                        // Right border
                        contentStream.moveTo(tableStartX + tableWidth, yPosition);
                        contentStream.lineTo(tableStartX + tableWidth, yPosition - rowHeight);
                        contentStream.stroke();
                        
                        // Draw vertical lines between columns
                        colX = tableStartX;
                        for (int i = 1; i < columnWidths.length; i++) {
                            colX += columnWidths[i - 1];
                            contentStream.moveTo(colX, yPosition);
                            contentStream.lineTo(colX, yPosition - rowHeight);
                            contentStream.stroke();
                        }
                        
                        float cellX = tableStartX + cellPadding;
                        
                        // Subject Name
                        String subjectName = assignment.getSubject() != null ? assignment.getSubject().getName() : "N/A";
                        contentStream.beginText();
                        contentStream.newLineAtOffset(cellX, yPosition - cellPadding - 13);
                        contentStream.showText(subjectName);
                        contentStream.endText();
                        cellX += columnWidths[0];
                        
                        // Section
                        String sectionName = assignment.getSection() != null ? assignment.getSection().getName() : "N/A";
                        contentStream.beginText();
                        contentStream.newLineAtOffset(cellX, yPosition - cellPadding - 13);
                        contentStream.showText(sectionName);
                        contentStream.endText();
                        cellX += columnWidths[1];
                        
                        // Grade Level
                        String gradeLevel = assignment.getSection() != null && assignment.getSection().getGradeLevel() != null 
                                ? assignment.getSection().getGradeLevel().toString() : "N/A";
                        contentStream.beginText();
                        contentStream.newLineAtOffset(cellX, yPosition - cellPadding - 13);
                        contentStream.showText(gradeLevel);
                        contentStream.endText();
                        cellX += columnWidths[2];
                        
                        // Strand
                        String strand = assignment.getSection() != null && assignment.getSection().getStrand() != null 
                                ? assignment.getSection().getStrand() : "N/A";
                        contentStream.beginText();
                        contentStream.newLineAtOffset(cellX, yPosition - cellPadding - 13);
                        contentStream.showText(strand);
                        contentStream.endText();
                        
                        yPosition -= rowHeight;
                        rowNum++;
                    }
                    
                    // Draw final bottom border
                    contentStream.setLineWidth(1.5f);
                    contentStream.moveTo(tableStartX, yPosition);
                    contentStream.lineTo(tableStartX + tableWidth, yPosition);
                    contentStream.stroke();
                }
                
                // Footer - centered
                yPosition -= 30;
                String footerText = "Total Assignments: " + ((assignments == null) ? 0 : assignments.size());
                contentStream.setFont(font, 11);
                float footerWidth = font.getStringWidth(footerText) / 1000 * 11;
                float footerX = (pageWidth - footerWidth) / 2;
                contentStream.beginText();
                contentStream.setFont(font, 11);
                contentStream.newLineAtOffset(footerX, yPosition);
                contentStream.showText(footerText);
                contentStream.endText();
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(file);
        }
    }
    
    private void updateSummary() {
        if (allTeachers != null) {
            summaryLabel.setText("Total Teachers: " + allTeachers.size());
        }
    }
    
    /**
     * Data class for displaying assignment information in the preview table
     * Matches the structure shown in Assign Subjects & Sections interface
     */
    private static class AssignmentInfo {
        private final String subjectName;
        private final String sectionName;
        private final String strand;
        private final Integer grade;
        
        public AssignmentInfo(String subjectName, String sectionName, String strand, Integer grade) {
            this.subjectName = subjectName;
            this.sectionName = sectionName;
            this.strand = strand;
            this.grade = grade;
        }
        
        public String getSubjectName() {
            return subjectName;
        }
        
        public String getSectionName() {
            return sectionName;
        }
        
        public String getStrand() {
            return strand;
        }
        
        public Integer getGrade() {
            return grade;
        }
    }
}
