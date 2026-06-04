package com.enrollment.system.controller;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.Student;
import com.enrollment.system.repository.StudentRepository;
import com.enrollment.system.service.SchoolYearService;
import com.enrollment.system.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students/re-enroll")
@CrossOrigin(origins = "*")
public class ReEnrollmentController {
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SchoolYearService schoolYearService;
    
    @GetMapping("/eligible")
    public ResponseEntity<?> getEligibleStudentsForReEnrollment(@RequestParam(required = false) Long previousSchoolYearId) {
        try {
            // Get previous school year (the one that ended before current started)
            com.enrollment.system.model.SchoolYear previousSchoolYear = schoolYearService.getPreviousSchoolYearEntity();
            
            // If no previous school year, return empty list
            if (previousSchoolYear == null) {
                return ResponseEntity.ok(new java.util.ArrayList<>());
            }
            
            List<Student> allStudents = studentRepository.findAll();
            
            // Filter eligible students:
            // 1. From previous school year
            // 2. Grade 11 (will be promoted to Grade 12) OR Grade 12 (failed, will remain in Grade 12)
            // 3. Not archived
            // 4. Not graduated (archive reason must not be "GRADUATED")
            List<StudentDto> eligibleStudents = allStudents.stream()
                .filter(student -> {
                    if (Boolean.TRUE.equals(student.getIsArchived())) {
                        // Check if student is graduated - exclude from re-enrollment
                        String archiveReason = student.getArchiveReason();
                        if (archiveReason != null && "GRADUATED".equalsIgnoreCase(archiveReason.trim())) {
                            return false;
                        }
                        // Other archived students are also excluded
                        return false;
                    }
                    
                    // Must have a school year assigned
                    if (student.getSchoolYear() == null) {
                        return false;
                    }
                    
                    // Must be from previous school year
                    if (!student.getSchoolYear().getId().equals(previousSchoolYear.getId())) {
                        return false;
                    }
                    
                    // Must be Grade 11 or Grade 12
                    Integer gradeLevel = student.getGradeLevel();
                    return gradeLevel != null && (gradeLevel == 11 || gradeLevel == 12);
                })
                .map(StudentDto::fromStudent)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(eligibleStudents);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> reEnrollStudent(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());
            Integer newGradeLevel = request.get("newGradeLevel") != null ? 
                Integer.valueOf(request.get("newGradeLevel").toString()) : null;
            Long newSectionId = request.get("newSectionId") != null ? 
                Long.valueOf(request.get("newSectionId").toString()) : null;
            
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
            
            // Check if student is graduated - prevent re-enrollment
            String archiveReason = student.getArchiveReason();
            if (archiveReason != null && "GRADUATED".equalsIgnoreCase(archiveReason.trim())) {
                throw new RuntimeException("Graduated students cannot be re-enrolled. Student \"" + student.getName() + "\" has already graduated.");
            }
            
            // Get current school year
            com.enrollment.system.model.SchoolYear currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
            
            // Update student
            StudentDto studentDto = StudentDto.fromStudent(student);
            
            // Update grade level if provided
            if (newGradeLevel != null) {
                studentDto.setGradeLevel(newGradeLevel);
            }
            
            // Update section if provided
            if (newSectionId != null) {
                studentDto.setSectionId(newSectionId);
            }
            
            // Set enrollment status to Enrolled
            studentDto.setEnrollmentStatus("Enrolled");
            
            // Set current school year
            studentDto.setSchoolYearId(currentSchoolYear.getId());
            
            // Update student
            StudentDto updated = studentService.updateStudent(studentId, studentDto);
            
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/batch")
    public ResponseEntity<?> batchReEnrollStudents(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> studentIds = (List<Long>) request.get("studentIds");
            Integer newGradeLevel = request.get("newGradeLevel") != null ? 
                Integer.valueOf(request.get("newGradeLevel").toString()) : null;
            Long newSectionId = request.get("newSectionId") != null ? 
                Long.valueOf(request.get("newSectionId").toString()) : null;
            
            com.enrollment.system.model.SchoolYear currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
            
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            for (Long studentId : studentIds) {
                try {
                    Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new RuntimeException("Student not found"));
                    
                    // Check if student is graduated - prevent re-enrollment
                    String archiveReason = student.getArchiveReason();
                    if (archiveReason != null && "GRADUATED".equalsIgnoreCase(archiveReason.trim())) {
                        failCount++;
                        errors.add("Student " + studentId + " (\"" + student.getName() + "\"): Graduated students cannot be re-enrolled.");
                        continue;
                    }
                    
                    StudentDto studentDto = StudentDto.fromStudent(student);
                    
                    if (newGradeLevel != null) {
                        studentDto.setGradeLevel(newGradeLevel);
                    }
                    
                    if (newSectionId != null) {
                        studentDto.setSectionId(newSectionId);
                    }
                    
                    studentDto.setEnrollmentStatus("Enrolled");
                    studentDto.setSchoolYearId(currentSchoolYear.getId());
                    
                    studentService.updateStudent(studentId, studentDto);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("Student " + studentId + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("errors", errors);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

