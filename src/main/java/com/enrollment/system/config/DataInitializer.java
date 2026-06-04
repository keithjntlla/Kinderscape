package com.enrollment.system.config;

import com.enrollment.system.model.SchoolYear;
import com.enrollment.system.model.Section;
import com.enrollment.system.model.Semester;
import com.enrollment.system.model.Student;
import com.enrollment.system.model.Strand;
import com.enrollment.system.model.User;
import com.enrollment.system.repository.SchoolYearRepository;
import com.enrollment.system.repository.SectionRepository;
import com.enrollment.system.repository.SemesterRepository;
import com.enrollment.system.repository.StudentRepository;
import com.enrollment.system.repository.StrandRepository;
import com.enrollment.system.repository.UserRepository;
import com.enrollment.system.util.DatabaseSchemaUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private StrandRepository strandRepository;
    
    @Autowired(required = false)
    private SchoolYearRepository schoolYearRepository;
    
    @Autowired(required = false)
    private SemesterRepository semesterRepository;
    
    @Autowired(required = false)
    private com.enrollment.system.service.SemesterService semesterService;
    
    @Autowired(required = false)
    private com.enrollment.system.service.StudentService studentService;
    
    @Autowired(required = false)
    private DatabaseSchemaUpdater databaseSchemaUpdater;
    
    @Autowired(required = false)
    private com.enrollment.system.repository.SubjectRepository subjectRepository;
    
    @Autowired(required = false)
    private com.enrollment.system.service.TeacherService teacherService;
    
    @Autowired(required = false)
    private com.enrollment.system.util.DatabaseSchemaFixer databaseSchemaFixer;
    
    @Autowired(required = false)
    private com.enrollment.system.util.TeacherSubjectAssigner teacherSubjectAssigner;
    
    @Override
    public void run(String... args) throws Exception {
        // NOTE: Removed code that was clearing all teacher assignments on startup
        // This was causing data loss. Teacher assignments are now preserved across restarts.
        
        // Update database schema if needed (e.g., add TEACHER role support)
        if (databaseSchemaUpdater != null) {
            try {
                databaseSchemaUpdater.updateUsersTableForTeacherRole();
            } catch (Exception e) {
                System.err.println("⚠ Warning: Could not update database schema: " + e.getMessage());
                // Continue - don't block application startup
            }
            
            // Ensure teacher_assignments table exists with correct schema
            // NOTE: This will only fix schema if there are actual issues - it won't drop the table if schema is correct
            try {
                databaseSchemaUpdater.ensureTeacherAssignmentsTableExists();
            } catch (Exception e) {
                System.err.println("⚠ Warning: Could not verify teacher_assignments table: " + e.getMessage());
                // Do NOT force fix here - it will drop the table and delete all data!
                // Force fix should only be used manually when schema is corrupted
            }
        }
        
        // Comprehensive schema scan and fix
        // NOTE: Only scan and fix if there are actual schema issues - do NOT force fix on every startup
        // Force fixing drops and recreates the table, which deletes all data!
        if (databaseSchemaFixer != null) {
            try {
                // Scan to detect issues - this will only fix if there are actual problems
                // It will NOT drop the table if the schema is already correct
                databaseSchemaFixer.scanAndFixTeacherAssignmentsTable();
                // Do NOT call forceFixTeacherAssignmentsTable() here - it drops the table unconditionally!
            } catch (Exception e) {
                System.err.println("⚠ Warning: Schema scan/fix failed: " + e.getMessage());
                // Only log the error - do NOT force fix as it will delete all data
                // Force fix should only be used manually when schema is corrupted
            }
        }
        
        // Create default admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setEmail("admin@kinderscape.edu.ph");
            admin.setRole(User.UserRole.ADMIN);
            admin.setIsActive(true);
            
            userRepository.save(admin);
            System.out.println("✓ Default admin user created - Username: admin, Password: admin123");
        }
        
        // Create default registrar user if not exists
        if (!userRepository.existsByUsername("registrar")) {
            User registrar = new User();
            registrar.setUsername("registrar");
            registrar.setPassword(passwordEncoder.encode("registrar123"));
            registrar.setFullName("School Registrar");
            registrar.setEmail("registrar@kinderscape.edu.ph");
            registrar.setRole(User.UserRole.REGISTRAR);
            registrar.setIsActive(true);
            
            userRepository.save(registrar);
            System.out.println("✓ Default registrar user created - Username: registrar, Password: registrar123");
        }
        
        // Initialize school years first (before students)
        try {
            initializeSchoolYears();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to initialize school years: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Initialize strands
        try {
            initializeStrands();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to initialize strands: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Initialize sections (3 per strand for Grade 11 and Grade 12)
        try {
            initializeSections();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to initialize sections: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Initialize subjects (sample subjects for Grade 11 and Grade 12)
        try {
            initializeSubjects();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to initialize subjects: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Ensure semesters exist for all school years
        try {
            if (semesterService != null) {
                semesterService.ensureSemestersExistForAllSchoolYears();
            }
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to ensure semesters exist: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Initialize test student data - only if database is empty (first run)
        // NOTE: This is disabled to preserve user-added students across restarts
        // Uncomment the code below only if you need to reset the database with test data
        /*
        try {
            long existingStudentCount = studentRepository.count();
            if (existingStudentCount == 0) {
                // Only initialize test data if database is empty (first run)
                initializeTestStudents();
                addCapacityTestStudents();
                createComprehensiveStudentDataset();
            } else {
                System.out.println("✓ Database already contains " + existingStudentCount + " students. Skipping test data initialization to preserve user data.");
            }
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to initialize test student data: " + e.getMessage());
            e.printStackTrace();
        }
        */
        
        System.out.println("=".repeat(60));
        System.out.println("Kinderscape");
        System.out.println("Enrollment System Started Successfully");
        System.out.println("=".repeat(60));
    }
    
    private void initializeSchoolYears() {
        if (schoolYearRepository == null) {
            System.out.println("⚠ SchoolYearRepository not available - skipping school year initialization");
            return;
        }
        
        // Create 2025-2026 school year if it doesn't exist (set as current)
        SchoolYear sy2025 = schoolYearRepository.findByYear("2025-2026").orElse(null);
        if (sy2025 == null) {
            sy2025 = new SchoolYear();
            sy2025.setYear("2025-2026");
            sy2025.setStartDate(LocalDate.of(2025, 6, 1));
            sy2025.setEndDate(LocalDate.of(2026, 3, 31));
            sy2025.setIsCurrent(true); // Set as current
            schoolYearRepository.save(sy2025);
            System.out.println("✓ Created school year 2025-2026 and set as current");
            
            // Create semesters for the new school year
            if (semesterService != null) {
                try {
                    semesterService.createSemestersForSchoolYear(sy2025.getId());
                    System.out.println("✓ Created semesters for school year 2025-2026");
                } catch (Exception e) {
                    System.err.println("⚠ Warning: Could not create semesters: " + e.getMessage());
                }
            }
        } else if (!Boolean.TRUE.equals(sy2025.getIsCurrent())) {
            // If it exists but is not current, make it current
            // First unset any other current school year
            java.util.List<SchoolYear> allCurrent = schoolYearRepository.findAll().stream()
                .filter(sy -> Boolean.TRUE.equals(sy.getIsCurrent()))
                .collect(java.util.stream.Collectors.toList());
            for (SchoolYear sy : allCurrent) {
                sy.setIsCurrent(false);
                schoolYearRepository.save(sy);
            }
            sy2025.setIsCurrent(true);
            schoolYearRepository.save(sy2025);
            System.out.println("✓ Set school year 2025-2026 as current");
        }
        
        // Ensure semesters exist for 2025-2026 (even if school year already existed)
        if (sy2025 != null && semesterService != null) {
            try {
                java.util.List<com.enrollment.system.model.Semester> existingSemesters = 
                    semesterRepository != null ? semesterRepository.findBySchoolYearId(sy2025.getId()) : java.util.Collections.emptyList();
                if (existingSemesters.isEmpty()) {
                    semesterService.createSemestersForSchoolYear(sy2025.getId());
                    System.out.println("✓ Created semesters for existing school year 2025-2026");
                }
            } catch (Exception e) {
                System.err.println("⚠ Warning: Could not create semesters for 2025-2026: " + e.getMessage());
            }
        }
        
        // Create 2026-2027 school year if it doesn't exist
        SchoolYear sy2026 = schoolYearRepository.findByYear("2026-2027").orElse(null);
        if (sy2026 == null) {
            sy2026 = new SchoolYear();
            sy2026.setYear("2026-2027");
            sy2026.setStartDate(LocalDate.of(2026, 6, 1));
            sy2026.setEndDate(LocalDate.of(2027, 3, 31));
            sy2026.setIsCurrent(false); // Not current initially
            schoolYearRepository.save(sy2026);
            System.out.println("✓ Created school year 2026-2027");
            
            // Create semesters for the new school year
            if (semesterService != null) {
                try {
                    semesterService.createSemestersForSchoolYear(sy2026.getId());
                    System.out.println("✓ Created semesters for school year 2026-2027");
                } catch (Exception e) {
                    System.err.println("⚠ Warning: Could not create semesters: " + e.getMessage());
                }
            }
        }
        
        // Ensure semesters exist for 2026-2027 (even if school year already existed)
        if (sy2026 != null && semesterService != null) {
            try {
                java.util.List<com.enrollment.system.model.Semester> existingSemesters = 
                    semesterRepository != null ? semesterRepository.findBySchoolYearId(sy2026.getId()) : java.util.Collections.emptyList();
                if (existingSemesters.isEmpty()) {
                    semesterService.createSemestersForSchoolYear(sy2026.getId());
                    System.out.println("✓ Created semesters for existing school year 2026-2027");
                }
            } catch (Exception e) {
                System.err.println("⚠ Warning: Could not create semesters for 2026-2027: " + e.getMessage());
            }
        }
        
        // Assign all existing students without school year to 2025-2026 (current)
        java.util.List<Student> studentsWithoutYear = studentRepository.findAll().stream()
            .filter(s -> s.getSchoolYear() == null)
            .collect(java.util.stream.Collectors.toList());
        
        if (!studentsWithoutYear.isEmpty()) {
            for (Student student : studentsWithoutYear) {
                student.setSchoolYear(sy2025);
            }
            studentRepository.saveAll(studentsWithoutYear);
            System.out.println("✓ Assigned " + studentsWithoutYear.size() + " existing students to school year 2025-2026");
        }
    }
    
    private void initializeStrands() {
        String[] strandNames = {"ABM", "HUMSS", "STEM", "GAS", "TVL"};
        String[] descriptions = {
            "Accountancy, Business and Management",
            "Humanities and Social Sciences",
            "Science, Technology, Engineering and Mathematics",
            "General Academic Strand",
            "Technical-Vocational-Livelihood"
        };
        
        int strandsCreated = 0;
        for (int i = 0; i < strandNames.length; i++) {
            if (!strandRepository.existsByName(strandNames[i])) {
                Strand strand = new Strand();
                strand.setName(strandNames[i]);
                strand.setDescription(descriptions[i]);
                strand.setIsActive(true);
                
                strandRepository.save(strand);
                strandsCreated++;
            }
        }
        
        if (strandsCreated > 0) {
            System.out.println("✓ Created " + strandsCreated + " default strands");
        } else {
            System.out.println("✓ Strands already exist in database");
        }
    }
    
    private void initializeSections() {
        String[] strands = {"ABM", "HUMSS", "STEM", "GAS", "TVL"};
        Integer[] gradeLevels = {11, 12};
        String[] sectionNames = {"A", "B", "C"};
        
        int sectionsCreated = 0;
        for (String strand : strands) {
            for (Integer gradeLevel : gradeLevels) {
                for (String sectionName : sectionNames) {
                    String fullSectionName = strand + "-" + gradeLevel + sectionName;
                    
                    // Check if section already exists
                    if (!sectionRepository.existsByNameAndStrandAndGradeLevel(fullSectionName, strand, gradeLevel)) {
                        Section section = new Section();
                        section.setName(fullSectionName);
                        section.setStrand(strand);
                        section.setGradeLevel(gradeLevel);
                        section.setIsActive(true);
                        section.setCapacity(40); // Default capacity
                        
                        sectionRepository.save(section);
                        sectionsCreated++;
                    }
                }
            }
        }
        
        if (sectionsCreated > 0) {
            System.out.println("✓ Created " + sectionsCreated + " sections (3 per strand for Grade 11 and 12)");
        } else {
            System.out.println("✓ Sections already exist in database");
        }
    }
    
    private void initializeTestStudents() {
        // Clear all existing students first
        long existingCount = studentRepository.count();
        if (existingCount > 0) {
            System.out.println("✓ Clearing " + existingCount + " existing student records...");
            studentRepository.deleteAll();
            System.out.println("✓ All existing students cleared.");
        }
        
        System.out.println("✓ Creating 40 new test student records (20 Grade 11, 20 Grade 12) with complete information...");
        Random random = new Random();
        
        // Get active strands and sections for proper assignment
        java.util.List<Strand> activeStrands = strandRepository.findByIsActiveTrue();
        java.util.List<Section> activeSections = sectionRepository.findByIsActiveTrue();
        
        if (activeStrands.isEmpty()) {
            System.err.println("⚠ Warning: No active strands found. Cannot create students.");
            return;
        }
        
        // Get current school year
        SchoolYear currentSchoolYear = null;
        if (schoolYearRepository != null) {
            currentSchoolYear = schoolYearRepository.findByIsCurrentTrue().orElse(null);
            if (currentSchoolYear == null) {
                System.err.println("⚠ Warning: No current school year found. Cannot assign students to school year.");
            }
        }
        
        // Comprehensive Filipino first names - expanded list for unique combinations
        String[] firstNames = {
            "Maria", "Juan", "Jose", "Ana", "Carlos", "Rosa", "Pedro", "Carmen", "Miguel", "Elena",
            "Antonio", "Francisco", "Teresa", "Manuel", "Isabel", "Ricardo", "Dolores", "Fernando", "Patricia", "Roberto",
            "Gloria", "Alberto", "Mercedes", "Eduardo", "Concepcion", "Ramon", "Esperanza", "Alfredo", "Rosario", "Cristina",
            "Josefina", "Felipe", "Margarita", "Rafael", "Enrique", "Consuelo", "Jorge", "Amparo", "Sergio", "Lourdes",
            "Vicente", "Angelica", "Benjamin", "Cecilia", "Daniel", "Diana", "Emilio", "Felicia", "Gabriel", "Hannah",
            "Ignacio", "Irene", "Julian", "Katherine", "Leonardo", "Lydia", "Marcelo", "Natalia", "Oscar", "Paula",
            "Quentin", "Rebecca", "Sebastian", "Sofia", "Tomas", "Valentina", "William", "Yvette", "Zachary", "Andrea",
            "Adrian", "Bianca", "Christian", "Denise", "Ethan", "Fatima", "Gian", "Hazel", "Ivan", "Jasmine",
            "Kyle", "Lara", "Marcus", "Nicole", "Oliver", "Princess", "Quinn", "Raven", "Stephanie", "Tristan"
        };
        
        // Comprehensive Filipino last names
        String[] lastNames = {
            "Santos", "Reyes", "Cruz", "Bautista", "Villanueva", "Fernandez", "Ramos", "Torres", "Garcia", "Lopez",
            "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez", "Rivera", "Morales", "Ortiz", "Gutierrez", "Castillo",
            "Dela Cruz", "Mendoza", "Aquino", "Castro", "Romero", "Vargas", "Flores", "Herrera", "Jimenez", "Moreno",
            "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado", "Molina", "Vasquez", "Guerrero", "Pineda",
            "Alvarez", "Bautista", "Cortez", "Domingo", "Espinosa", "Fuentes", "Guzman", "Hernandez", "Ibarra", "Javier",
            "Kalaw", "Luna", "Maceda", "Nunez", "Ocampo", "Pascual", "Quizon", "Ramos", "Salazar", "Tolentino"
        };
        
        // Comprehensive middle names
        String[] middleNames = {
            "Cruz", "Reyes", "Santos", "Garcia", "Lopez", "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez",
            "Bautista", "Fernandez", "Ramos", "Torres", "Villanueva", "Mendoza", "Aquino", "Castro", "Romero", "Vargas",
            "Flores", "Herrera", "Jimenez", "Moreno", "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado",
            "Molina", "Vasquez", "Guerrero", "Pineda", "Alvarez", "Cortez", "Domingo", "Espinosa", "Fuentes", "Guzman"
        };
        
        // Parent relationships
        String[] relationships = {"Father", "Mother", "Guardian", "Other"};
        
        // Previous schools (all spelled out, no abbreviations)
        String[] previousSchools = {
            "Kinderscape Elementary School", "San Jose Elementary School", "Rizal Elementary School",
            "Bonifacio Elementary School", "Aguinaldo Elementary School", "Mabini Elementary School",
            "Quezon Elementary School", "Osmena Elementary School", "Roxas Elementary School", 
            "Magsaysay Elementary School", "Lapu-Lapu Elementary School", "Luna Elementary School"
        };
        
        // Addresses
        String[] addresses = {
            "Barangay Poblacion, Kinderscape", "Barangay San Jose, Kinderscape", "Barangay Rizal, Kinderscape",
            "Barangay Bonifacio, Kinderscape", "Barangay Aguinaldo, Kinderscape", "Barangay Mabini, Kinderscape",
            "Barangay Quezon, Kinderscape", "Barangay Osmena, Kinderscape", "Barangay Roxas, Kinderscape", 
            "Barangay Magsaysay, Kinderscape"
        };
        
        // Generate unique contact numbers to avoid duplicates
        java.util.Set<String> usedContacts = new java.util.HashSet<>();
        java.util.Set<String> usedLrns = new java.util.HashSet<>();
        java.util.Set<String> usedFullNames = new java.util.HashSet<>(); // Track full name combinations
        
        // Create shuffled lists for name components
        java.util.List<String> shuffledFirstNames = new java.util.ArrayList<>(java.util.Arrays.asList(firstNames));
        java.util.List<String> shuffledLastNames = new java.util.ArrayList<>(java.util.Arrays.asList(lastNames));
        java.util.List<String> shuffledMiddleNames = new java.util.ArrayList<>(java.util.Arrays.asList(middleNames));
        java.util.Collections.shuffle(shuffledFirstNames, random);
        java.util.Collections.shuffle(shuffledLastNames, random);
        java.util.Collections.shuffle(shuffledMiddleNames, random);
        
        int successCount = 0;
        int enrolledCount = 0;
        int pendingCount = 0;
        
        // Track students per strand per grade for even distribution
        // 5 strands, 2 grades = 10 groups, 4 students per group (40 total)
        int[][] strandGradeCount = new int[5][2]; // [strandIndex][gradeIndex: 0=G11, 1=G12]
        
        // Track section assignments per strand/grade for even distribution
        java.util.Map<String, Integer> sectionAssignmentCount = new java.util.HashMap<>();
        
        // Generate unique name combinations
        int nameIndex = 0;
        for (int i = 0; i < 40; i++) {
            try {
                Student student = new Student();
                
                // Generate unique full name combination (first + middle + last)
                String firstName, middleName, lastName, fullName;
                int attempts = 0;
                do {
                    firstName = shuffledFirstNames.get(nameIndex % shuffledFirstNames.size());
                    middleName = shuffledMiddleNames.get((nameIndex + i) % shuffledMiddleNames.size());
                    lastName = shuffledLastNames.get((nameIndex + i * 2) % shuffledLastNames.size());
                    fullName = firstName + " " + middleName + " " + lastName;
                    nameIndex++;
                    attempts++;
                    
                    // Safety check to prevent infinite loop
                    if (attempts > 1000) {
                        // Fallback: add a unique identifier
                        fullName = firstName + " " + middleName + " " + lastName + " " + (i + 1);
                        break;
                    }
                } while (usedFullNames.contains(fullName.toLowerCase()));
                
                usedFullNames.add(fullName.toLowerCase());
                student.setName(fullName);
                
                // Generate birthdate (age between 16-18 to meet validation requirements)
                int age = 16 + random.nextInt(3); // 16-18 years old (must be >= 16)
                LocalDate birthdate = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
                student.setBirthdate(birthdate);
                student.setAge(age);
                
                // Sex (required field)
                student.setSex(i % 2 == 0 ? "Male" : "Female");
                
                // Address (required)
                student.setAddress(addresses[i % addresses.length]);
                
                // Contact number (Philippine format: 09XXXXXXXXX, must be unique)
                String contact;
                do {
                    contact = "09" + String.format("%09d", 100000000 + random.nextInt(900000000));
                } while (usedContacts.contains(contact));
                usedContacts.add(contact);
                student.setContactNumber(contact);
                
                // Parent/Guardian information (all required)
                String parentFirstName = firstNames[(i + 5) % firstNames.length];
                String parentLastName = lastNames[(i + 3) % lastNames.length];
                student.setParentGuardianName(parentFirstName + " " + parentLastName);
                
                // Parent contact (must be different from student contact)
                String parentContact;
                do {
                    parentContact = "09" + String.format("%09d", 100000000 + random.nextInt(900000000));
                } while (usedContacts.contains(parentContact) || parentContact.equals(contact));
                usedContacts.add(parentContact);
                student.setParentGuardianContact(parentContact);
                student.setParentGuardianRelationship(relationships[i % relationships.length]);
                
                // Grade level (required field - 11 or 12)
                // First 20 are Grade 11, next 20 are Grade 12
                int gradeLevel = (i < 20) ? 11 : 12;
                int gradeIndex = (gradeLevel == 11) ? 0 : 1;
                student.setGradeLevel(gradeLevel);
                
                // Strand (required - distribute evenly: 4 students per strand per grade)
                // Calculate which strand this student should be assigned to
                int studentsInGrade = (i < 20) ? i : (i - 20);
                int strandIndex = (studentsInGrade / 4) % activeStrands.size(); // 4 students per strand
                Strand selectedStrand = activeStrands.get(strandIndex);
                student.setStrand(selectedStrand.getName());
                strandGradeCount[strandIndex][gradeIndex]++;
                
                // Previous school (required, spelled out)
                student.setPreviousSchool(previousSchools[i % previousSchools.length]);
                
                // GWA (required, between 75.0 and 100.0)
                double gwa = 75.0 + (random.nextDouble() * 25.0);
                student.setGwa(Math.round(gwa * 100.0) / 100.0);
                
                // LRN (required, exactly 12 digits, must be unique)
                String lrn;
                do {
                    lrn = String.format("%012d", 100000000000L + random.nextInt(900000000));
                } while (usedLrns.contains(lrn));
                usedLrns.add(lrn);
                student.setLrn(lrn);
                
                // Enrollment status - ALL students are Enrolled
                student.setEnrollmentStatus("Enrolled");
                
                // Section assignment (required for Enrolled students)
                // Find all sections matching the strand and grade level
                java.util.List<Section> matchingSections = activeSections.stream()
                        .filter(s -> selectedStrand.getName().equals(s.getStrand()) 
                                && gradeLevel == s.getGradeLevel())
                        .collect(java.util.stream.Collectors.toList());
                
                if (!matchingSections.isEmpty()) {
                    // Distribute students across available sections evenly
                    // Use a key to track assignments per strand/grade combination
                    String sectionKey = selectedStrand.getName() + "-" + gradeLevel;
                    int currentCount = sectionAssignmentCount.getOrDefault(sectionKey, 0);
                    int sectionIndex = currentCount % matchingSections.size();
                    Section assignedSection = matchingSections.get(sectionIndex);
                    student.setSection(assignedSection);
                    sectionAssignmentCount.put(sectionKey, currentCount + 1);
                    enrolledCount++;
                } else {
                    // If no matching section found, this is an error - sections should exist
                    System.err.println("⚠ Warning: No matching section found for " + selectedStrand.getName() + " Grade " + gradeLevel);
                    // Still enroll the student but without section (shouldn't happen)
                    student.setEnrollmentStatus("Pending");
                    student.setSection(null);
                    pendingCount++;
                }
                
                // Ensure not archived
                student.setIsArchived(false);
                
                // Assign to current school year (REQUIRED for all students)
                if (currentSchoolYear != null) {
                    student.setSchoolYear(currentSchoolYear);
                    
                    // Assign semester (REQUIRED for enrolled students)
                    // All students are enrolled, so assign Semester 1 for the grade level
                    if (semesterRepository != null) {
                        java.util.Optional<Semester> semester = semesterRepository
                            .findBySchoolYearIdAndGradeLevelAndSemesterNumber(
                                currentSchoolYear.getId(), gradeLevel, 1);
                        if (semester.isPresent()) {
                            student.setSemester(semester.get());
                        } else {
                            System.err.println("⚠ Warning: Semester not found for Grade " + gradeLevel + " Semester 1 in school year " + currentSchoolYear.getYear());
                            System.err.println("⚠ This student will not have a semester assigned. Please ensure semesters are created for the school year.");
                        }
                    } else {
                        System.err.println("⚠ Warning: SemesterRepository not available - student will not have semester assigned");
                    }
                } else {
                    System.err.println("⚠ Warning: No current school year - student will not have school year or semester assigned");
                }
                
                studentRepository.save(student);
                successCount++;
            } catch (Exception e) {
                System.err.println("⚠ Error creating student " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("✓ Successfully created " + successCount + " test student records with unique full names");
        System.out.println("  - Grade 11: 20 students");
        System.out.println("  - Grade 12: 20 students");
        System.out.println("  - Enrolled: " + enrolledCount + " students (all with sections and semesters assigned)");
        System.out.println("  - All students have unique first + middle + last name combinations");
        if (pendingCount > 0) {
            System.out.println("  - Pending: " + pendingCount + " students (no section found - check section configuration)");
        }
        
        // Print distribution summary
        System.out.println("\n  Strand Distribution:");
        String[] strandNames = {"ABM", "HUMSS", "STEM", "GAS", "TVL"};
        for (int s = 0; s < Math.min(activeStrands.size(), strandNames.length); s++) {
            System.out.println("    " + strandNames[s] + ": G11=" + strandGradeCount[s][0] + 
                             ", G12=" + strandGradeCount[s][1] + 
                             " (Total: " + (strandGradeCount[s][0] + strandGradeCount[s][1]) + ")");
        }
    }
    
    private void addCapacityTestStudents() {
        System.out.println("\n✓ Adding 40 students to specific sections for capacity testing...");
        
        // Find specific sections for testing
        // Using ABM and GAS strands - Grade 11 Section A and Grade 12 Section A
        // Specific to 2025-2026 Semester 1
        String[] testStrands = {"ABM", "GAS"}; // Test with both ABM and GAS
        Integer[] testGrades = {11, 12};
        String[] testSectionNames = {"A"}; // Section A for each grade
        String targetSchoolYear = "2025-2026";
        Integer targetSemesterNumber = 1; // Semester 1
        
        SchoolYear targetSchoolYearEntity = null;
        if (schoolYearRepository != null) {
            targetSchoolYearEntity = schoolYearRepository.findByYear(targetSchoolYear).orElse(null);
            if (targetSchoolYearEntity == null) {
                System.err.println("⚠ Warning: School year " + targetSchoolYear + " not found. Cannot add capacity test students.");
                return;
            }
        } else {
            System.err.println("⚠ Warning: SchoolYearRepository not available. Cannot add capacity test students.");
            return;
        }
        
        Random random = new Random();
        java.util.Set<String> usedContacts = new java.util.HashSet<>();
        java.util.Set<String> usedLrns = new java.util.HashSet<>();
        
        // Get existing contacts and LRNs to avoid duplicates
        java.util.List<Student> existingStudents = studentRepository.findAll();
        for (Student s : existingStudents) {
            if (s.getContactNumber() != null) usedContacts.add(s.getContactNumber());
            if (s.getLrn() != null) usedLrns.add(s.getLrn());
        }
        
        // Get existing first names to avoid duplicates
        java.util.Set<String> usedFirstNames = new java.util.HashSet<>();
        for (Student s : existingStudents) {
            if (s.getName() != null) {
                String[] nameParts = s.getName().split("\\s+");
                if (nameParts.length > 0) {
                    usedFirstNames.add(nameParts[0].toLowerCase());
                }
            }
        }
        
        // Filipino first names - expanded list to ensure all students have unique first names
        String[] firstNames = {
            "Maria", "Juan", "Jose", "Ana", "Carlos", "Rosa", "Pedro", "Carmen", "Miguel", "Elena",
            "Antonio", "Francisco", "Teresa", "Manuel", "Isabel", "Ricardo", "Dolores", "Fernando", "Patricia",
            "Roberto", "Gloria", "Alberto", "Mercedes", "Eduardo", "Concepcion", "Ramon", "Esperanza", "Alfredo", "Rosario", "Cristina",
            "Josefina", "Felipe", "Margarita", "Rafael", "Enrique", "Consuelo", "Jorge", "Amparo", "Sergio", "Lourdes",
            "Vicente", "Angelica", "Benjamin", "Cecilia", "Daniel", "Diana", "Emilio", "Felicia", "Gabriel", "Hannah",
            "Ignacio", "Irene", "Julian", "Katherine", "Leonardo", "Lydia", "Marcelo", "Natalia", "Oscar", "Paula",
            "Quentin", "Rebecca", "Sebastian", "Sofia", "Tomas", "Valentina", "William", "Yvette", "Zachary", "Andrea"
        };
        
        // Filter out already used first names and shuffle
        java.util.List<String> availableFirstNames = new java.util.ArrayList<>();
        for (String name : firstNames) {
            if (!usedFirstNames.contains(name.toLowerCase())) {
                availableFirstNames.add(name);
            }
        }
        java.util.Collections.shuffle(availableFirstNames, random);
        int firstNameIndex = 0;
        
        // Filipino last names
        String[] lastNames = {
            "Santos", "Reyes", "Cruz", "Bautista", "Villanueva", "Fernandez", "Ramos", "Torres", "Garcia", "Lopez",
            "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez", "Rivera", "Morales", "Ortiz", "Gutierrez", "Castillo",
            "Dela Cruz", "Mendoza", "Aquino", "Castro", "Romero", "Vargas", "Flores", "Herrera", "Jimenez", "Moreno",
            "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado", "Molina", "Herrera", "Vasquez", "Guerrero"
        };
        
        // Middle names
        String[] middleNames = {
            "Cruz", "Reyes", "Santos", "Garcia", "Lopez", "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez"
        };
        
        // Parent relationships
        String[] relationships = {"Father", "Mother", "Guardian", "Other"};
        
        // Previous schools
        String[] previousSchools = {
            "Kinderscape Elementary School", "San Jose Elementary School", "Rizal Elementary School",
            "Bonifacio Elementary School", "Aguinaldo Elementary School", "Mabini Elementary School"
        };
        
        // Addresses
        String[] addresses = {
            "Barangay Poblacion, Kinderscape", "Barangay San Jose, Kinderscape", "Barangay Rizal, Kinderscape",
            "Barangay Bonifacio, Kinderscape", "Barangay Aguinaldo, Kinderscape", "Barangay Mabini, Kinderscape"
        };
        
        int totalAdded = 0;
        
        // Loop through each strand (ABM and GAS)
        for (String testStrand : testStrands) {
            // Loop through each grade level (11 and 12)
            for (Integer gradeLevel : testGrades) {
                // Use section A for each grade
                String sectionName = testStrand + "-" + gradeLevel + testSectionNames[0];
            
            // Find the section
            java.util.Optional<Section> sectionOpt = sectionRepository.findByNameAndStrandAndGradeLevel(
                sectionName, testStrand, gradeLevel);
            
            if (!sectionOpt.isPresent()) {
                System.err.println("⚠ Warning: Section " + sectionName + " not found. Skipping capacity test for this section.");
                continue;
            }
            
            Section section = sectionOpt.get();
            
            // Find the specific semester (2025-2026 Semester 1 for the grade level)
            java.util.Optional<Semester> semesterOpt = null;
            if (semesterRepository != null) {
                semesterOpt = semesterRepository.findBySchoolYearIdAndGradeLevelAndSemesterNumber(
                    targetSchoolYearEntity.getId(), gradeLevel, targetSemesterNumber);
            }
            
            if (semesterOpt == null || !semesterOpt.isPresent()) {
                System.err.println("⚠ Warning: Semester " + targetSemesterNumber + " for Grade " + gradeLevel + " in " + targetSchoolYear + " not found. Skipping capacity test for " + sectionName);
                continue;
            }
            
            Semester targetSemester = semesterOpt.get();
            
            // Check current count for this specific section and semester (using enhanced query with grade level and strand)
            long initialCount = studentRepository.countBySectionIdAndSemesterIdAndGradeLevelAndStrandAndEnrolled(
                section.getId(), targetSemester.getId(), gradeLevel, testStrand);
            int studentsToAdd = 40 - (int) initialCount;
            
            if (studentsToAdd <= 0) {
                System.out.println("  ✓ Section " + sectionName + " (" + targetSchoolYear + " Semester " + targetSemesterNumber + ") already has " + initialCount + " students (at or above capacity)");
                continue;
            }
            
            System.out.println("  → Adding " + studentsToAdd + " students to " + sectionName + " (" + targetSchoolYear + " Semester " + targetSemesterNumber + ") - currently has " + initialCount + " students");
            
            for (int i = 0; i < studentsToAdd; i++) {
                try {
                    // Check capacity before creating student - use the enhanced count that verifies grade level and strand
                    long currentCountBeforeAdd = studentRepository.countBySectionIdAndSemesterIdAndGradeLevelAndStrandAndEnrolled(
                        section.getId(), targetSemester.getId(), gradeLevel, testStrand);
                    
                    if (section.getCapacity() != null && currentCountBeforeAdd >= section.getCapacity()) {
                        System.err.println("⚠ Warning: Section " + sectionName + " is already at capacity (" + currentCountBeforeAdd + "/" + section.getCapacity() + "). Stopping addition.");
                        break; // Stop adding to this section
                    }
                    
                    Student student = new Student();
                    
                    // Generate name - use unique first names sequentially
                    String firstName;
                    if (firstNameIndex < availableFirstNames.size()) {
                        firstName = availableFirstNames.get(firstNameIndex);
                        firstNameIndex++;
                    } else {
                        // Fallback if we run out of unique names
                        firstName = firstNames[(totalAdded + i) % firstNames.length];
                    }
                    String lastName = lastNames[(totalAdded + i) % lastNames.length];
                    String middleName = middleNames[(totalAdded + i) % middleNames.length];
                    student.setName(firstName + " " + middleName + " " + lastName);
                    
                    // Generate birthdate (age between 16-18)
                    int age = 16 + random.nextInt(3);
                    LocalDate birthdate = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
                    student.setBirthdate(birthdate);
                    student.setAge(age);
                    
                    // Sex
                    student.setSex((totalAdded + i) % 2 == 0 ? "Male" : "Female");
                    
                    // Address
                    student.setAddress(addresses[(totalAdded + i) % addresses.length]);
                    
                    // Contact number (must be unique)
                    String contact;
                    do {
                        contact = "09" + String.format("%09d", 200000000 + random.nextInt(700000000));
                    } while (usedContacts.contains(contact));
                    usedContacts.add(contact);
                    student.setContactNumber(contact);
                    
                    // Parent/Guardian information
                    String parentFirstName = firstNames[(totalAdded + i + 10) % firstNames.length];
                    String parentLastName = lastNames[(totalAdded + i + 5) % lastNames.length];
                    student.setParentGuardianName(parentFirstName + " " + parentLastName);
                    
                    // Parent contact (must be different from student contact)
                    String parentContact;
                    do {
                        parentContact = "09" + String.format("%09d", 200000000 + random.nextInt(700000000));
                    } while (usedContacts.contains(parentContact) || parentContact.equals(contact));
                    usedContacts.add(parentContact);
                    student.setParentGuardianContact(parentContact);
                    student.setParentGuardianRelationship(relationships[(totalAdded + i) % relationships.length]);
                    
                    // Grade level
                    student.setGradeLevel(gradeLevel);
                    
                    // Strand
                    student.setStrand(testStrand);
                    
                    // Previous school
                    student.setPreviousSchool(previousSchools[(totalAdded + i) % previousSchools.length]);
                    
                    // GWA (between 75.0 and 100.0)
                    double gwa = 75.0 + (random.nextDouble() * 25.0);
                    student.setGwa(Math.round(gwa * 100.0) / 100.0);
                    
                    // LRN (must be unique)
                    String lrn;
                    do {
                        lrn = String.format("%012d", 200000000000L + random.nextInt(700000000));
                    } while (usedLrns.contains(lrn));
                    usedLrns.add(lrn);
                    student.setLrn(lrn);
                    
                    // Enrollment status - Enrolled
                    student.setEnrollmentStatus("Enrolled");
                    
                    // Section assignment
                    student.setSection(section);
                    
                    // Not archived
                    student.setIsArchived(false);
                    
                    // Assign to target school year (2025-2026)
                    student.setSchoolYear(targetSchoolYearEntity);
                    
                    // Assign target semester (Semester 1)
                    student.setSemester(targetSemester);
                    
                    studentRepository.save(student);
                    totalAdded++;
                } catch (Exception e) {
                    System.err.println("⚠ Error creating capacity test student " + (i + 1) + " for " + sectionName + ": " + e.getMessage());
                }
            }
            
                System.out.println("  ✓ Added " + studentsToAdd + " students to " + sectionName + " (" + targetSchoolYear + " Semester " + targetSemesterNumber + ")");
            } // End of grade level loop
        } // End of strand loop
        
        if (totalAdded > 0) {
            System.out.println("✓ Successfully added " + totalAdded + " capacity test students to " + targetSchoolYear + " Semester " + targetSemesterNumber);
        } else {
            System.out.println("✓ Capacity test sections already have sufficient students for " + targetSchoolYear + " Semester " + targetSemesterNumber);
        }
    }
    
    private void createComprehensiveStudentDataset() {
        System.out.println("\n✓ Creating comprehensive student dataset for current school year...");
        
        // Get current school year
        SchoolYear currentSchoolYear = null;
        if (schoolYearRepository != null) {
            currentSchoolYear = schoolYearRepository.findByIsCurrentTrue().orElse(null);
            if (currentSchoolYear == null) {
                System.err.println("⚠ Warning: No current school year found. Cannot create students.");
                return;
            }
        } else {
            System.err.println("⚠ Warning: SchoolYearRepository not available. Cannot create students.");
            return;
        }
        
        // Get all active sections
        java.util.List<Section> allSections = sectionRepository.findByIsActiveTrue();
        if (allSections.isEmpty()) {
            System.err.println("⚠ Warning: No active sections found. Cannot create students.");
            return;
        }
        
        Random random = new Random();
        
        // Comprehensive name pools (expanded for 660+ students)
        String[] firstNames = {
            "Maria", "Juan", "Jose", "Ana", "Carlos", "Rosa", "Pedro", "Carmen", "Miguel", "Elena",
            "Antonio", "Francisco", "Teresa", "Manuel", "Isabel", "Ricardo", "Dolores", "Fernando", "Patricia", "Roberto",
            "Gloria", "Alberto", "Mercedes", "Eduardo", "Concepcion", "Ramon", "Esperanza", "Alfredo", "Rosario", "Cristina",
            "Josefina", "Felipe", "Margarita", "Rafael", "Enrique", "Consuelo", "Jorge", "Amparo", "Sergio", "Lourdes",
            "Vicente", "Angelica", "Benjamin", "Cecilia", "Daniel", "Diana", "Emilio", "Felicia", "Gabriel", "Hannah",
            "Ignacio", "Irene", "Julian", "Katherine", "Leonardo", "Lydia", "Marcelo", "Natalia", "Oscar", "Paula",
            "Quentin", "Rebecca", "Sebastian", "Sofia", "Tomas", "Valentina", "William", "Yvette", "Zachary", "Andrea",
            "Adrian", "Bianca", "Christian", "Denise", "Ethan", "Fatima", "Gian", "Hazel", "Ivan", "Jasmine",
            "Kyle", "Lara", "Marcus", "Nicole", "Oliver", "Princess", "Quinn", "Raven", "Stephanie", "Tristan",
            "Alexis", "Brandon", "Chloe", "Dominic", "Evelyn", "Felix", "Grace", "Henry", "Isabella", "Jacob",
            "Kaitlyn", "Liam", "Madison", "Nathan", "Olivia", "Parker", "Quinn", "Rachel", "Samuel", "Taylor",
            "Uma", "Victor", "Wendy", "Xavier", "Yara", "Zoe", "Aaron", "Bella", "Caleb", "Daisy",
            "Ethan", "Faith", "Gavin", "Hope", "Isaac", "Joy", "Kevin", "Lily", "Mason", "Nora",
            "Owen", "Penelope", "Quinn", "Ruby", "Simon", "Tessa", "Ulysses", "Violet", "Wesley", "Xara",
            "Yuki", "Zane", "Aria", "Blake", "Clara", "Derek", "Emma", "Finn", "Gina", "Hugo",
            "Ivy", "Jake", "Kara", "Luke", "Maya", "Noah", "Opal", "Paul", "Quincy", "Rosa",
            "Seth", "Tara", "Uriah", "Vera", "Wade", "Ximena", "Yara", "Zara", "Aiden", "Brooke",
            "Cameron", "Diana", "Evan", "Fiona", "Grayson", "Harper", "Ian", "Julia", "Kai", "Leah"
        };
        
        String[] middleNames = {
            "Cruz", "Reyes", "Santos", "Garcia", "Lopez", "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez",
            "Bautista", "Fernandez", "Ramos", "Torres", "Villanueva", "Mendoza", "Aquino", "Castro", "Romero", "Vargas",
            "Flores", "Herrera", "Jimenez", "Moreno", "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado",
            "Molina", "Vasquez", "Guerrero", "Pineda", "Alvarez", "Cortez", "Domingo", "Espinosa", "Fuentes", "Guzman",
            "Hernandez", "Ibarra", "Javier", "Kalaw", "Luna", "Maceda", "Nunez", "Ocampo", "Pascual", "Quizon",
            "Salazar", "Tolentino", "Uy", "Valdez", "Wong", "Yap", "Zamora", "Abad", "Bautista", "Cruz",
            "Dela Cruz", "Espiritu", "Fernandez", "Garcia", "Herrera", "Ibarra", "Javier", "Kalaw", "Luna", "Maceda"
        };
        
        String[] lastNames = {
            "Santos", "Reyes", "Cruz", "Bautista", "Villanueva", "Fernandez", "Ramos", "Torres", "Garcia", "Lopez",
            "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez", "Rivera", "Morales", "Ortiz", "Gutierrez", "Castillo",
            "Dela Cruz", "Mendoza", "Aquino", "Castro", "Romero", "Vargas", "Flores", "Herrera", "Jimenez", "Moreno",
            "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado", "Molina", "Vasquez", "Guerrero", "Pineda",
            "Alvarez", "Cortez", "Domingo", "Espinosa", "Fuentes", "Guzman", "Hernandez", "Ibarra", "Javier", "Kalaw",
            "Luna", "Maceda", "Nunez", "Ocampo", "Pascual", "Quizon", "Salazar", "Tolentino", "Uy", "Valdez",
            "Wong", "Yap", "Zamora", "Abad", "Bautista", "Cruz", "Dela Cruz", "Espiritu", "Fernandez", "Garcia",
            "Herrera", "Ibarra", "Javier", "Kalaw", "Luna", "Maceda", "Nunez", "Ocampo", "Pascual", "Quizon",
            "Salazar", "Tolentino", "Uy", "Valdez", "Wong", "Yap", "Zamora", "Abad", "Bautista", "Cruz"
        };
        
        // Parent relationships
        String[] relationships = {"Father", "Mother", "Guardian", "Other"};
        
        // Previous schools
        String[] previousSchools = {
            "Seguinon Elementary School", "San Jose Elementary School", "Rizal Elementary School",
            "Bonifacio Elementary School", "Aguinaldo Elementary School", "Mabini Elementary School",
            "Quezon Elementary School", "Osmena Elementary School", "Roxas Elementary School", 
            "Magsaysay Elementary School", "Lapu-Lapu Elementary School", "Luna Elementary School"
        };
        
        // Addresses
        String[] addresses = {
            "Barangay Poblacion, Seguinon", "Barangay San Jose, Seguinon", "Barangay Rizal, Seguinon",
            "Barangay Bonifacio, Seguinon", "Barangay Aguinaldo, Seguinon", "Barangay Mabini, Seguinon",
            "Barangay Quezon, Seguinon", "Barangay Osmena, Seguinon", "Barangay Roxas, Seguinon", 
            "Barangay Magsaysay, Seguinon"
        };
        
        // Track used names and contacts
        java.util.Set<String> usedFullNames = new java.util.HashSet<>();
        java.util.Set<String> usedContacts = new java.util.HashSet<>();
        java.util.Set<String> usedLrns = new java.util.HashSet<>();
        
        // Shuffle name components for variety
        java.util.List<String> shuffledFirstNames = new java.util.ArrayList<>(java.util.Arrays.asList(firstNames));
        java.util.List<String> shuffledLastNames = new java.util.ArrayList<>(java.util.Arrays.asList(lastNames));
        java.util.List<String> shuffledMiddleNames = new java.util.ArrayList<>(java.util.Arrays.asList(middleNames));
        java.util.Collections.shuffle(shuffledFirstNames, random);
        java.util.Collections.shuffle(shuffledLastNames, random);
        java.util.Collections.shuffle(shuffledMiddleNames, random);
        
        int totalStudentsCreated = 0;
        int nameIndex = 0;
        
        // Process each section
        for (Section section : allSections) {
            String strand = section.getStrand();
            Integer gradeLevel = section.getGradeLevel();
            String sectionName = section.getName();
            
            // Determine how many students to add
            int studentsNeeded;
            if ("ABM".equals(strand) || "GAS".equals(strand)) {
                // ABM and GAS: 40 students per section
                studentsNeeded = 40;
            } else {
                // Other strands: 10 students per section
                studentsNeeded = 10;
            }
            
            // Get semester for this grade level first (needed for count)
            java.util.Optional<Semester> semesterOpt = null;
            if (semesterRepository != null) {
                semesterOpt = semesterRepository.findBySchoolYearIdAndGradeLevelAndSemesterNumber(
                    currentSchoolYear.getId(), gradeLevel, 1);
            }
            
            if (semesterOpt == null || !semesterOpt.isPresent()) {
                System.err.println("    ⚠ Warning: Semester not found for " + sectionName + ". Skipping this section.");
                continue;
            }
            
            Semester semester = semesterOpt.get();
            
            // Check current count in this section
            long currentCount = studentRepository.countBySectionIdAndSemesterIdAndGradeLevelAndStrandAndEnrolled(
                section.getId(), semester.getId(), gradeLevel, strand);
            
            int studentsToAdd = studentsNeeded - (int) currentCount;
            
            if (studentsToAdd <= 0) {
                System.out.println("  ✓ Section " + sectionName + " already has " + currentCount + " students (target: " + studentsNeeded + ")");
                continue;
            }
            
            System.out.println("  → Adding " + studentsToAdd + " students to " + sectionName + " (target: " + studentsNeeded + ")");
            
            // Create students for this section
            for (int i = 0; i < studentsToAdd; i++) {
                try {
                    // Check capacity
                    long currentCountBeforeAdd = studentRepository.countBySectionIdAndSemesterIdAndGradeLevelAndStrandAndEnrolled(
                        section.getId(), semester.getId(), gradeLevel, strand);
                    
                    if (section.getCapacity() != null && currentCountBeforeAdd >= section.getCapacity()) {
                        System.err.println("    ⚠ Warning: Section " + sectionName + " is at capacity. Stopping.");
                        break;
                    }
                    
                    Student student = new Student();
                    
                    // Generate unique full name
                    String firstName, middleName, lastName, fullName;
                    int attempts = 0;
                    do {
                        firstName = shuffledFirstNames.get(nameIndex % shuffledFirstNames.size());
                        middleName = shuffledMiddleNames.get((nameIndex + totalStudentsCreated) % shuffledMiddleNames.size());
                        lastName = shuffledLastNames.get((nameIndex + totalStudentsCreated * 2) % shuffledLastNames.size());
                        fullName = firstName + " " + middleName + " " + lastName;
                        nameIndex++;
                        attempts++;
                        
                        if (attempts > 1000) {
                            fullName = firstName + " " + middleName + " " + lastName + " " + (totalStudentsCreated + 1);
                            break;
                        }
                    } while (usedFullNames.contains(fullName.toLowerCase()));
                    
                    usedFullNames.add(fullName.toLowerCase());
                    student.setName(fullName);
                    
                    // Generate birthdate (age 16-18)
                    int age = 16 + random.nextInt(3);
                    LocalDate birthdate = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
                    student.setBirthdate(birthdate);
                    student.setAge(age);
                    
                    // Sex
                    student.setSex(totalStudentsCreated % 2 == 0 ? "Male" : "Female");
                    
                    // Address
                    student.setAddress(addresses[totalStudentsCreated % addresses.length]);
                    
                    // Contact number (unique)
                    String contact;
                    do {
                        contact = "09" + String.format("%09d", 100000000 + random.nextInt(900000000));
                    } while (usedContacts.contains(contact));
                    usedContacts.add(contact);
                    student.setContactNumber(contact);
                    
                    // Parent/Guardian information
                    String parentFirstName = shuffledFirstNames.get((nameIndex + 10) % shuffledFirstNames.size());
                    String parentLastName = shuffledLastNames.get((nameIndex + 5) % shuffledLastNames.size());
                    student.setParentGuardianName(parentFirstName + " " + parentLastName);
                    
                    // Parent contact (unique and different from student)
                    String parentContact;
                    do {
                        parentContact = "09" + String.format("%09d", 100000000 + random.nextInt(900000000));
                    } while (usedContacts.contains(parentContact) || parentContact.equals(contact));
                    usedContacts.add(parentContact);
                    student.setParentGuardianContact(parentContact);
                    student.setParentGuardianRelationship(relationships[totalStudentsCreated % relationships.length]);
                    
                    // Grade level
                    student.setGradeLevel(gradeLevel);
                    
                    // Strand
                    student.setStrand(strand);
                    
                    // Previous school
                    student.setPreviousSchool(previousSchools[totalStudentsCreated % previousSchools.length]);
                    
                    // GWA (75.0 - 100.0)
                    double gwa = 75.0 + (random.nextDouble() * 25.0);
                    student.setGwa(Math.round(gwa * 100.0) / 100.0);
                    
                    // LRN (unique, 12 digits)
                    String lrn;
                    do {
                        lrn = String.format("%012d", 100000000000L + random.nextInt(900000000));
                    } while (usedLrns.contains(lrn));
                    usedLrns.add(lrn);
                    student.setLrn(lrn);
                    
                    // Enrollment status
                    student.setEnrollmentStatus("Enrolled");
                    
                    // Section assignment
                    student.setSection(section);
                    
                    // Not archived
                    student.setIsArchived(false);
                    
                    // School year
                    student.setSchoolYear(currentSchoolYear);
                    
                    // Semester
                    student.setSemester(semester);
                    
                    studentRepository.save(student);
                    totalStudentsCreated++;
                    
                } catch (Exception e) {
                    System.err.println("    ⚠ Error creating student for " + sectionName + ": " + e.getMessage());
                }
            }
            
            System.out.println("    ✓ Added " + studentsToAdd + " students to " + sectionName);
        }
        
        System.out.println("\n✓ Successfully created " + totalStudentsCreated + " students across all sections");
        System.out.println("  - All students are enrolled in the current school year");
        System.out.println("  - All students have unique full names");
        System.out.println("  - ABM and GAS sections: 40 students each");
        System.out.println("  - Other strand sections: 10 students each");
    }
    
    private void initializeSubjects() {
        if (subjectRepository == null) {
            System.out.println("⚠ SubjectRepository not available - skipping subject initialization");
            return;
        }
        
        // Build comprehensive list of valid subjects
        java.util.Set<String> validSubjects = new java.util.HashSet<>();
        
        // GRADE 11 CORE SUBJECTS (All Strands)
        String[] grade11CoreSubjects = {
            "Oral Communication",
            "Komunikasyon at Pananaliksik sa Wika at Kulturang Filipino",
            "General Mathematics",
            "Earth and Life Science",
            "21st Century Literature from the Philippines and the World",
            "Physical Education & Health 1",
            "Physical Education & Health 2"
        };
        
        // GRADE 12 CORE SUBJECTS (All Strands)
        String[] grade12CoreSubjects = {
            "Reading and Writing",
            "Pagbasa at Pagsusuri ng Iba't Ibang Teksto Tungo sa Pananaliksik",
            "Statistics and Probability",
            "Physical Science",
            "Understanding Culture, Society and Politics (UCSP)",
            "Contemporary Philippine Arts from the Regions (CPAR)",
            "Introduction to the Philosophy of the Human Person (IPHP)",
            "Media and Information Literacy (MIL)",
            "Physical Education & Health 3",
            "Physical Education & Health 4"
        };
        
        // STEM STRAND - Grade 11 Major Subjects
        String[] stem11Subjects = {
            "Pre-Calculus",
            "Basic Calculus",
            "General Biology 1",
            "General Chemistry 1",
            "General Physics 1"
        };
        
        // STEM STRAND - Grade 12 Major Subjects
        String[] stem12Subjects = {
            "General Biology 2",
            "General Chemistry 2",
            "General Physics 2",
            "Disaster Readiness and Risk Reduction (DRRR)",
            "Research / Inquiry, Investigation and Immersion"
        };
        
        // ABM STRAND - Grade 11 Major Subjects
        String[] abm11Subjects = {
            "Applied Economics",
            "Business Math",
            "Organization and Management",
            "Fundamentals of Accountancy, Business & Management 1 (FABM 1)"
        };
        
        // ABM STRAND - Grade 12 Major Subjects
        String[] abm12Subjects = {
            "Fundamentals of Accountancy, Business & Management 2 (FABM 2)",
            "Business Finance",
            "Principles of Marketing",
            "Business Ethics and Social Responsibility",
            "Work Immersion / Business Research"
        };
        
        // HUMSS STRAND - Grade 11 Major Subjects
        String[] humss11Subjects = {
            "Creative Writing",
            "Creative Nonfiction",
            "Introduction to World Religions and Belief Systems",
            "Community Engagement, Solidarity and Citizenship"
        };
        
        // HUMSS STRAND - Grade 12 Major Subjects
        String[] humss12Subjects = {
            "Disciplines and Ideas in the Social Sciences (DISS)",
            "Disciplines and Ideas in the Applied Social Sciences (DIASS)",
            "Philippine Politics and Governance",
            "Humanities 1",
            "Humanities 2",
            "Work Immersion / Research"
        };
        
        // GAS STRAND - Grade 11 Major Subjects
        String[] gas11Subjects = {
            "Humanities 1",
            "Social Science 1",
            "Applied Economics",
            "Organization and Management"
        };
        
        // GAS STRAND - Grade 12 Major Subjects
        String[] gas12Subjects = {
            "Humanities 2",
            "Social Science 2",
            "Disaster Readiness and Risk Reduction (DRRR)",
            "Work Immersion / Research"
        };
        
        // TVL-ICT STRAND - Grade 11 Major Subjects
        String[] tvlIct11Subjects = {
            "Computer Systems Servicing (CSS) 1",
            "Computer Systems Servicing (CSS) 2",
            "Introduction to Computing",
            "Computer Programming 1",
            "Empowerment Technologies (E-Tech)"
        };
        
        // TVL-ICT STRAND - Grade 12 Major Subjects
        String[] tvlIct12Subjects = {
            "Computer Programming 2",
            "Web Development / Web Technologies",
            "Animation / Graphic Illustration",
            "Practical Research / Inquiries, Investigations & Immersion",
            "Work Immersion"
        };
        
        // Build set of all valid subjects (grade:name format)
        for (String name : grade11CoreSubjects) {
            validSubjects.add("11:" + name);
        }
        for (String name : grade12CoreSubjects) {
            validSubjects.add("12:" + name);
        }
        for (String name : stem11Subjects) {
            validSubjects.add("11:" + name);
        }
        for (String name : stem12Subjects) {
            validSubjects.add("12:" + name);
        }
        for (String name : abm11Subjects) {
            validSubjects.add("11:" + name);
        }
        for (String name : abm12Subjects) {
            validSubjects.add("12:" + name);
        }
        for (String name : humss11Subjects) {
            validSubjects.add("11:" + name);
        }
        for (String name : humss12Subjects) {
            validSubjects.add("12:" + name);
        }
        for (String name : gas11Subjects) {
            validSubjects.add("11:" + name);
        }
        for (String name : gas12Subjects) {
            validSubjects.add("12:" + name);
        }
        for (String name : tvlIct11Subjects) {
            validSubjects.add("11:" + name);
        }
        for (String name : tvlIct12Subjects) {
            validSubjects.add("12:" + name);
        }
        
        // PERMANENTLY DELETE all subjects that are NOT in the valid list
        System.out.println("🔄 Permanently deleting all old subjects not in the comprehensive list...");
        java.util.List<com.enrollment.system.model.Subject> allExistingSubjects = subjectRepository.findAll();
        int deletedCount = 0;
        java.util.List<com.enrollment.system.model.Subject> toDelete = new java.util.ArrayList<>();
        
        for (com.enrollment.system.model.Subject existingSubject : allExistingSubjects) {
            String subjectKey = existingSubject.getGradeLevel() + ":" + existingSubject.getName();
            if (!validSubjects.contains(subjectKey)) {
                // This subject is NOT in the new list - DELETE IT PERMANENTLY
                toDelete.add(existingSubject);
                deletedCount++;
            }
        }
        
        // Delete all old subjects
        if (!toDelete.isEmpty()) {
            subjectRepository.deleteAll(toDelete);
            System.out.println("✓ Permanently deleted " + deletedCount + " old subjects");
        } else {
            System.out.println("✓ No old subjects to delete");
        }
        
        int subjectsCreated = 0;
        int subjectsUpdated = 0;
        
        // Create/Update Grade 11 Core Subjects
        for (String subjectName : grade11CoreSubjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 11);
            
            if (existingOpt.isPresent()) {
                // Update existing subject
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("CORE");
                subject.setStrand("ALL STRAND"); // Core subjects are for all strands
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                // Create new subject
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(11);
                subject.setSubjectType("CORE");
                subject.setStrand("ALL STRAND"); // Core subjects are for all strands
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update Grade 12 Core Subjects
        for (String subjectName : grade12CoreSubjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 12);
            
            if (existingOpt.isPresent()) {
                // Update existing subject
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("CORE");
                subject.setStrand("ALL STRAND"); // Core subjects are for all strands
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                // Create new subject
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(12);
                subject.setSubjectType("CORE");
                subject.setStrand("ALL STRAND"); // Core subjects are for all strands
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update STEM Grade 11 subjects
        for (String subjectName : stem11Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 11);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("STEM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(11);
                subject.setSubjectType("MAJOR");
                subject.setStrand("STEM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update STEM Grade 12 subjects
        for (String subjectName : stem12Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 12);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("STEM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(12);
                subject.setSubjectType("MAJOR");
                subject.setStrand("STEM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update ABM Grade 11 subjects
        for (String subjectName : abm11Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 11);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("ABM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(11);
                subject.setSubjectType("MAJOR");
                subject.setStrand("ABM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update ABM Grade 12 subjects
        for (String subjectName : abm12Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 12);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("ABM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(12);
                subject.setSubjectType("MAJOR");
                subject.setStrand("ABM");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update HUMSS Grade 11 subjects
        for (String subjectName : humss11Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 11);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("HUMSS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(11);
                subject.setSubjectType("MAJOR");
                subject.setStrand("HUMSS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update HUMSS Grade 12 subjects
        for (String subjectName : humss12Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 12);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("HUMSS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(12);
                subject.setSubjectType("MAJOR");
                subject.setStrand("HUMSS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update GAS Grade 11 subjects
        for (String subjectName : gas11Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 11);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("GAS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(11);
                subject.setSubjectType("MAJOR");
                subject.setStrand("GAS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update GAS Grade 12 subjects
        for (String subjectName : gas12Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 12);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("GAS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(12);
                subject.setSubjectType("MAJOR");
                subject.setStrand("GAS");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update TVL-ICT Grade 11 subjects
        for (String subjectName : tvlIct11Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 11);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("TVL-ICT");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(11);
                subject.setSubjectType("MAJOR");
                subject.setStrand("TVL-ICT");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        // Create/Update TVL-ICT Grade 12 subjects
        for (String subjectName : tvlIct12Subjects) {
            java.util.Optional<com.enrollment.system.model.Subject> existingOpt = 
                subjectRepository.findByNameAndGradeLevel(subjectName, 12);
            
            if (existingOpt.isPresent()) {
                com.enrollment.system.model.Subject subject = existingOpt.get();
                subject.setSubjectType("MAJOR");
                subject.setStrand("TVL-ICT");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsUpdated++;
            } else {
                com.enrollment.system.model.Subject subject = new com.enrollment.system.model.Subject();
                subject.setName(subjectName);
                subject.setGradeLevel(12);
                subject.setSubjectType("MAJOR");
                subject.setStrand("TVL-ICT");
                subject.setIsActive(true);
                subject.setIsCustom(false);
                subjectRepository.save(subject);
                subjectsCreated++;
            }
        }
        
        if (subjectsCreated > 0 || subjectsUpdated > 0) {
            System.out.println("✓ Created " + subjectsCreated + " new subjects");
            System.out.println("✓ Updated " + subjectsUpdated + " existing subjects");
            System.out.println("✓ Database now contains only the comprehensive subject list (CORE and MAJOR)");
        } else {
            System.out.println("✓ All subjects are up to date in database");
        }
        
        // Assign sample subjects to Hazel and Arnel for testing
        try {
            if (teacherSubjectAssigner != null) {
                teacherSubjectAssigner.assignSampleSubjectsToTestTeachers();
            }
        } catch (Exception e) {
            System.err.println("⚠ Warning: Could not assign sample subjects to test teachers: " + e.getMessage());
            // Continue - don't block application startup
        }
    }
}