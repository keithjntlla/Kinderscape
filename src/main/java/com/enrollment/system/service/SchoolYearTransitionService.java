package com.enrollment.system.service;

import com.enrollment.system.model.SchoolYear;
import com.enrollment.system.model.Student;
import com.enrollment.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class SchoolYearTransitionService {
    
    @Autowired
    private SchoolYearService schoolYearService;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private com.enrollment.system.repository.SchoolYearRepository schoolYearRepository;
    
    @Transactional
    public TransitionResult transitionToNewSchoolYear(Long newSchoolYearId, boolean carryOverEnrolled, boolean carryOverPending) {
        // Get the new school year entity
        SchoolYear newSchoolYearEntity = schoolYearRepository.findById(newSchoolYearId)
                .orElseThrow(() -> new RuntimeException("School year not found with id: " + newSchoolYearId));
        
        // Get current school year
        SchoolYear currentSchoolYear;
        try {
            currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
        } catch (RuntimeException e) {
            // No current school year - this is okay for first-time setup
            currentSchoolYear = null;
        }
        
        // Get students to carry over
        List<Student> studentsToCarryOver = new ArrayList<>();
        
        if (currentSchoolYear != null) {
            List<Student> allStudents = studentRepository.findAll();
            
            for (Student student : allStudents) {
                // Only process students from current school year
                if (student.getSchoolYear() == null || 
                    !student.getSchoolYear().getId().equals(currentSchoolYear.getId())) {
                    continue;
                }
                
                // Skip archived students
                if (Boolean.TRUE.equals(student.getIsArchived())) {
                    continue;
                }
                
                String status = student.getEnrollmentStatus();
                if (status == null) {
                    status = "";
                }
                
                // Check if we should carry over this student
                boolean shouldCarryOver = false;
                if (carryOverEnrolled && "Enrolled".equals(status)) {
                    shouldCarryOver = true;
                } else if (carryOverPending && "Pending".equals(status)) {
                    shouldCarryOver = true;
                }
                
                if (shouldCarryOver) {
                    studentsToCarryOver.add(student);
                }
            }
        }
        
        // Create new student records for the new school year
        int enrolledCount = 0;
        int pendingCount = 0;
        int skippedCount = 0;
        
        List<Student> newStudents = new ArrayList<>();
        
        for (Student oldStudent : studentsToCarryOver) {
            // Skip Grade 12 students (they graduate)
            if (oldStudent.getGradeLevel() != null && oldStudent.getGradeLevel() == 12) {
                skippedCount++;
                continue;
            }
            
            // Create new student record
            Student newStudent = new Student();
            
            // Copy personal information
            newStudent.setName(oldStudent.getName());
            newStudent.setBirthdate(oldStudent.getBirthdate());
            newStudent.setAge(oldStudent.getAge());
            newStudent.setSex(oldStudent.getSex());
            newStudent.setAddress(oldStudent.getAddress());
            newStudent.setContactNumber(oldStudent.getContactNumber());
            newStudent.setParentGuardianName(oldStudent.getParentGuardianName());
            newStudent.setParentGuardianContact(oldStudent.getParentGuardianContact());
            newStudent.setParentGuardianRelationship(oldStudent.getParentGuardianRelationship());
            newStudent.setStrand(oldStudent.getStrand());
            newStudent.setPreviousSchool(oldStudent.getPreviousSchool());
            newStudent.setGwa(oldStudent.getGwa());
            newStudent.setLrn(oldStudent.getLrn());
            
            // Set new school year
            newStudent.setSchoolYear(newSchoolYearEntity);
            
            // Increment grade level
            if (oldStudent.getGradeLevel() != null) {
                if (oldStudent.getGradeLevel() == 11) {
                    newStudent.setGradeLevel(12);
                } else if (oldStudent.getGradeLevel() == 12) {
                    // Should not reach here due to skip above, but just in case
                    skippedCount++;
                    continue;
                } else {
                    newStudent.setGradeLevel(oldStudent.getGradeLevel() + 1);
                }
            }
            
            // Set enrollment status to Pending (requires re-enrollment)
            newStudent.setEnrollmentStatus("Pending");
            
            // Clear section assignment (they need new section)
            newStudent.setSection(null);
            
            // Not archived
            newStudent.setIsArchived(false);
            
            newStudents.add(newStudent);
            
            if ("Enrolled".equals(oldStudent.getEnrollmentStatus())) {
                enrolledCount++;
            } else {
                pendingCount++;
            }
        }
        
        // Save all new students
        if (!newStudents.isEmpty()) {
            studentRepository.saveAll(newStudents);
        }
        
        // Set new school year as current
        schoolYearService.setCurrentSchoolYear(newSchoolYearId);
        
        TransitionResult result = new TransitionResult();
        result.setEnrolledCarriedOver(enrolledCount);
        result.setPendingCarriedOver(pendingCount);
        result.setSkipped(skippedCount);
        result.setTotalCarriedOver(enrolledCount + pendingCount);
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public TransitionPreview getTransitionPreview(Long newSchoolYearId, boolean carryOverEnrolled, boolean carryOverPending) {
        SchoolYear currentSchoolYear;
        try {
            currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
        } catch (RuntimeException e) {
            currentSchoolYear = null;
        }
        
        int enrolledCount = 0;
        int pendingCount = 0;
        int skippedCount = 0;
        
        if (currentSchoolYear != null) {
            List<Student> allStudents = studentRepository.findAll();
            
            for (Student student : allStudents) {
                if (student.getSchoolYear() == null || 
                    !student.getSchoolYear().getId().equals(currentSchoolYear.getId())) {
                    continue;
                }
                
                if (Boolean.TRUE.equals(student.getIsArchived())) {
                    continue;
                }
                
                String status = student.getEnrollmentStatus();
                if (status == null) {
                    status = "";
                }
                
                boolean shouldCarryOver = false;
                if (carryOverEnrolled && "Enrolled".equals(status)) {
                    shouldCarryOver = true;
                } else if (carryOverPending && "Pending".equals(status)) {
                    shouldCarryOver = true;
                }
                
                if (shouldCarryOver) {
                    // Check if Grade 12 (will be skipped)
                    if (student.getGradeLevel() != null && student.getGradeLevel() == 12) {
                        skippedCount++;
                    } else {
                        if ("Enrolled".equals(status)) {
                            enrolledCount++;
                        } else {
                            pendingCount++;
                        }
                    }
                }
            }
        }
        
        TransitionPreview preview = new TransitionPreview();
        preview.setEnrolledCount(enrolledCount);
        preview.setPendingCount(pendingCount);
        preview.setSkippedCount(skippedCount);
        preview.setTotalCount(enrolledCount + pendingCount);
        
        return preview;
    }
    
    // Inner classes for results
    public static class TransitionResult {
        private int enrolledCarriedOver;
        private int pendingCarriedOver;
        private int skipped;
        private int totalCarriedOver;
        
        // Getters and Setters
        public int getEnrolledCarriedOver() {
            return enrolledCarriedOver;
        }
        
        public void setEnrolledCarriedOver(int enrolledCarriedOver) {
            this.enrolledCarriedOver = enrolledCarriedOver;
        }
        
        public int getPendingCarriedOver() {
            return pendingCarriedOver;
        }
        
        public void setPendingCarriedOver(int pendingCarriedOver) {
            this.pendingCarriedOver = pendingCarriedOver;
        }
        
        public int getSkipped() {
            return skipped;
        }
        
        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }
        
        public int getTotalCarriedOver() {
            return totalCarriedOver;
        }
        
        public void setTotalCarriedOver(int totalCarriedOver) {
            this.totalCarriedOver = totalCarriedOver;
        }
    }
    
    public static class TransitionPreview {
        private int enrolledCount;
        private int pendingCount;
        private int skippedCount;
        private int totalCount;
        
        // Getters and Setters
        public int getEnrolledCount() {
            return enrolledCount;
        }
        
        public void setEnrolledCount(int enrolledCount) {
            this.enrolledCount = enrolledCount;
        }
        
        public int getPendingCount() {
            return pendingCount;
        }
        
        public void setPendingCount(int pendingCount) {
            this.pendingCount = pendingCount;
        }
        
        public int getSkippedCount() {
            return skippedCount;
        }
        
        public void setSkippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }
    }
}

