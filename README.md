# 🎓 Kinderscape Enrollment System

A high-performance, robust, and secure desktop enrollment management system designed for school administrators, registrars, and teachers. Built on **Spring Boot** and **JavaFX 21**, the system features a local **SQLite** database optimized for concurrency with WAL (Write-Ahead Logging) mode, asynchronous UI threading, automated school year transitions, and custom database schema correction tools.

---

## 🚀 Key Features

### 👤 Admin Portal
* **Dashboard Summary**: Real-time statistics of enrolled vs. pending students, strand and section counts, gender distribution, and active school years.
* **Teacher Management**: Create, edit, activate, or deactivate teacher accounts, and assign subjects/sections (up to 8 assignments per teacher).
* **School Year Management**: Complete control over school years, start/end dates, and automated creation of semesters.
* **System Transitions**: Seamless transition mechanisms to carry over student records, auto-promote grade levels, and set previous year enrollments to pending.

### 📝 Registrar Portal
* **Student Lifecycle Management**: Add new students, search/filter active students, edit student profiles, and archive students to system logs (dropped, graduated, transferred).
* **Section & Strand Management**: Create and configure academic strands (STEM, ABM, HUMSS, GAS, TVL) and setup sections with custom seat capacities.
* **Excel & PDF Reports**: Generate and export section lists, teacher assignments, and enrollment summaries directly into `.xlsx` spreadsheet files and `.pdf` documents.

### 👨‍🏫 Teacher Portal
* **Subject Dashboard**: Dedicated dashboard displaying assigned subjects, sections, and the current headcount of students.
* **Class Lists**: Access and search class lists for all assigned sections.
* **Profile Management**: Customize details and upload profile photos.

---

## 🛠️ Technology Stack

* **Language**: Java 17
* **Framework**: Spring Boot 3.2.0 (Spring Data JPA, Spring Security)
* **Frontend/UI**: JavaFX 21 (styled with vanilla CSS, layouts in FXML)
* **Database**: SQLite (configured with WAL mode, normal synchronous write-ahead log, and custom community dialects)
* **Libraries**: Apache POI (Excel export), Apache PDFBox (PDF export), Lombok

---

## 📥 Getting Started

### Prerequisites
* **Java Development Kit (JDK)**: Version 17 or higher.
* **Maven**: (Optional, as the project includes the Maven Wrapper `mvnw`).

### Installation & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/keithjntlla/Kinderscape.git
   cd Kinderscape
   ```

2. Run the application using the Maven Wrapper:
   
   **On Windows (Command Prompt / PowerShell):**
   ```powershell
   .\mvnw.cmd javafx:run
   ```
   
   **On macOS / Linux:**
   ```bash
   chmod +x mvnw
   ./mvnw javafx:run
   ```

---

## 🔐 Default Credentials
On the first run, the database is auto-seeded with default administrative accounts:

| Role | Username | Password | Email |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | `admin@kinderscape.edu.ph` |
| **School Registrar** | `registrar` | `registrar123` | `registrar@kinderscape.edu.ph` |

*Note: For security reasons, please change default passwords after logging in.*

---

## 📁 Project Directory Structure
```
Kinderscape/
├── src/
│   ├── main/
│   │   ├── java/com/enrollment/system/
│   │   │   ├── config/       # Security, DB initialization, and configuration settings
│   │   │   ├── controller/   # JavaFX controllers managing UI interactions
│   │   │   ├── dto/          # Data Transfer Objects for API-to-UI layer communication
│   │   │   ├── model/        # JPA Database Entities
│   │   │   ├── repository/   # Spring Data JPA Repository Interfaces
│   │   │   ├── service/      # Services holding core business logic
│   │   │   └── util/         # DB schema correction, session helpers, utilities
│   │   └── resources/
│   │       ├── FXML/         # FXML layout structures
│   │       └── application.properties # Database, Server, and upload configurations
└── pom.xml                   # Maven dependencies and plugin build descriptors
```

---

## 🔧 Database Maintenance (Auto-fix Utilities)
SQLite database migrations are notoriously difficult due to limited DDL features. The project implements a standalone database fixer utility that runs automatically during startup:
* **Schema Correction**: Scans the database at boot time, checks the structural integrity of complex tables (such as `teacher_assignments`), and resolves mismatching column definitions or missing indices without losing active data schema configurations.
