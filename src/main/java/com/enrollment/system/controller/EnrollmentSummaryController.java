package com.enrollment.system.controller;

import com.enrollment.system.service.ReportService;
import com.enrollment.system.service.ReportService.EnrollmentStatistics;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
public class EnrollmentSummaryController implements Initializable {
    
    @FXML
    private VBox mainContainer;
    
    @FXML
    private ScrollPane scrollPane;
    
    @FXML
    private Button exportPdfButton;
    
    @FXML
    private Button exportExcelButton;
    
    @Autowired
    private ReportService reportService;
    
    private EnrollmentStatistics statistics;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: transparent;");
        
        loadData();
    }
    
    private void loadData() {
        new Thread(() -> {
            try {
                statistics = reportService.getEnrollmentStatistics();
                
                Platform.runLater(() -> {
                    buildSummaryView();
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
    
    private void buildSummaryView() {
        // Main container - vertical layout
        VBox mainContainer = new VBox(12);
        mainContainer.setPadding(new Insets(12));
        mainContainer.setStyle("-fx-background-color: #ecf0f1;");
        
        // TOP ROW: Summary Cards
        HBox summaryCards = new HBox(15);
        summaryCards.setAlignment(Pos.CENTER);
        summaryCards.setPrefWidth(Region.USE_COMPUTED_SIZE);
        
        VBox totalEnrolledCard = createStatCard("Total Enrolled", String.valueOf(statistics.totalEnrolled));
        VBox pendingCard = createStatCard("Pending Applications", String.valueOf(statistics.totalPending));
        
        // Make cards larger to match bigger graphs
        totalEnrolledCard.setPrefWidth(350);
        pendingCard.setPrefWidth(350);
        totalEnrolledCard.setMaxWidth(350);
        pendingCard.setMaxWidth(350);
        
        summaryCards.getChildren().addAll(totalEnrolledCard, pendingCard);
        mainContainer.getChildren().add(summaryCards);
        
        // BOTTOM SECTION: 2x2 Grid of Graphs - Bigger size
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(15);
        chartsGrid.setVgap(15);
        chartsGrid.setAlignment(Pos.CENTER);
        chartsGrid.setPrefWidth(Region.USE_COMPUTED_SIZE);
        GridPane.setHgrow(chartsGrid, Priority.ALWAYS);
        
        // Grade Level Chart - Bigger size
        VBox gradeLevelContainer = createChartContainer("Students by Grade Level", createGradeLevelChart());
        gradeLevelContainer.setPrefWidth(400);
        gradeLevelContainer.setPrefHeight(280);
        gradeLevelContainer.setMaxWidth(400);
        gradeLevelContainer.setMaxHeight(280);
        chartsGrid.add(gradeLevelContainer, 0, 0);
        
        // Strand Chart - Bigger size
        VBox strandContainer = createChartContainer("Students by Strand", createStrandChart());
        strandContainer.setPrefWidth(400);
        strandContainer.setPrefHeight(280);
        strandContainer.setMaxWidth(400);
        strandContainer.setMaxHeight(280);
        chartsGrid.add(strandContainer, 1, 0);
        
        // Gender Pie Chart - Bigger size
        VBox genderContainer = createChartContainer("Students by Gender", createGenderChart());
        genderContainer.setPrefWidth(400);
        genderContainer.setPrefHeight(280);
        genderContainer.setMaxWidth(400);
        genderContainer.setMaxHeight(280);
        chartsGrid.add(genderContainer, 0, 1);
        
        // Enrollment Status Chart - 4th graph showing Enrolled vs Pending
        VBox statusContainer = createChartContainer("Enrollment Status", createEnrollmentStatusChart());
        statusContainer.setPrefWidth(400);
        statusContainer.setPrefHeight(280);
        statusContainer.setMaxWidth(400);
        statusContainer.setMaxHeight(280);
        chartsGrid.add(statusContainer, 1, 1);
        
        mainContainer.getChildren().add(chartsGrid);
        VBox.setVgrow(chartsGrid, Priority.ALWAYS);
        
        // Set as content - no scrolling needed
        scrollPane.setContent(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }
    
    private Label createTableLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #2c3e50; " +
            "-fx-padding: 8 12; " +
            "-fx-background-color: #34495e; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: white;"
        );
        return label;
    }
    
    private VBox createChartContainer(String title, javafx.scene.Node chart) {
        VBox container = new VBox(8);
        container.setPadding(new Insets(12));
        container.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(44,62,80,0.1), 5, 0, 0, 2); " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8;"
        );
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #2c3e50; " +
            "-fx-padding: 6 10; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 4;"
        );
        
        // Set chart size - bigger to fill screen
        if (chart instanceof BarChart) {
            ((BarChart<?, ?>) chart).setPrefWidth(380);
            ((BarChart<?, ?>) chart).setPrefHeight(250);
        } else if (chart instanceof PieChart) {
            ((PieChart) chart).setPrefWidth(380);
            ((PieChart) chart).setPrefHeight(250);
        }
        
        container.getChildren().addAll(titleLabel, chart);
        
        // Subtle hover effect matching sidebar theme
        container.setOnMouseEntered(e -> {
            container.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(44,62,80,0.15), 8, 0, 0, 3); " +
                "-fx-border-color: #34495e; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        
        container.setOnMouseExited(e -> {
            container.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(44,62,80,0.1), 5, 0, 0, 2); " +
                "-fx-border-color: #bdc3c7; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: default;"
            );
        });
        
        return container;
    }
    
    private VBox createStatCard(String title, String value) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setPrefHeight(120); // Compact height for summary cards
        card.setMinHeight(120);
        card.setMaxHeight(120);
        
        // Use sidebar theme colors - same style as graph containers
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(44,62,80,0.1), 5, 0, 0, 2); " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8;"
        );
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #2c3e50; " +
            "-fx-padding: 6 10; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 4;"
        );
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle(
            "-fx-font-size: 36px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #2c3e50;"
        );
        
        card.getChildren().addAll(titleLabel, valueLabel);
        
        // Subtle hover effect matching sidebar theme - same as graph containers
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(44,62,80,0.15), 8, 0, 0, 3); " +
                "-fx-border-color: #34495e; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: hand;"
            );
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(44,62,80,0.1), 5, 0, 0, 2); " +
                "-fx-border-color: #bdc3c7; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-cursor: default;"
            );
        });
        
        return card;
    }
    
    private BarChart<String, Number> createGradeLevelChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        
        // Style axes
        xAxis.setStyle("-fx-tick-label-font-size: 11px; -fx-tick-label-fill: #2c3e50;");
        yAxis.setStyle("-fx-tick-label-font-size: 11px; -fx-tick-label-fill: #2c3e50;");
        
        // Sidebar theme colors for grade levels - subtle variations
        String[] colors = {"#2c3e50", "#34495e", "#3d4f5f", "#2c3e50", "#34495e", "#3d4f5f"};
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (statistics.byGradeLevel != null) {
            List<Map.Entry<Integer, Long>> sortedEntries = new ArrayList<>(statistics.byGradeLevel.entrySet());
            sortedEntries.sort(Map.Entry.comparingByKey());
            
            for (Map.Entry<Integer, Long> entry : sortedEntries) {
                XYChart.Data<String, Number> data = new XYChart.Data<>("G" + entry.getKey(), entry.getValue());
                series.getData().add(data);
            }
        }
        chart.getData().add(series);
        
        // Apply colors to bars when nodes are available
        chart.getData().get(0).getData().forEach(data -> {
            int index = chart.getData().get(0).getData().indexOf(data);
            String color = colors[index % colors.length];
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + color + ";");
                }
            });
            // Try to apply immediately if node already exists
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: " + color + ";");
            }
        });
        
        return chart;
    }
    
    private BarChart<String, Number> createStrandChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        
        // Style axes
        xAxis.setStyle("-fx-tick-label-font-size: 10px; -fx-tick-label-fill: #2c3e50;");
        yAxis.setStyle("-fx-tick-label-font-size: 11px; -fx-tick-label-fill: #2c3e50;");
        
        // Sidebar theme colors for strands - subtle variations
        Map<String, String> strandColors = new HashMap<>();
        strandColors.put("ABM", "#2c3e50");      // Dark
        strandColors.put("HUMSS", "#34495e");    // Medium
        strandColors.put("STEM", "#3d4f5f");     // Lighter
        strandColors.put("GAS", "#2c3e50");      // Dark
        strandColors.put("TVL", "#34495e");      // Medium
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (statistics.byStrand != null) {
            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(statistics.byStrand.entrySet());
            sortedEntries.sort(Map.Entry.comparingByKey());
            
            for (Map.Entry<String, Long> entry : sortedEntries) {
                String strandName = entry.getKey();
                // Shorten long strand names
                if (strandName.length() > 12) {
                    strandName = strandName.substring(0, 9) + "...";
                }
                XYChart.Data<String, Number> data = new XYChart.Data<>(strandName, entry.getValue());
                series.getData().add(data);
            }
        }
        chart.getData().add(series);
        
        // Apply strand-specific colors when nodes are available
        if (statistics.byStrand != null) {
            List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(statistics.byStrand.entrySet());
            sortedEntries.sort(Map.Entry.comparingByKey());
            
            for (int i = 0; i < chart.getData().get(0).getData().size(); i++) {
                String strand = sortedEntries.get(i).getKey();
                String color = strandColors.getOrDefault(strand.toUpperCase(), "#3498db");
                XYChart.Data<String, Number> data = chart.getData().get(0).getData().get(i);
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + color + ";");
                    }
                });
                // Try to apply immediately if node already exists
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-bar-fill: " + color + ";");
                }
            }
        }
        
        return chart;
    }
    
    private PieChart createGenderChart() {
        PieChart chart = new PieChart();
        chart.setTitle("");
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(10);
        chart.setAnimated(false);
        
        // Sidebar theme colors for gender - subtle variations
        Map<String, String> genderColors = new HashMap<>();
        genderColors.put("Male", "#2c3e50");      // Dark
        genderColors.put("Female", "#34495e");    // Medium
        genderColors.put("M", "#2c3e50");
        genderColors.put("F", "#34495e");
        
        if (statistics.byGender != null) {
            statistics.byGender.forEach((gender, count) -> {
                PieChart.Data slice = new PieChart.Data(gender + " (" + count + ")", count);
                chart.getData().add(slice);
            });
        }
        
        // Apply colors to pie slices after chart is rendered
        chart.getData().forEach(data -> {
            // Use a listener to apply color when node is available
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    String gender = data.getName().split(" ")[0];
                    String color = genderColors.getOrDefault(gender, "#95a5a6");
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                }
            });
        });
        
        return chart;
    }
    
    private PieChart createEnrollmentStatusChart() {
        PieChart chart = new PieChart();
        chart.setTitle("");
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(10);
        chart.setAnimated(false);
        
        // Sidebar theme colors for enrollment status
        Map<String, String> statusColors = new HashMap<>();
        statusColors.put("Enrolled", "#27ae60");    // Green for enrolled
        statusColors.put("Pending", "#e67e22");      // Orange for pending
        
        // Add enrolled slice
        if (statistics.totalEnrolled > 0) {
            PieChart.Data enrolledSlice = new PieChart.Data("Enrolled (" + statistics.totalEnrolled + ")", statistics.totalEnrolled);
            chart.getData().add(enrolledSlice);
        }
        
        // Add pending slice
        if (statistics.totalPending > 0) {
            PieChart.Data pendingSlice = new PieChart.Data("Pending (" + statistics.totalPending + ")", statistics.totalPending);
            chart.getData().add(pendingSlice);
        }
        
        // Apply colors to pie slices
        chart.getData().forEach(data -> {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    String status = data.getName().split(" ")[0];
                    String color = statusColors.getOrDefault(status, "#95a5a6");
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                }
            });
        });
        
        return chart;
    }
    
    private TableView<Map<String, String>> createStatisticsTable(String col1Header, String col2Header, Map<?, Long> data, VBox container) {
        TableView<Map<String, String>> table = new TableView<>();
        table.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-table-header-border-color: transparent; " +
            "-fx-table-cell-border-color: #ecf0f1;"
        );
        
        TableColumn<Map<String, String>, String> col1 = new TableColumn<>(col1Header);
        col1.setCellValueFactory(rowData -> {
            Map<String, String> row = rowData.getValue();
            return new javafx.beans.property.SimpleStringProperty(row.get("key"));
        });
        // Auto-adjust width - use 60% of available space
        col1.setPrefWidth(Region.USE_COMPUTED_SIZE);
        col1.setMinWidth(200);
        
        TableColumn<Map<String, String>, String> col2 = new TableColumn<>(col2Header);
        col2.setCellValueFactory(rowData -> {
            Map<String, String> row = rowData.getValue();
            return new javafx.beans.property.SimpleStringProperty(row.get("value"));
        });
        // Auto-adjust width - use 40% of available space
        col2.setPrefWidth(Region.USE_COMPUTED_SIZE);
        col2.setMinWidth(100);
        
        @SuppressWarnings("unchecked")
        TableColumn<Map<String, String>, String>[] columns = new TableColumn[] {col1, col2};
        table.getColumns().addAll(columns);
        
        javafx.collections.ObservableList<Map<String, String>> tableData = javafx.collections.FXCollections.observableArrayList();
        data.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(java.util.Comparator.reverseOrder()))
                .forEach(entry -> {
                    Map<String, String> row = new HashMap<>();
                    row.put("key", entry.getKey().toString());
                    row.put("value", String.valueOf(entry.getValue()));
                    tableData.add(row);
                });
        
        table.setItems(tableData);
        // Auto-resize columns to fit content
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Set columns to grow proportionally
        col1.prefWidthProperty().bind(table.widthProperty().multiply(0.6));
        col2.prefWidthProperty().bind(table.widthProperty().multiply(0.4));
        
        // If container is provided, calculate fixed cell size to fill available height
        if (container != null) {
            int rowCount = tableData.size();
            if (rowCount > 0) {
                // Use a listener to update cell size when container height changes
                container.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                    if (newHeight.doubleValue() > 0) {
                        // Subtract label height (~40px) and spacing (~10px)
                        double availableHeight = newHeight.doubleValue() - 50;
                        double cellSize = Math.max(25, availableHeight / rowCount);
                        table.setFixedCellSize(cellSize);
                    }
                });
                // Set initial cell size
                Platform.runLater(() -> {
                    if (container.getHeight() > 0) {
                        double availableHeight = container.getHeight() - 50;
                        double cellSize = Math.max(25, availableHeight / rowCount);
                        table.setFixedCellSize(cellSize);
                    } else {
                        // Fallback: use reasonable default
                        table.setFixedCellSize(30);
                    }
                });
            } else {
                table.setFixedCellSize(30);
            }
        } else {
            // Default cell size for tables without container binding
            table.setFixedCellSize(30);
        }
        
        // Style cells with sidebar theme - ensure text is fully visible
        col1.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<Map<String, String>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle(
                            "-fx-font-size: 12px; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-padding: 10; " +
                            "-fx-background-color: white; " +
                            "-fx-wrap-text: true;"
                        );
                    }
                }
            };
        });
        
        col2.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<Map<String, String>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle(
                            "-fx-font-size: 13px; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 10; " +
                            "-fx-background-color: #ecf0f1; " +
                            "-fx-alignment: center;"
                        );
                    }
                }
            };
        });
        
        // Style header with sidebar theme
        Platform.runLater(() -> {
            javafx.scene.Node header = table.lookup(".column-header-background");
            if (header != null) {
                header.setStyle(
                    "-fx-background-color: #34495e;"
                );
            }
        });
        
        return table;
    }
    
    @FXML
    private void exportToPDF() {
        if (statistics == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Data");
            alert.setHeaderText("No statistics available");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Enrollment_Summary_Report.pdf");
        
        Stage stage = (Stage) exportPdfButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            new Thread(() -> {
                try {
                    generatePDF(file);
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
        if (statistics == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Data");
            alert.setHeaderText("No statistics available");
            alert.showAndWait();
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Enrollment_Summary_Report.xlsx");
        
        Stage stage = (Stage) exportExcelButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            new Thread(() -> {
                try {
                    generateExcel(file);
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
    
    private void generatePDF(File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Use landscape orientation (swap width and height)
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                float margin = 40;
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float yPosition = pageHeight - margin;
                
                // Use default fonts
                PDType1Font fontBold = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font font = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
                
                // Title
                contentStream.beginText();
                contentStream.setFont(fontBold, 18);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("ENROLLMENT SUMMARY REPORT");
                contentStream.endText();
                
                yPosition -= 25;
                
                // Date
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                contentStream.endText();
                
                yPosition -= 35;
                
                // Table settings
                float tableStartX = margin;
                float tableWidth = pageWidth - (2 * margin);
                float rowHeight = 22;
                float col1Width = tableWidth * 0.7f;
                float col2Width = tableWidth * 0.3f;
                
                // Summary Table
                yPosition = drawTablePDF(contentStream, fontBold, font, tableStartX, yPosition, tableWidth, col1Width, col2Width, rowHeight,
                    "SUMMARY",
                    Arrays.asList(
                        new String[]{"Total Enrolled", String.valueOf(statistics.totalEnrolled)},
                        new String[]{"Pending Applications", String.valueOf(statistics.totalPending)}
                    ));
                yPosition -= 20;
                
                // Grade Level Table
                if (statistics.byGradeLevel != null && !statistics.byGradeLevel.isEmpty()) {
                    yPosition = drawTablePDF(contentStream, fontBold, font, tableStartX, yPosition, tableWidth, col1Width, col2Width, rowHeight,
                        "STUDENTS BY GRADE LEVEL",
                        statistics.byGradeLevel.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> new String[]{"Grade " + e.getKey(), String.valueOf(e.getValue())})
                            .collect(java.util.stream.Collectors.toList()));
                    yPosition -= 20;
                }
                
                // Strand Table
                if (statistics.byStrand != null && !statistics.byStrand.isEmpty()) {
                    yPosition = drawTablePDF(contentStream, fontBold, font, tableStartX, yPosition, tableWidth, col1Width, col2Width, rowHeight,
                        "STUDENTS BY STRAND",
                        statistics.byStrand.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> new String[]{e.getKey(), String.valueOf(e.getValue())})
                            .collect(java.util.stream.Collectors.toList()));
                    yPosition -= 20;
                }
                
                // Gender Table
                if (statistics.byGender != null && !statistics.byGender.isEmpty()) {
                    yPosition = drawTablePDF(contentStream, fontBold, font, tableStartX, yPosition, tableWidth, col1Width, col2Width, rowHeight,
                        "STUDENTS BY GENDER",
                        statistics.byGender.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(e -> new String[]{e.getKey(), String.valueOf(e.getValue())})
                            .collect(java.util.stream.Collectors.toList()));
                    yPosition -= 20;
                }
                
                // Section Table
                if (statistics.bySection != null && !statistics.bySection.isEmpty()) {
                    // Check if we need a new page
                    int sectionCount = statistics.bySection.size();
                    float estimatedHeight = 50 + (sectionCount * rowHeight);
                    if (yPosition - estimatedHeight < margin) {
                        // Close current content stream properly
                        contentStream.close();
                        // Create new page
                        page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = pageHeight - margin;
                    }
                    
                    yPosition = drawTablePDF(contentStream, fontBold, font, tableStartX, yPosition, tableWidth, col1Width, col2Width, rowHeight,
                        "STUDENTS BY SECTION",
                        statistics.bySection.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .map(e -> new String[]{e.getKey(), String.valueOf(e.getValue())})
                            .collect(java.util.stream.Collectors.toList()));
                }
                
            } finally {
                contentStream.close();
            }
            
            document.save(file);
        }
    }
    
    private float drawTablePDF(PDPageContentStream contentStream, PDType1Font fontBold, PDType1Font font,
                               float startX, float startY, float tableWidth, float col1Width, float col2Width, 
                               float rowHeight, String header, List<String[]> data) throws IOException {
        float cellPadding = 8;
        float yPos = startY;
        float headerHeight = rowHeight;
        
        // Draw table header with two columns
        contentStream.setNonStrokingColor(0.1f, 0.2f, 0.3f); // Dark blue-gray like Excel
        contentStream.addRect(startX, yPos - headerHeight, col1Width, headerHeight);
        contentStream.fill();
        contentStream.addRect(startX + col1Width, yPos - headerHeight, col2Width, headerHeight);
        contentStream.fill();
        
        // Draw header borders
        contentStream.setStrokingColor(0.0f, 0.0f, 0.0f);
        contentStream.setLineWidth(1.0f);
        // Top border
        contentStream.moveTo(startX, yPos);
        contentStream.lineTo(startX + tableWidth, yPos);
        contentStream.stroke();
        // Bottom border
        contentStream.moveTo(startX, yPos - headerHeight);
        contentStream.lineTo(startX + tableWidth, yPos - headerHeight);
        contentStream.stroke();
        // Vertical border between columns
        contentStream.moveTo(startX + col1Width, yPos);
        contentStream.lineTo(startX + col1Width, yPos - headerHeight);
        contentStream.stroke();
        // Left border
        contentStream.moveTo(startX, yPos);
        contentStream.lineTo(startX, yPos - headerHeight);
        contentStream.stroke();
        // Right border
        contentStream.moveTo(startX + tableWidth, yPos);
        contentStream.lineTo(startX + tableWidth, yPos - headerHeight);
        contentStream.stroke();
        
        // Header text - Column 1
        contentStream.setNonStrokingColor(1f, 1f, 1f); // White text
        contentStream.beginText();
        contentStream.setFont(fontBold, 11);
        contentStream.newLineAtOffset(startX + cellPadding, yPos - headerHeight + 7);
        contentStream.showText(header);
        contentStream.endText();
        
        // Header text - Column 2
        contentStream.beginText();
        contentStream.setFont(fontBold, 11);
        contentStream.newLineAtOffset(startX + col1Width + cellPadding, yPos - headerHeight + 7);
        contentStream.showText("COUNT");
        contentStream.endText();
        
        yPos -= headerHeight;
        
        // Draw data rows
        boolean alternate = false;
        for (String[] row : data) {
            // Fill background
            if (alternate) {
                contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f); // Light gray
            } else {
                contentStream.setNonStrokingColor(1f, 1f, 1f); // White
            }
            contentStream.addRect(startX, yPos - rowHeight, col1Width, rowHeight);
            contentStream.fill();
            contentStream.addRect(startX + col1Width, yPos - rowHeight, col2Width, rowHeight);
            contentStream.fill();
            
            // Draw borders for this row
            contentStream.setStrokingColor(0.7f, 0.7f, 0.7f);
            contentStream.setLineWidth(0.5f);
            // Top border
            contentStream.moveTo(startX, yPos);
            contentStream.lineTo(startX + tableWidth, yPos);
            contentStream.stroke();
            // Bottom border
            contentStream.moveTo(startX, yPos - rowHeight);
            contentStream.lineTo(startX + tableWidth, yPos - rowHeight);
            contentStream.stroke();
            // Vertical border between columns
            contentStream.moveTo(startX + col1Width, yPos);
            contentStream.lineTo(startX + col1Width, yPos - rowHeight);
            contentStream.stroke();
            // Left border
            contentStream.moveTo(startX, yPos);
            contentStream.lineTo(startX, yPos - rowHeight);
            contentStream.stroke();
            // Right border
            contentStream.moveTo(startX + tableWidth, yPos);
            contentStream.lineTo(startX + tableWidth, yPos - rowHeight);
            contentStream.stroke();
            
            // Draw text
            contentStream.setNonStrokingColor(0f, 0f, 0f); // Black text
            // Column 1 text
            contentStream.beginText();
            contentStream.setFont(font, 10);
            contentStream.newLineAtOffset(startX + cellPadding, yPos - rowHeight + 7);
            String text1 = row[0];
            if (text1.length() > 50) {
                text1 = text1.substring(0, 47) + "...";
            }
            contentStream.showText(text1);
            contentStream.endText();
            
            // Column 2 text (right-aligned like Excel)
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            // Approximate text width: ~6 pixels per character for font size 10
            float textWidth = row[1].length() * 6f;
            float rightAlignX = startX + col1Width + col2Width - cellPadding - textWidth;
            contentStream.newLineAtOffset(Math.max(startX + col1Width + cellPadding, rightAlignX), yPos - rowHeight + 7);
            contentStream.showText(row[1]);
            contentStream.endText();
            
            yPos -= rowHeight;
            alternate = !alternate;
        }
        
        return yPos;
    }
    
    private void generateExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Enrollment Summary");
            
            // Set print orientation to landscape
            org.apache.poi.ss.usermodel.PrintSetup printSetup = sheet.getPrintSetup();
            printSetup.setLandscape(true);
            printSetup.setFitWidth((short) 1);
            printSetup.setFitHeight((short) 0);
            
            // Create styles
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            
            CellStyle alternateStyle = workbook.createCellStyle();
            alternateStyle.cloneStyleFrom(dataStyle);
            alternateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            alternateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            
            CellStyle numberAlternateStyle = workbook.createCellStyle();
            numberAlternateStyle.cloneStyleFrom(alternateStyle);
            numberAlternateStyle.setAlignment(HorizontalAlignment.RIGHT);
            
            int rowNum = 0;
            
            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ENROLLMENT SUMMARY REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));
            
            // Date
            Row dateRow = sheet.createRow(rowNum++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));
            
            rowNum++; // Empty row
            
            // Summary Table
            Row summaryHeader = sheet.createRow(rowNum++);
            Cell sumHeader1 = summaryHeader.createCell(0);
            sumHeader1.setCellValue("SUMMARY");
            sumHeader1.setCellStyle(headerStyle);
            Cell sumHeader2 = summaryHeader.createCell(1);
            sumHeader2.setCellValue("");
            sumHeader2.setCellStyle(headerStyle);
            
            Row enrolledRow = sheet.createRow(rowNum++);
            Cell enrolledLabel = enrolledRow.createCell(0);
            enrolledLabel.setCellValue("Total Enrolled");
            enrolledLabel.setCellStyle(dataStyle);
            Cell enrolledValue = enrolledRow.createCell(1);
            enrolledValue.setCellValue(statistics.totalEnrolled);
            enrolledValue.setCellStyle(numberStyle);
            
            Row pendingRow = sheet.createRow(rowNum++);
            Cell pendingLabel = pendingRow.createCell(0);
            pendingLabel.setCellValue("Pending Applications");
            pendingLabel.setCellStyle(alternateStyle);
            Cell pendingValue = pendingRow.createCell(1);
            pendingValue.setCellValue(statistics.totalPending);
            pendingValue.setCellStyle(numberAlternateStyle);
            
            rowNum++; // Empty row
            
            // Grade Level Table
            if (statistics.byGradeLevel != null && !statistics.byGradeLevel.isEmpty()) {
                rowNum = createTableSection(sheet, rowNum, "STUDENTS BY GRADE LEVEL",
                    statistics.byGradeLevel.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new Object[]{"Grade " + e.getKey(), e.getValue()})
                        .collect(java.util.stream.Collectors.toList()),
                    headerStyle, dataStyle, alternateStyle, numberStyle, numberAlternateStyle);
                rowNum++;
            }
            
            // Strand Table
            if (statistics.byStrand != null && !statistics.byStrand.isEmpty()) {
                rowNum = createTableSection(sheet, rowNum, "STUDENTS BY STRAND",
                    statistics.byStrand.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new Object[]{e.getKey(), e.getValue()})
                        .collect(java.util.stream.Collectors.toList()),
                    headerStyle, dataStyle, alternateStyle, numberStyle, numberAlternateStyle);
                rowNum++;
            }
            
            // Gender Table
            if (statistics.byGender != null && !statistics.byGender.isEmpty()) {
                rowNum = createTableSection(sheet, rowNum, "STUDENTS BY GENDER",
                    statistics.byGender.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> new Object[]{e.getKey(), e.getValue()})
                        .collect(java.util.stream.Collectors.toList()),
                    headerStyle, dataStyle, alternateStyle, numberStyle, numberAlternateStyle);
                rowNum++;
            }
            
            // Section Table
            if (statistics.bySection != null && !statistics.bySection.isEmpty()) {
                rowNum = createTableSection(sheet, rowNum, "STUDENTS BY SECTION",
                    statistics.bySection.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .map(e -> new Object[]{e.getKey(), e.getValue()})
                        .collect(java.util.stream.Collectors.toList()),
                    headerStyle, dataStyle, alternateStyle, numberStyle, numberAlternateStyle);
            }
            
            // Auto-size columns
            sheet.setColumnWidth(0, 15000); // ~150px
            sheet.setColumnWidth(1, 5000);  // ~50px
            
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }
    
    private int createTableSection(Sheet sheet, int startRow, String header, List<Object[]> data,
                                   CellStyle headerStyle, CellStyle dataStyle, CellStyle alternateStyle,
                                   CellStyle numberStyle, CellStyle numberAlternateStyle) {
        int rowNum = startRow;
        
        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue(header);
        headerCell1.setCellStyle(headerStyle);
        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("COUNT");
        headerCell2.setCellStyle(headerStyle);
        
        // Data rows
        boolean alternate = false;
        for (Object[] row : data) {
            Row dataRow = sheet.createRow(rowNum++);
            Cell cell1 = dataRow.createCell(0);
            cell1.setCellValue(row[0].toString());
            cell1.setCellStyle(alternate ? alternateStyle : dataStyle);
            
            Cell cell2 = dataRow.createCell(1);
            if (row[1] instanceof Number) {
                cell2.setCellValue(((Number) row[1]).doubleValue());
            } else {
                cell2.setCellValue(row[1].toString());
            }
            cell2.setCellStyle(alternate ? numberAlternateStyle : numberStyle);
            alternate = !alternate;
        }
        
        return rowNum;
    }
}

