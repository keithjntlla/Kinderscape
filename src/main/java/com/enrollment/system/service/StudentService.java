package com.enrollment.system.service;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.Section;
import com.enrollment.system.model.SchoolYear;
import com.enrollment.system.model.Student;
import com.enrollment.system.repository.SectionRepository;
import com.enrollment.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private com.enrollment.system.service.SectionService sectionService;
    
    @Autowired(required = false)
    private com.enrollment.system.service.SchoolYearService schoolYearService;
    
    @Autowired(required = false)
    private com.enrollment.system.repository.SchoolYearRepository schoolYearRepository;
    
    @Autowired(required = false)
    private com.enrollment.system.repository.SemesterRepository semesterRepository;
    
    @Transactional
    public StudentDto saveStudent(StudentDto studentDto) {
        // Validate unique name
        String studentName = studentDto.getName() != null ? studentDto.getName().trim() : null;
        if (studentName != null && !studentName.isEmpty()) {
            if (studentRepository.existsByNameIgnoreCase(studentName)) {
                throw new RuntimeException("A student with the name \"" + studentName + "\" already exists. Student names must be unique.");
            }
            
            // Validate: Student name cannot match parent name without suffix
            String parentName = studentDto.getParentGuardianName() != null ? studentDto.getParentGuardianName().trim() : null;
            if (parentName != null && !parentName.isEmpty()) {
                if (namesMatchWithoutSuffix(studentName, parentName)) {
                    throw new RuntimeException("Student name cannot be the same as parent/guardian name. If they share the same name, the student must have a suffix (e.g., Jr., Sr., II, III, IV). Example: \"Arnel Caparoso Jr.\"");
                }
            }
        }
        
        Student student = new Student();
        student.setName(studentName);
        student.setBirthdate(studentDto.getBirthdate());
        
        // Calculate age from birthdate if provided
        if (studentDto.getBirthdate() != null) {
            student.setAge(calculateAge(studentDto.getBirthdate()));
        } else {
            student.setAge(studentDto.getAge());
        }
        
        student.setSex(studentDto.getSex());
        student.setAddress(studentDto.getAddress());
        student.setContactNumber(studentDto.getContactNumber());
        student.setParentGuardianName(studentDto.getParentGuardianName());
        student.setParentGuardianContact(studentDto.getParentGuardianContact());
        student.setParentGuardianRelationship(studentDto.getParentGuardianRelationship());
        student.setGradeLevel(studentDto.getGradeLevel());
        student.setStrand(studentDto.getStrand());
        student.setPreviousSchool(studentDto.getPreviousSchool());
        student.setGwa(studentDto.getGwa());
        student.setLrn(studentDto.getLrn());
        student.setEnrollmentStatus(studentDto.getEnrollmentStatus());
        student.setReEnrollmentReason(studentDto.getReEnrollmentReason());
        
        // Ensure new students are not archived
        student.setIsArchived(false);
        
        // Assign school year - use current school year if not provided
        SchoolYear assignedSchoolYear = null;
        if (studentDto.getSchoolYearId() != null && schoolYearRepository != null) {
            // School year explicitly provided
            SchoolYear schoolYear = schoolYearRepository.findById(studentDto.getSchoolYearId())
                    .orElseThrow(() -> new RuntimeException("School year not found with id: " + studentDto.getSchoolYearId()));
            student.setSchoolYear(schoolYear);
            assignedSchoolYear = schoolYear;
        } else if (schoolYearService != null) {
            // Try to assign current school year
            try {
                SchoolYear currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
                student.setSchoolYear(currentSchoolYear);
                assignedSchoolYear = currentSchoolYear;
            } catch (RuntimeException e) {
                // No current school year set - this is okay, student will have null school year
                // This maintains backward compatibility
            }
        }
        
        // Assign semester if provided (needed for capacity check)
        com.enrollment.system.model.Semester assignedSemester = null;
        if (studentDto.getSemesterId() != null && semesterRepository != null) {
            com.enrollment.system.model.Semester semester = semesterRepository.findById(studentDto.getSemesterId())
                    .orElseThrow(() -> new RuntimeException("Semester not found with id: " + studentDto.getSemesterId()));
            
            // Validate that semester belongs to the assigned school year
            if (assignedSchoolYear != null && semester.getSchoolYear() != null) {
                if (!semester.getSchoolYear().getId().equals(assignedSchoolYear.getId())) {
                    throw new RuntimeException("Semester does not belong to the selected school year.");
                }
            }
            
            student.setSemester(semester);
            assignedSemester = semester;
        } else {
            // Semester is optional - set to null for backward compatibility
            student.setSemester(null);
        }
        
        // Handle section assignment with validation (after semester is assigned for capacity check)
        if (studentDto.getSectionId() != null) {
            // Validate: Only enrolled students can be assigned to a section
            if (!"Enrolled".equals(studentDto.getEnrollmentStatus())) {
                throw new RuntimeException("Sections can only be assigned to enrolled students. Please change the enrollment status to 'Enrolled' or remove the section assignment.");
            }
            Section section = sectionRepository.findById(studentDto.getSectionId())
                    .orElseThrow(() -> new RuntimeException("Section not found with id: " + studentDto.getSectionId()));
            
            // Validate that student's grade level and strand match the section
            if (studentDto.getGradeLevel() != null && section.getGradeLevel() != null && 
                !studentDto.getGradeLevel().equals(section.getGradeLevel())) {
                throw new RuntimeException("Student grade level (" + studentDto.getGradeLevel() + 
                    ") does not match section grade level (" + section.getGradeLevel() + ").");
            }
            if (studentDto.getStrand() != null && section.getStrand() != null && 
                !studentDto.getStrand().equals(section.getStrand())) {
                throw new RuntimeException("Student strand (" + studentDto.getStrand() + 
                    ") does not match section strand (" + section.getStrand() + ").");
            }
            
            // Check section capacity (with semester if available)
            // IMPORTANT: Semester must be provided for enrolled students to check capacity correctly
            Long semesterId = assignedSemester != null ? assignedSemester.getId() : null;
            
            // For enrolled students, semester is required for proper capacity checking
            if ("Enrolled".equals(studentDto.getEnrollmentStatus()) && semesterId == null) {
                throw new RuntimeException("Semester is required for enrolled students to check section capacity. Please select a School Year & Semester.");
            }
            
            // Perform capacity check BEFORE assigning section
            // This must happen before student.setSection() to prevent saving if capacity is full
            boolean hasCapacity = sectionService.hasAvailableCapacity(section.getId(), semesterId);
            if (!hasCapacity) {
                // Return error message as specified in requirements
                throw new RuntimeException("This section is full. Maximum capacity reached.");
            }
            
            student.setSection(section);
        } else {
            // Validate: Enrolled students must have a section
            if ("Enrolled".equals(studentDto.getEnrollmentStatus())) {
                throw new RuntimeException("Enrolled students must be assigned to a section.");
            }
            student.setSection(null);
        }
        
        Student savedStudent = studentRepository.save(student);
        return StudentDto.fromStudent(savedStudent);
    }
    
    @Transactional(readOnly = true)
    public List<StudentDto> getAllStudents() {
        // Filter by current school year if available, otherwise return all non-archived
        SchoolYear currentSchoolYear = null;
        if (schoolYearService != null && schoolYearRepository != null) {
            try {
                currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
            } catch (RuntimeException e) {
                // No current school year - show all students (backward compatibility)
                // This is expected during initial setup
            } catch (Exception e) {
                // Any other error - log but don't fail
                System.err.println("Warning: Could not get current school year: " + e.getMessage());
            }
        }
        
        final SchoolYear finalCurrentSchoolYear = currentSchoolYear;
        
        try {
            return studentRepository.findAllWithSectionByOrderByNameAsc()
                    .stream()
                    .filter(student -> {
                        Boolean archived = student.getIsArchived();
                        if (archived != null && archived) {
                            return false;
                        }
                        // If current school year is set, only show students from current year
                        // Otherwise show all (backward compatibility)
                        if (finalCurrentSchoolYear != null) {
                            try {
                                return student.getSchoolYear() != null && 
                                       student.getSchoolYear().getId().equals(finalCurrentSchoolYear.getId());
                            } catch (Exception e) {
                                // If there's an error accessing school year, include the student
                                return true;
                            }
                        }
                        return true;
                    })
                    .map(StudentDto::fromStudent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // If there's any error, return empty list rather than crashing
            System.err.println("Error loading students: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    @Transactional(readOnly = true)
    public List<StudentDto> getArchivedStudents() {
        return studentRepository.findAllWithSectionByOrderByNameAsc()
                .stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    return archived != null && archived;
                })
                .map(StudentDto::fromStudent)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id) {
        return studentRepository.findById(id)
                .map(student -> {
                    // Force initialization of lazy-loaded section within transaction
                    if (student.getSection() != null) {
                        student.getSection().getName(); // Trigger lazy load
                    }
                    return StudentDto.fromStudent(student);
                })
                .orElse(null);
    }
    
    @Transactional
    public StudentDto updateStudent(Long id, StudentDto studentDto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        // Validate unique name if name is being changed
        String newName = studentDto.getName() != null ? studentDto.getName().trim() : null;
        String currentName = student.getName();
        
        if (newName != null && !newName.isEmpty()) {
            // Check if name is being changed
            if (!newName.equalsIgnoreCase(currentName != null ? currentName.trim() : "")) {
                // Check if another student already has this name
                if (studentRepository.existsByNameIgnoreCaseExcludingId(newName, id)) {
                    throw new RuntimeException("A student with the name \"" + newName + "\" already exists. Student names must be unique.");
                }
            }
            
            // Validate: Student name cannot match parent name without suffix
            String parentName = studentDto.getParentGuardianName() != null ? studentDto.getParentGuardianName().trim() : null;
            if (parentName == null || parentName.isEmpty()) {
                // If parent name is not provided in update, check existing parent name
                parentName = student.getParentGuardianName() != null ? student.getParentGuardianName().trim() : null;
            }
            if (parentName != null && !parentName.isEmpty()) {
                if (namesMatchWithoutSuffix(newName, parentName)) {
                    throw new RuntimeException("Student name cannot be the same as parent/guardian name. If they share the same name, the student must have a suffix (e.g., Jr., Sr., II, III, IV). Example: \"Arnel Caparoso Jr.\"");
                }
            }
            
            student.setName(newName);
        } else if (newName == null || newName.isEmpty()) {
            throw new RuntimeException("Student name cannot be empty.");
        }
        student.setBirthdate(studentDto.getBirthdate());
        
        // Calculate age from birthdate if provided
        if (studentDto.getBirthdate() != null) {
            student.setAge(calculateAge(studentDto.getBirthdate()));
        } else {
            student.setAge(studentDto.getAge());
        }
        
        student.setSex(studentDto.getSex());
        student.setAddress(studentDto.getAddress());
        student.setContactNumber(studentDto.getContactNumber());
        student.setParentGuardianName(studentDto.getParentGuardianName());
        student.setParentGuardianContact(studentDto.getParentGuardianContact());
        student.setParentGuardianRelationship(studentDto.getParentGuardianRelationship());
        student.setGradeLevel(studentDto.getGradeLevel());
        student.setStrand(studentDto.getStrand());
        student.setPreviousSchool(studentDto.getPreviousSchool());
        student.setGwa(studentDto.getGwa());
        student.setEnrollmentStatus(studentDto.getEnrollmentStatus());
        student.setReEnrollmentReason(studentDto.getReEnrollmentReason());
        
        // Update school year if provided
        SchoolYear updatedSchoolYear = null;
        if (studentDto.getSchoolYearId() != null && schoolYearRepository != null) {
            SchoolYear schoolYear = schoolYearRepository.findById(studentDto.getSchoolYearId())
                    .orElseThrow(() -> new RuntimeException("School year not found with id: " + studentDto.getSchoolYearId()));
            student.setSchoolYear(schoolYear);
            updatedSchoolYear = schoolYear;
        }
        
        // Update semester if provided (needed for capacity check)
        com.enrollment.system.model.Semester updatedSemester = null;
        if (studentDto.getSemesterId() != null && semesterRepository != null) {
            com.enrollment.system.model.Semester semester = semesterRepository.findById(studentDto.getSemesterId())
                    .orElseThrow(() -> new RuntimeException("Semester not found with id: " + studentDto.getSemesterId()));
            
            // Validate that semester belongs to the school year
            if (updatedSchoolYear != null && semester.getSchoolYear() != null) {
                if (!semester.getSchoolYear().getId().equals(updatedSchoolYear.getId())) {
                    throw new RuntimeException("Semester does not belong to the selected school year.");
                }
            } else if (student.getSchoolYear() != null && semester.getSchoolYear() != null) {
                // Use existing school year if no update provided
                if (!semester.getSchoolYear().getId().equals(student.getSchoolYear().getId())) {
                    throw new RuntimeException("Semester does not belong to the student's school year.");
                }
            }
            
            student.setSemester(semester);
            updatedSemester = semester;
        } else if (studentDto.getSemesterId() == null) {
            // Explicitly set to null if semesterId is null in DTO
            student.setSemester(null);
        }
        // If semesterId is not provided in DTO, keep existing semester (backward compatible)
        if (updatedSemester == null && student.getSemester() != null) {
            updatedSemester = student.getSemester();
        }
        
        // Handle section assignment with validation (after semester is updated for capacity check)
        if (studentDto.getSectionId() != null) {
            // Validate: Only enrolled students can be assigned to a section
            if (!"Enrolled".equals(studentDto.getEnrollmentStatus())) {
                throw new RuntimeException("Sections can only be assigned to enrolled students. Please change the enrollment status to 'Enrolled' or remove the section assignment.");
            }
            Section section = sectionRepository.findById(studentDto.getSectionId())
                    .orElseThrow(() -> new RuntimeException("Section not found with id: " + studentDto.getSectionId()));
            
            // Validate that student's grade level and strand match the section
            if (studentDto.getGradeLevel() != null && section.getGradeLevel() != null && 
                !studentDto.getGradeLevel().equals(section.getGradeLevel())) {
                throw new RuntimeException("Student grade level (" + studentDto.getGradeLevel() + 
                    ") does not match section grade level (" + section.getGradeLevel() + ").");
            }
            if (studentDto.getStrand() != null && section.getStrand() != null && 
                !studentDto.getStrand().equals(section.getStrand())) {
                throw new RuntimeException("Student strand (" + studentDto.getStrand() + 
                    ") does not match section strand (" + section.getStrand() + ").");
            }
            
            // Check section capacity - must check if:
            // 1. Section is being changed to a different section, OR
            // 2. Enrollment status is changing to "Enrolled" (student might have been "Pending" before)
            // Note: If student is already enrolled in the same section, they're already counted, so no need to check
            Section currentSection = student.getSection();
            boolean isSectionChanging = currentSection == null || !currentSection.getId().equals(section.getId());
            String currentEnrollmentStatus = student.getEnrollmentStatus();
            boolean isEnrollmentStatusChanging = !"Enrolled".equals(currentEnrollmentStatus) && "Enrolled".equals(studentDto.getEnrollmentStatus());
            
            Long semesterId = updatedSemester != null ? updatedSemester.getId() : null;
            
            // Check capacity if:
            // - Section is changing (moving to different section), OR
            // - Enrollment status is changing to "Enrolled" (wasn't counted before, now will be counted)
            // We need to check BEFORE the student is saved to prevent exceeding capacity
            if ((isSectionChanging || isEnrollmentStatusChanging)) {
                if (!sectionService.hasAvailableCapacity(section.getId(), semesterId)) {
                    // Return error message as specified in requirements
                    throw new RuntimeException("This section is full. Maximum capacity reached.");
                }
            }
            
            student.setSection(section);
        } else {
            // Validate: Enrolled students must have a section
            if ("Enrolled".equals(studentDto.getEnrollmentStatus())) {
                throw new RuntimeException("Enrolled students must be assigned to a section.");
            }
            student.setSection(null);
        }
        
        // Handle LRN update - check for uniqueness if LRN is being changed
        String newLrn = studentDto.getLrn() != null ? studentDto.getLrn().trim() : null;
        String currentLrn = student.getLrn();
        
        // Only update LRN if it's different and not empty
        if (newLrn != null && !newLrn.isEmpty()) {
            // Check if LRN is being changed
            if (!newLrn.equals(currentLrn)) {
                // Check if another student already has this LRN
                if (studentRepository.existsByLrn(newLrn)) {
                    // Check if it's the same student (shouldn't happen, but just in case)
                    java.util.Optional<Student> existingStudent = studentRepository.findByLrn(newLrn);
                    if (existingStudent.isPresent() && !existingStudent.get().getId().equals(id)) {
                        throw new RuntimeException("LRN " + newLrn + " already exists for another student.");
                    }
                }
                student.setLrn(newLrn);
            }
            // If LRN is the same, no need to update
        } else {
            // If new LRN is empty/null, clear it
            student.setLrn(null);
        }
        
        // Unarchive student if they are being re-enrolled (from archive)
        // This happens when a student is being re-enrolled from the archive list
        // If the student was archived and is being updated to "Enrolled" status, unarchive them
        // BUT: Do not allow graduated students to be re-enrolled
        Boolean wasArchived = student.getIsArchived();
        if (wasArchived != null && wasArchived && "Enrolled".equals(studentDto.getEnrollmentStatus())) {
            // Check if student is graduated - prevent re-enrollment
            String archiveReason = student.getArchiveReason();
            if (archiveReason != null && "GRADUATED".equalsIgnoreCase(archiveReason.trim())) {
                throw new RuntimeException("Graduated students cannot be re-enrolled. Student \"" + student.getName() + "\" has already graduated.");
            }
            // Unarchive the student - they're being re-enrolled and moved back to active students
            student.setIsArchived(false);
            student.setArchiveReason(null);
            student.setArchivedAt(null);
        }
        
        Student updatedStudent = studentRepository.save(student);
        return StudentDto.fromStudent(updatedStudent);
    }
    
    /**
     * Restricted update method for teachers - only allows updating:
     * - Name
     * - Contact Number
     * - Sex
     * - LRN
     * 
     * All other fields (Grade Level, Strand, Section) remain unchanged.
     */
    @Transactional
    public StudentDto updateStudentForTeacher(Long id, String name, String contactNumber, String sex, String lrn) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        // Validate and update name
        String newName = name != null ? name.trim() : null;
        String currentName = student.getName();
        
        if (newName != null && !newName.isEmpty()) {
            // Check if name is being changed
            if (!newName.equalsIgnoreCase(currentName != null ? currentName.trim() : "")) {
                // Check if another student already has this name
                if (studentRepository.existsByNameIgnoreCaseExcludingId(newName, id)) {
                    throw new RuntimeException("A student with the name \"" + newName + "\" already exists. Student names must be unique.");
                }
            }
            
            // Validate: Student name cannot match parent name without suffix
            String parentName = student.getParentGuardianName() != null ? student.getParentGuardianName().trim() : null;
            if (parentName != null && !parentName.isEmpty()) {
                if (namesMatchWithoutSuffix(newName, parentName)) {
                    throw new RuntimeException("Student name cannot be the same as parent/guardian name. If they share the same name, the student must have a suffix (e.g., Jr., Sr., II, III, IV). Example: \"Arnel Caparoso Jr.\"");
                }
            }
            
            student.setName(newName);
        } else {
            throw new RuntimeException("Student name cannot be empty.");
        }
        
        // Update contact number
        student.setContactNumber(contactNumber != null ? contactNumber.trim() : null);
        
        // Update sex
        if (sex != null && !sex.trim().isEmpty()) {
            student.setSex(sex.trim());
        } else {
            throw new RuntimeException("Sex is required.");
        }
        
        // Update LRN - check for uniqueness if LRN is being changed
        String newLrn = lrn != null ? lrn.trim() : null;
        String currentLrn = student.getLrn();
        
        if (newLrn != null && !newLrn.isEmpty()) {
            // Check if LRN is being changed
            if (!newLrn.equals(currentLrn)) {
                // Check if another student already has this LRN
                if (studentRepository.existsByLrn(newLrn)) {
                    java.util.Optional<Student> existingStudent = studentRepository.findByLrn(newLrn);
                    if (existingStudent.isPresent() && !existingStudent.get().getId().equals(id)) {
                        throw new RuntimeException("LRN " + newLrn + " already exists for another student.");
                    }
                }
                student.setLrn(newLrn);
            }
            // If LRN is the same, no need to update
        } else {
            // If new LRN is empty/null, clear it
            student.setLrn(null);
        }
        
        // Note: Grade Level, Strand, Section are NOT updated - they remain unchanged
        
        Student updatedStudent = studentRepository.save(student);
        return StudentDto.fromStudent(updatedStudent);
    }
    
    @Transactional
    public void archiveStudent(Long id, String archiveReason) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        student.setIsArchived(true);
        student.setArchiveReason(archiveReason);
        student.setArchivedAt(LocalDateTime.now());
        
        studentRepository.save(student);
    }
    
    @Transactional
    public void restoreStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        student.setIsArchived(false);
        student.setArchiveReason(null);
        student.setArchivedAt(null);
        
        studentRepository.save(student);
    }
    
    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }
    
    /**
     * Clears all students from the database.
     * This removes all student records from all grades, sections, and school years.
     */
    @Transactional
    public void clearAllStudents() {
        long count = studentRepository.count();
        if (count > 0) {
            studentRepository.deleteAll();
            System.out.println("✓ Cleared all " + count + " students from the database.");
        } else {
            System.out.println("✓ No students found to clear.");
        }
    }
    
    /**
     * Updates all existing students in the database with unique full names.
     * This ensures every student has a unique first + middle + last name combination.
     */
    @Transactional
    public void updateAllStudentsWithUniqueNames() {
        List<Student> allStudents = studentRepository.findAllByOrderByNameAsc();
        
        if (allStudents.isEmpty()) {
            System.out.println("✓ No students found to update.");
            return;
        }
        
        System.out.println("✓ Updating " + allStudents.size() + " students with unique full names...");
        
        // Comprehensive name pools
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
        
        String[] middleNames = {
            "Cruz", "Reyes", "Santos", "Garcia", "Lopez", "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez",
            "Bautista", "Fernandez", "Ramos", "Torres", "Villanueva", "Mendoza", "Aquino", "Castro", "Romero", "Vargas",
            "Flores", "Herrera", "Jimenez", "Moreno", "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado",
            "Molina", "Vasquez", "Guerrero", "Pineda", "Alvarez", "Cortez", "Domingo", "Espinosa", "Fuentes", "Guzman",
            "Hernandez", "Ibarra", "Javier", "Kalaw", "Luna", "Maceda", "Nunez", "Ocampo", "Pascual", "Quizon"
        };
        
        String[] lastNames = {
            "Santos", "Reyes", "Cruz", "Bautista", "Villanueva", "Fernandez", "Ramos", "Torres", "Garcia", "Lopez",
            "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez", "Rivera", "Morales", "Ortiz", "Gutierrez", "Castillo",
            "Dela Cruz", "Mendoza", "Aquino", "Castro", "Romero", "Vargas", "Flores", "Herrera", "Jimenez", "Moreno",
            "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado", "Molina", "Vasquez", "Guerrero", "Pineda",
            "Alvarez", "Cortez", "Domingo", "Espinosa", "Fuentes", "Guzman", "Hernandez", "Ibarra", "Javier", "Kalaw",
            "Luna", "Maceda", "Nunez", "Ocampo", "Pascual", "Quizon", "Salazar", "Tolentino", "Uy", "Valdez"
        };
        
        java.util.Set<String> usedFullNames = new java.util.HashSet<>();
        int updatedCount = 0;
        
        for (int i = 0; i < allStudents.size(); i++) {
            Student student = allStudents.get(i);
            String newName;
            int attempts = 0;
            
            // Generate unique full name combination
            do {
                int firstNameIndex = (i * 3) % firstNames.length;
                int middleNameIndex = (i * 5) % middleNames.length;
                int lastNameIndex = (i * 7) % lastNames.length;
                
                // If we've tried many times, use different indices
                if (attempts > 0) {
                    firstNameIndex = (i * 3 + attempts) % firstNames.length;
                    middleNameIndex = (i * 5 + attempts * 2) % middleNames.length;
                    lastNameIndex = (i * 7 + attempts * 3) % lastNames.length;
                }
                
                newName = firstNames[firstNameIndex] + " " + middleNames[middleNameIndex] + " " + lastNames[lastNameIndex];
                attempts++;
                
                // Safety check
                if (attempts > 1000) {
                    // Fallback: add index to ensure uniqueness
                    newName = firstNames[firstNameIndex] + " " + middleNames[middleNameIndex] + " " + lastNames[lastNameIndex] + " " + (i + 1);
                    break;
                }
            } while (usedFullNames.contains(newName.toLowerCase()));
            
            usedFullNames.add(newName.toLowerCase());
            
            // Always update to ensure unique name (even if current name is already unique)
            String oldName = student.getName();
            if (!newName.equals(oldName)) {
                student.setName(newName);
                studentRepository.save(student);
                updatedCount++;
                if (updatedCount <= 10) { // Show first 10 updates
                    System.out.println("  - Updated: '" + oldName + "' → '" + newName + "'");
                }
            }
        }
        
        System.out.println("✓ Updated " + updatedCount + " students with unique full names.");
        if (updatedCount > 10) {
            System.out.println("  ... and " + (updatedCount - 10) + " more students updated.");
        }
    }
    
    /**
     * Utility method to fix duplicate student names by using name variations
     * (different middle names, last names, or suffixes like Jr., Sr., II, III).
     * This should be run once to fix existing duplicates in the database.
     */
    @Transactional
    public void fixDuplicateNames() {
        List<Student> allStudents = studentRepository.findAllByOrderByNameAsc();
        java.util.Map<String, java.util.List<Student>> nameGroups = new java.util.HashMap<>();
        
        // Group students by name (case-insensitive, trimmed)
        for (Student student : allStudents) {
            if (student.getName() != null) {
                String normalizedName = student.getName().trim().toLowerCase();
                nameGroups.computeIfAbsent(normalizedName, k -> new java.util.ArrayList<>()).add(student);
            }
        }
        
        // Alternative last names to use for variations
        String[] alternativeLastNames = {
            "Santos", "Reyes", "Cruz", "Bautista", "Villanueva", "Fernandez", "Ramos", "Torres", "Garcia", "Lopez",
            "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez", "Rivera", "Morales", "Ortiz", "Gutierrez", "Castillo",
            "Dela Cruz", "Mendoza", "Aquino", "Castro", "Romero", "Vargas", "Flores", "Herrera", "Jimenez", "Moreno",
            "Navarro", "Ortega", "Vega", "Medina", "Silva", "Delgado", "Molina", "Vasquez", "Guerrero", "Pineda"
        };
        
        // Alternative middle names
        String[] alternativeMiddleNames = {
            "Cruz", "Reyes", "Santos", "Garcia", "Lopez", "Martinez", "Rodriguez", "Gonzalez", "Perez", "Sanchez",
            "Bautista", "Fernandez", "Ramos", "Torres", "Villanueva", "Mendoza", "Aquino", "Castro", "Romero", "Vargas"
        };
        
        // Name suffixes
        String[] nameSuffixes = {"Jr.", "Sr.", "II", "III", "IV"};
        
        // Fix duplicates
        int fixedCount = 0;
        for (java.util.Map.Entry<String, java.util.List<Student>> entry : nameGroups.entrySet()) {
            java.util.List<Student> studentsWithSameName = entry.getValue();
            if (studentsWithSameName.size() > 1) {
                // Keep first one as-is, modify others
                for (int i = 1; i < studentsWithSameName.size(); i++) {
                    Student student = studentsWithSameName.get(i);
                    String originalName = student.getName().trim();
                    String newName = generateUniqueNameVariation(originalName, i, alternativeLastNames, alternativeMiddleNames, nameSuffixes, student.getId());
                    
                    student.setName(newName);
                    studentRepository.save(student);
                    fixedCount++;
                    System.out.println("  - Renamed: '" + originalName + "' → '" + newName + "'");
                }
            }
        }
        
        System.out.println("✓ Fixed " + fixedCount + " duplicate student names using name variations.");
    }
    
    /**
     * Generates a unique name variation by modifying middle name, last name, or adding suffixes.
     */
    private String generateUniqueNameVariation(String originalName, int index, String[] alternativeLastNames, 
                                                String[] alternativeMiddleNames, String[] nameSuffixes, Long studentId) {
        // Parse the name into parts (assuming format: FirstName MiddleName LastName)
        String[] parts = originalName.split("\\s+");
        
        if (parts.length < 2) {
            // If name doesn't have enough parts, just add a suffix
            String baseName = originalName;
            int suffixIndex = (index - 1) % nameSuffixes.length;
            String newName = baseName + " " + nameSuffixes[suffixIndex];
            
            // Check if unique, if not try different suffixes
            int attempts = 0;
            while (studentRepository.existsByNameIgnoreCaseExcludingId(newName, studentId) && attempts < nameSuffixes.length * 2) {
                suffixIndex = (suffixIndex + 1) % nameSuffixes.length;
                newName = baseName + " " + nameSuffixes[suffixIndex];
                attempts++;
            }
            
            // If still not unique, try adding a different last name
            if (studentRepository.existsByNameIgnoreCaseExcludingId(newName, studentId)) {
                int lastNameIndex = (index - 1) % alternativeLastNames.length;
                newName = baseName + " " + alternativeLastNames[lastNameIndex];
            }
            
            return newName;
        }
        
        String firstName = parts[0];
        String middleName = parts.length > 2 ? parts[1] : "";
        String lastName = parts[parts.length - 1];
        
        // Strategy 1: Try changing the middle name
        if (parts.length >= 3) {
            int middleNameIndex = ((index - 1) * 3) % alternativeMiddleNames.length;
            String newMiddleName = alternativeMiddleNames[middleNameIndex];
            // Make sure it's different from current middle name
            if (!newMiddleName.equalsIgnoreCase(middleName)) {
                String newName = firstName + " " + newMiddleName + " " + lastName;
                if (!studentRepository.existsByNameIgnoreCaseExcludingId(newName, studentId)) {
                    return newName;
                }
            }
        }
        
        // Strategy 2: Try changing the last name
        int lastNameIndex = ((index - 1) * 5) % alternativeLastNames.length;
        String newLastName = alternativeLastNames[lastNameIndex];
        // Make sure it's different from current last name
        if (!newLastName.equalsIgnoreCase(lastName)) {
            String newName;
            if (parts.length >= 3) {
                newName = firstName + " " + middleName + " " + newLastName;
            } else {
                newName = firstName + " " + newLastName;
            }
            if (!studentRepository.existsByNameIgnoreCaseExcludingId(newName, studentId)) {
                return newName;
            }
        }
        
        // Strategy 3: Add a suffix (Jr., Sr., II, III, etc.)
        int suffixIndex = (index - 1) % nameSuffixes.length;
        String suffix = nameSuffixes[suffixIndex];
        String newName = originalName + " " + suffix;
        
        // Check if unique, if not try different suffixes
        int attempts = 0;
        while (studentRepository.existsByNameIgnoreCaseExcludingId(newName, studentId) && attempts < nameSuffixes.length * 2) {
            suffixIndex = (suffixIndex + 1) % nameSuffixes.length;
            suffix = nameSuffixes[suffixIndex];
            newName = originalName + " " + suffix;
            attempts++;
        }
        
        // Strategy 4: If still not unique, combine last name change with suffix
        if (studentRepository.existsByNameIgnoreCaseExcludingId(newName, studentId)) {
            lastNameIndex = ((index - 1) * 7 + 1) % alternativeLastNames.length;
            newLastName = alternativeLastNames[lastNameIndex];
            if (parts.length >= 3) {
                newName = firstName + " " + middleName + " " + newLastName + " " + suffix;
            } else {
                newName = firstName + " " + newLastName + " " + suffix;
            }
        }
        
        return newName;
    }
    
    /**
     * Checks if two names match when ignoring common suffixes (Jr., Sr., II, III, IV, etc.)
     * Returns true if the names are the same after removing suffixes, false otherwise.
     */
    private boolean namesMatchWithoutSuffix(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        
        // Normalize names: trim and convert to lowercase
        String normalized1 = name1.trim().toLowerCase();
        String normalized2 = name2.trim().toLowerCase();
        
        // Remove common suffixes from both names
        normalized1 = removeSuffixes(normalized1);
        normalized2 = removeSuffixes(normalized2);
        
        // Compare normalized names
        return normalized1.equals(normalized2);
    }
    
    /**
     * Removes common name suffixes from a name string.
     * Suffixes: jr, sr, ii, iii, iv, v, junior, senior, 2nd, 3rd, 4th, etc.
     */
    private String removeSuffixes(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // Common suffixes to remove (case-insensitive)
        String[] suffixes = {
            "jr", "sr", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x",
            "junior", "senior", "2nd", "3rd", "4th", "5th",
            "jr.", "sr.", "ii.", "iii.", "iv.", "v.", "vi.", "vii.", "viii.", "ix.", "x."
        };
        
        String normalized = name.trim();
        
        // Remove suffixes from the end of the name
        for (String suffix : suffixes) {
            // Check if name ends with suffix (with or without period)
            String suffixWithSpace = " " + suffix;
            String suffixWithSpacePeriod = " " + suffix + ".";
            
            if (normalized.endsWith(suffixWithSpacePeriod)) {
                normalized = normalized.substring(0, normalized.length() - suffixWithSpacePeriod.length()).trim();
            } else if (normalized.endsWith(suffixWithSpace)) {
                normalized = normalized.substring(0, normalized.length() - suffixWithSpace.length()).trim();
            } else if (normalized.endsWith(suffix + ".")) {
                normalized = normalized.substring(0, normalized.length() - (suffix.length() + 1)).trim();
            } else if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
            }
        }
        
        return normalized;
    }
    
    private int calculateAge(LocalDate birthdate) {
        if (birthdate == null) {
            return 0;
        }
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}
