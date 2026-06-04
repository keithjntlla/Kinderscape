package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.Section;
import com.enrollment.system.service.ReportService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;

@Component
public class StudentListBySectionController implements Initializable {
    
    @FXML
    private VBox mainContainer;
    
    @FXML
    private ComboBox<Section> sectionComboBox;
    
    @FXML
    private TableView<StudentDto> studentsTable;
    
    @FXML
    private TableColumn<StudentDto, String> nameColumn;
    
    @FXML
    private TableColumn<StudentDto, LocalDate> birthdateColumn;
    
    @FXML
    private TableColumn<StudentDto, Integer> ageColumn;
    
    @FXML
    private TableColumn<StudentDto, String> sexColumn;
    
    @FXML
    private TableColumn<StudentDto, String> addressColumn;
    
    @FXML
    private TableColumn<StudentDto, String> contactNumberColumn;
    
    @FXML
    private TableColumn<StudentDto, String> parentGuardianNameColumn;
    
    @FXML
    private TableColumn<StudentDto, String> parentGuardianContactColumn;
    
    @FXML
    private TableColumn<StudentDto, String> parentGuardianRelationshipColumn;
    
    @FXML
    private TableColumn<StudentDto, Integer> gradeLevelColumn;
    
    @FXML
    private TableColumn<StudentDto, String> strandColumn;
    
    @FXML
    private TableColumn<StudentDto, String> lrnColumn;
    
    @FXML
    private TableColumn<StudentDto, String> previousSchoolColumn;
    
    @FXML
    private TableColumn<StudentDto, Double> gwaColumn;
    
    @FXML
    private TableColumn<StudentDto, String> enrollmentStatusColumn;
    
    @FXML
    private Button exportPdfButton;
    
    @FXML
    private Button exportExcelButton;
    
    @FXML
    private Label sectionInfoLabel;
    
    @Autowired
    private ReportService reportService;
    
    private Map<Section, List<StudentDto>> studentsBySection;
    private ObservableList<StudentDto> currentStudents;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupComboBox();
        loadData();
    }
    
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        birthdateColumn.setCellValueFactory(new PropertyValueFactory<>("birthdate"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        sexColumn.setCellValueFactory(new PropertyValueFactory<>("sex"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        contactNumberColumn.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        parentGuardianNameColumn.setCellValueFactory(new PropertyValueFactory<>("parentGuardianName"));
        parentGuardianContactColumn.setCellValueFactory(new PropertyValueFactory<>("parentGuardianContact"));
        parentGuardianRelationshipColumn.setCellValueFactory(new PropertyValueFactory<>("parentGuardianRelationship"));
        gradeLevelColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevel"));
        strandColumn.setCellValueFactory(new PropertyValueFactory<>("strand"));
        lrnColumn.setCellValueFactory(new PropertyValueFactory<>("lrn"));
        previousSchoolColumn.setCellValueFactory(new PropertyValueFactory<>("previousSchool"));
        gwaColumn.setCellValueFactory(new PropertyValueFactory<>("gwa"));
        enrollmentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("enrollmentStatus"));
        
        // Format date column
        birthdateColumn.setCellFactory(column -> new TableCell<StudentDto, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                }
            }
        });
    }
    
    private void setupComboBox() {
        sectionComboBox.setCellFactory(param -> new ListCell<Section>() {
            @Override
            protected void updateItem(Section item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getStrand() + " (Grade " + item.getGradeLevel() + ")");
                }
            }
        });
        
        sectionComboBox.setButtonCell(new ListCell<Section>() {
            @Override
            protected void updateItem(Section item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getStrand() + " (Grade " + item.getGradeLevel() + ")");
                }
            }
        });
        
        sectionComboBox.setOnAction(e -> {
            Section selected = sectionComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && studentsBySection != null) {
                List<StudentDto> students = studentsBySection.get(selected);
                if (students != null) {
                    currentStudents = FXCollections.observableArrayList(students);
                    studentsTable.setItems(currentStudents);
                    sectionInfoLabel.setText("Section: " + selected.getName() + " | Students: " + students.size());
                } else {
                    currentStudents = FXCollections.observableArrayList();
                    studentsTable.setItems(currentStudents);
                    sectionInfoLabel.setText("Section: " + selected.getName() + " | Students: 0");
                }
            }
        });
    }
    
    private void loadData() {
        new Thread(() -> {
            try {
                studentsBySection = reportService.getStudentsBySection();
                
                Platform.runLater(() -> {
                    List<Section> sections = new ArrayList<>(studentsBySection.keySet());
                    sectionComboBox.setItems(FXCollections.observableArrayList(sections));
                    
                    if (!sections.isEmpty()) {
                        sectionComboBox.getSelectionModel().select(0);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load data");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    @FXML
    private void exportToPDF() {
        Section selected = sectionComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Section Selected");
            alert.setHeaderText("Please select a section first");
            alert.showAndWait();
            return;
        }
        
        List<StudentDto> students = studentsBySection.get(selected);
        if (students == null || students.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Data");
            alert.setHeaderText("No students found in this section");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Student_List_" + selected.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        
        Stage stage = (Stage) exportPdfButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            new Thread(() -> {
                try {
                    generatePDF(file, selected, students);
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
    private void exportToExcel() {
        Section selected = sectionComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Section Selected");
            alert.setHeaderText("Please select a section first");
            alert.showAndWait();
            return;
        }
        
        List<StudentDto> students = studentsBySection.get(selected);
        if (students == null || students.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Data");
            alert.setHeaderText("No students found in this section");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Student_List_" + selected.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");
        
        Stage stage = (Stage) exportExcelButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            new Thread(() -> {
                try {
                    generateExcel(file, selected, students);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setHeaderText("Excel exported successfully");
                        alert.setContentText("File saved to: " + file.getAbsolutePath());
                        alert.showAndWait();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to export Excel");
                        alert.setContentText("Error: " + e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }
    
    private void generatePDF(File file, Section section, List<StudentDto> students) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Create landscape A4 page (842 x 595 points) - perfect for 8 columns
            PDPage page = new PDPage(new PDRectangle(842, 595));
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                // Professional fonts - Helvetica is clean and professional
                PDType1Font fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                
                // A4 Landscape dimensions: width=842, height=595
                float margin = 35;
                float pageWidth = 842;
                float pageHeight = 595;
                float yPosition = pageHeight - margin;
                float lineHeight = 12;
                float headerLineHeight = 14;
                
                // Title Section
                contentStream.beginText();
                contentStream.setFont(fontBold, 18);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("STUDENT LIST BY SECTION");
                contentStream.endText();
                
                yPosition -= 25;
                
                // Section Info
                contentStream.beginText();
                contentStream.setFont(font, 11);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Section: " + section.getName() + " | Strand: " + section.getStrand() + " | Grade Level: " + section.getGradeLevel());
                contentStream.endText();
                
                yPosition -= 18;
                
                // Date
                contentStream.beginText();
                contentStream.setFont(font, 9);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                contentStream.endText();
                
                yPosition -= 25;
                
                // Draw separator line
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                
                yPosition -= 15;
                
                // Table headers with optimized column widths for A4 landscape
                // Excluded: Strand, Grade, Previous School, GWA, Enrollment Status, Relationship, Birthdate
                float[] columnWidths = {
                    130,  // Name
                    35,   // Age
                    35,   // Sex
                    150,  // Address
                    80,   // Contact
                    120,  // Parent Name
                    80,   // Parent Contact
                    85    // LRN
                };
                
                String[] headers = {
                    "Name", "Age", "Sex", "Address", "Contact", 
                    "Parent Name", "Parent Contact", "LRN"
                };
                
                // Calculate total width and center if needed
                float totalWidth = 0;
                for (float width : columnWidths) {
                    totalWidth += width;
                }
                
                // Header row with background
                float headerY = yPosition;
                contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f); // Light gray background
                contentStream.addRect(margin, headerY - headerLineHeight, totalWidth, headerLineHeight);
                contentStream.fill();
                contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                
                // Header text
                float xPosition = margin;
                contentStream.setFont(fontBold, 9);
                for (int i = 0; i < headers.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition + 2, headerY - 10);
                    contentStream.showText(headers[i]);
                    contentStream.endText();
                    xPosition += columnWidths[i];
                }
                
                yPosition -= headerLineHeight;
                
                // Draw header bottom line
                contentStream.setLineWidth(0.5f);
                contentStream.setStrokingColor(0.3f, 0.3f, 0.3f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(margin + totalWidth, yPosition);
                contentStream.stroke();
                contentStream.setStrokingColor(0, 0, 0); // Reset to black
                
                yPosition -= 8;
                
                // Student data rows
                contentStream.setFont(font, 8);
                int rowNumber = 0;
                for (StudentDto student : students) {
                    // Check if we need a new page
                    if (yPosition < 60) {
                        contentStream.close();
                        page = new PDPage(new PDRectangle(842, 595));
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        // Re-initialize fonts for new page
                        fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                        font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                        yPosition = pageHeight - margin;
                        
                        // Redraw header on new page
                        headerY = yPosition;
                        contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                        contentStream.addRect(margin, headerY - headerLineHeight, totalWidth, headerLineHeight);
                        contentStream.fill();
                        contentStream.setNonStrokingColor(0, 0, 0);
                        
                        xPosition = margin;
                        contentStream.setFont(fontBold, 9);
                        for (int i = 0; i < headers.length; i++) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(xPosition + 2, headerY - 10);
                            contentStream.showText(headers[i]);
                            contentStream.endText();
                            xPosition += columnWidths[i];
                        }
                        
                        contentStream.setLineWidth(0.5f);
                        contentStream.setStrokingColor(0.3f, 0.3f, 0.3f);
                        contentStream.moveTo(margin, headerY - headerLineHeight);
                        contentStream.lineTo(margin + totalWidth, headerY - headerLineHeight);
                        contentStream.stroke();
                        contentStream.setStrokingColor(0, 0, 0);
                        
                        yPosition = headerY - headerLineHeight - 8;
                        contentStream.setFont(font, 8);
                    }
                    
                    // Alternate row background for better readability
                    if (rowNumber % 2 == 0) {
                        contentStream.setNonStrokingColor(0.98f, 0.98f, 0.98f);
                        contentStream.addRect(margin, yPosition - lineHeight, totalWidth, lineHeight);
                        contentStream.fill();
                        contentStream.setNonStrokingColor(0, 0, 0);
                    }
                    
                    xPosition = margin;
                    // Excluded: Grade Level, Strand, Previous School, GWA, Enrollment Status, Relationship, Birthdate
                    String[] rowData = {
                        student.getName() != null ? student.getName() : "",
                        student.getAge() != null ? String.valueOf(student.getAge()) : "",
                        student.getSex() != null ? student.getSex() : "",
                        student.getAddress() != null ? student.getAddress() : "",
                        student.getContactNumber() != null ? student.getContactNumber() : "",
                        student.getParentGuardianName() != null ? student.getParentGuardianName() : "",
                        student.getParentGuardianContact() != null ? student.getParentGuardianContact() : "",
                        student.getLrn() != null ? student.getLrn() : ""
                    };
                    
                    for (int i = 0; i < rowData.length; i++) {
                        String text = rowData[i];
                        contentStream.beginText();
                        contentStream.newLineAtOffset(xPosition + 2, yPosition - 9);
                        contentStream.showText(text);
                        contentStream.endText();
                        xPosition += columnWidths[i];
                    }
                    
                    yPosition -= lineHeight;
                    rowNumber++;
                }
                
                // Footer
                yPosition -= 15;
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(pageWidth - margin, yPosition);
                contentStream.stroke();
                
                yPosition -= 12;
                contentStream.beginText();
                contentStream.setFont(fontBold, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Total Students: " + students.size());
                contentStream.endText();
                
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(file);
        }
    }
    
    private void generateExcel(File file, Section section, List<StudentDto> students) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Student List");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            int rowNum = 0;
            
            // Title row
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("STUDENT LIST BY SECTION");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            
            // Section info row
            Row infoRow = sheet.createRow(rowNum++);
            infoRow.createCell(0).setCellValue("Section: " + section.getName() + " | Strand: " + section.getStrand() + " | Grade: " + section.getGradeLevel());
            
            // Date row
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            rowNum++; // Empty row
            
            // Header row
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Name", "Birthdate", "Age", "Sex", "Address", "Contact Number", "Parent/Guardian Name", "Parent/Guardian Contact", "Relationship", "Grade Level", "Strand", "LRN", "Previous School", "GWA", "Enrollment Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            for (StudentDto student : students) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                
                row.createCell(colNum++).setCellValue(student.getName() != null ? student.getName() : "");
                row.createCell(colNum++).setCellValue(student.getBirthdate() != null ? student.getBirthdate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "");
                row.createCell(colNum++).setCellValue(student.getAge() != null ? student.getAge() : 0);
                row.createCell(colNum++).setCellValue(student.getSex() != null ? student.getSex() : "");
                row.createCell(colNum++).setCellValue(student.getAddress() != null ? student.getAddress() : "");
                row.createCell(colNum++).setCellValue(student.getContactNumber() != null ? student.getContactNumber() : "");
                row.createCell(colNum++).setCellValue(student.getParentGuardianName() != null ? student.getParentGuardianName() : "");
                row.createCell(colNum++).setCellValue(student.getParentGuardianContact() != null ? student.getParentGuardianContact() : "");
                row.createCell(colNum++).setCellValue(student.getParentGuardianRelationship() != null ? student.getParentGuardianRelationship() : "");
                row.createCell(colNum++).setCellValue(student.getGradeLevel() != null ? student.getGradeLevel() : 0);
                row.createCell(colNum++).setCellValue(student.getStrand() != null ? student.getStrand() : "");
                row.createCell(colNum++).setCellValue(student.getLrn() != null ? student.getLrn() : "");
                row.createCell(colNum++).setCellValue(student.getPreviousSchool() != null ? student.getPreviousSchool() : "");
                row.createCell(colNum++).setCellValue(student.getGwa() != null ? student.getGwa() : 0.0);
                row.createCell(colNum++).setCellValue(student.getEnrollmentStatus() != null ? student.getEnrollmentStatus() : "");
                
                // Apply data style
                for (int i = 0; i < colNum; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Footer row
            rowNum++;
            Row footerRow = sheet.createRow(rowNum);
            footerRow.createCell(0).setCellValue("Total Students: " + students.size());
            
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }
}

