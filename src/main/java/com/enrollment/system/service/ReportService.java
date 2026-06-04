package com.enrollment.system.service;

import com.enrollment.system.dto.StudentDto;
import com.enrollment.system.model.*;
import com.enrollment.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private TeacherAssignmentRepository teacherAssignmentRepository;
    
    
    @Autowired
    private SchoolYearService schoolYearService;
    
    /**
     * Get students grouped by section for Student List by Section report
     */
    @Transactional(readOnly = true)
    public Map<Section, List<StudentDto>> getStudentsBySection() {
        SchoolYear currentSchoolYear;
        try {
            currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
        } catch (Exception e) {
            // No current school year set, use all students
            currentSchoolYear = null;
        }
        final SchoolYear finalCurrentSchoolYear = currentSchoolYear;
        
        List<Student> allStudents = studentRepository.findAllWithSectionByOrderByNameAsc();
        
        // Filter students
        List<Student> filteredStudents = allStudents.stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    // Filter by current school year if available
                    if (finalCurrentSchoolYear != null) {
                        try {
                            return student.getSchoolYear() != null && 
                                   student.getSchoolYear().getId().equals(finalCurrentSchoolYear.getId());
                        } catch (Exception e) {
                            return true;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        // Group by section
        Map<Section, List<StudentDto>> studentsBySection = new LinkedHashMap<>();
        
        // First, get all sections and initialize empty lists
        List<Section> allSections = sectionRepository.findAll().stream()
                .filter(section -> section.getIsActive() != null && section.getIsActive())
                .sorted(Comparator.comparing(Section::getGradeLevel)
                        .thenComparing(Section::getStrand)
                        .thenComparing(Section::getName))
                .collect(Collectors.toList());
        
        for (Section section : allSections) {
            studentsBySection.put(section, new ArrayList<>());
        }
        
        // Add students to their sections
        for (Student student : filteredStudents) {
            try {
                Section section = student.getSection();
                if (section != null) {
                    studentsBySection.computeIfAbsent(section, k -> new ArrayList<>())
                            .add(com.enrollment.system.dto.StudentDto.fromStudent(student));
                }
            } catch (Exception e) {
                // Skip if section cannot be loaded
            }
        }
        
        // Remove sections with no students
        studentsBySection.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        return studentsBySection;
    }
    
    /**
     * Get teacher assignments grouped by teacher for Teacher Assignment Report
     */
    @Transactional(readOnly = true)
    public Map<User, List<TeacherAssignmentInfo>> getTeacherAssignments() {
        List<TeacherAssignment> allAssignments = teacherAssignmentRepository.findAll();
        
        // Group by teacher
        Map<User, List<TeacherAssignmentInfo>> assignmentsByTeacher = new LinkedHashMap<>();
        
        for (TeacherAssignment assignment : allAssignments) {
            try {
                User teacher = assignment.getTeacher();
                Subject subject = assignment.getSubject();
                Section section = assignment.getSection();
                
                if (teacher != null && subject != null && section != null) {
                    TeacherAssignmentInfo info = new TeacherAssignmentInfo(
                            subject.getName(),
                            section.getName(),
                            section.getStrand(),
                            section.getGradeLevel()
                    );
                    
                    assignmentsByTeacher.computeIfAbsent(teacher, k -> new ArrayList<>())
                            .add(info);
                }
            } catch (Exception e) {
                // Skip if entities cannot be loaded
            }
        }
        
        // Sort teachers by name
        Map<User, List<TeacherAssignmentInfo>> sortedMap = new LinkedHashMap<>();
        assignmentsByTeacher.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(User::getFullName)))
                .forEachOrdered(entry -> sortedMap.put(entry.getKey(), entry.getValue()));
        
        return sortedMap;
    }
    
    /**
     * Get enrollment statistics for Enrollment Summary report
     */
    @Transactional(readOnly = true)
    public EnrollmentStatistics getEnrollmentStatistics() {
        SchoolYear currentSchoolYear;
        try {
            currentSchoolYear = schoolYearService.getCurrentSchoolYearEntity();
        } catch (Exception e) {
            // No current school year set, use all students
            currentSchoolYear = null;
        }
        final SchoolYear finalCurrentSchoolYear = currentSchoolYear;
        
        List<Student> allStudents = studentRepository.findAllWithSectionByOrderByNameAsc();
        
        // Filter students
        List<Student> filteredStudents = allStudents.stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    // Filter by current school year if available
                    if (finalCurrentSchoolYear != null) {
                        try {
                            return student.getSchoolYear() != null && 
                                   student.getSchoolYear().getId().equals(finalCurrentSchoolYear.getId());
                        } catch (Exception e) {
                            return true;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        EnrollmentStatistics stats = new EnrollmentStatistics();
        
        // Total enrolled
        stats.totalEnrolled = filteredStudents.stream()
                .filter(s -> "Enrolled".equals(s.getEnrollmentStatus()))
                .count();
        
        // Total pending
        stats.totalPending = filteredStudents.stream()
                .filter(s -> "Pending".equals(s.getEnrollmentStatus()))
                .count();
        
        // By grade level
        stats.byGradeLevel = new HashMap<>();
        for (Student student : filteredStudents) {
            if ("Enrolled".equals(student.getEnrollmentStatus()) && student.getGradeLevel() != null) {
                stats.byGradeLevel.merge(student.getGradeLevel(), 1L, Long::sum);
            }
        }
        
        // By strand
        stats.byStrand = new HashMap<>();
        for (Student student : filteredStudents) {
            if ("Enrolled".equals(student.getEnrollmentStatus()) && student.getStrand() != null) {
                stats.byStrand.merge(student.getStrand(), 1L, Long::sum);
            }
        }
        
        // By section
        stats.bySection = new HashMap<>();
        for (Student student : filteredStudents) {
            if ("Enrolled".equals(student.getEnrollmentStatus())) {
                try {
                    Section section = student.getSection();
                    if (section != null) {
                        String sectionKey = section.getName() + " (" + section.getStrand() + " - Grade " + section.getGradeLevel() + ")";
                        stats.bySection.merge(sectionKey, 1L, Long::sum);
                    }
                } catch (Exception e) {
                    // Skip if section cannot be loaded
                }
            }
        }
        
        // By gender
        stats.byGender = new HashMap<>();
        for (Student student : filteredStudents) {
            if ("Enrolled".equals(student.getEnrollmentStatus()) && student.getSex() != null) {
                stats.byGender.merge(student.getSex(), 1L, Long::sum);
            }
        }
        
        return stats;
    }
    
    /**
     * Inner class for teacher assignment information
     */
    public static class TeacherAssignmentInfo {
        public String subjectName;
        public String sectionName;
        public String strand;
        public Integer gradeLevel;
        
        public TeacherAssignmentInfo(String subjectName, String sectionName, String strand, Integer gradeLevel) {
            this.subjectName = subjectName;
            this.sectionName = sectionName;
            this.strand = strand;
            this.gradeLevel = gradeLevel;
        }
    }
    
    /**
     * Inner class for enrollment statistics
     */
    public static class EnrollmentStatistics {
        public long totalEnrolled;
        public long totalPending;
        public Map<Integer, Long> byGradeLevel;
        public Map<String, Long> byStrand;
        public Map<String, Long> bySection;
        public Map<String, Long> byGender;
    }
}

