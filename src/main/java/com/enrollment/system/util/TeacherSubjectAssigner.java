package com.enrollment.system.util;

import com.enrollment.system.model.Section;
import com.enrollment.system.model.Subject;
import com.enrollment.system.model.TeacherAssignment;
import com.enrollment.system.model.User;
import com.enrollment.system.repository.SectionRepository;
import com.enrollment.system.repository.SubjectRepository;
import com.enrollment.system.repository.TeacherAssignmentRepository;
import com.enrollment.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to assign sample subjects with sections to teachers for testing purposes.
 * This creates complete TeacherAssignment records (subject-section pairs) with proper grade and strand matching.
 */
@Component
public class TeacherSubjectAssigner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private TeacherAssignmentRepository teacherAssignmentRepository;
    
    /**
     * Assigns 8 complete subject-section assignments to teachers named "Hazel" and "Arnel"
     * Each assignment includes subject, section, grade, and strand information.
     */
    @Transactional
    public void assignSampleSubjectsToTestTeachers() {
        // Find teachers by full name (case-insensitive partial match)
        List<User> hazelTeachers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.UserRole.TEACHER)
                .filter(user -> user.getFullName() != null && 
                        user.getFullName().toLowerCase().contains("hazel"))
                .collect(Collectors.toList());
        
        List<User> arnelTeachers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.UserRole.TEACHER)
                .filter(user -> user.getFullName() != null && 
                        user.getFullName().toLowerCase().contains("arnel"))
                .collect(Collectors.toList());
        
        // Get all available subjects and sections
        List<Subject> allSubjects = subjectRepository.findAll().stream()
                .filter(subject -> subject.getIsActive() == null || subject.getIsActive())
                .collect(Collectors.toList());
        
        List<Section> allSections = sectionRepository.findAll().stream()
                .filter(section -> section.getIsActive() == null || section.getIsActive())
                .collect(Collectors.toList());
        
        if (allSubjects.isEmpty()) {
            System.out.println("WARNING: No subjects found in database. Cannot assign subjects to teachers.");
            return;
        }
        
        if (allSections.isEmpty()) {
            System.out.println("WARNING: No sections found in database. Cannot assign subjects to teachers.");
            return;
        }
        
        // Assign complete assignments to Hazel teachers
        for (User hazel : hazelTeachers) {
            assignCompleteAssignmentsToTeacher(hazel, allSubjects, allSections, "Hazel");
        }
        
        // Assign complete assignments to Arnel teachers
        for (User arnel : arnelTeachers) {
            assignCompleteAssignmentsToTeacher(arnel, allSubjects, allSections, "Arnel");
        }
    }
    
    /**
     * Assigns exactly 8 complete subject-section assignments to a teacher.
     * Each assignment matches subject grade level with section grade level.
     */
    private void assignCompleteAssignmentsToTeacher(User teacher, List<Subject> allSubjects, 
                                                     List<Section> allSections, String teacherName) {
        // Check existing assignments
        List<TeacherAssignment> existingAssignments = teacherAssignmentRepository.findByTeacherId(teacher.getId());
        int currentAssignmentCount = existingAssignments.size();
        
        if (currentAssignmentCount >= 8) {
            System.out.println("Teacher " + teacher.getFullName() + " already has " + currentAssignmentCount + 
                    " assignments. Preserving existing assignments.");
            return;
        }
        
        // Group sections by grade level for easier matching
        Map<Integer, List<Section>> sectionsByGrade = allSections.stream()
                .filter(section -> section.getGradeLevel() != null)
                .collect(Collectors.groupingBy(Section::getGradeLevel));
        
        // Filter available subjects (not already fully assigned)
        List<Subject> availableSubjects = allSubjects.stream()
                .filter(subject -> subject.getGradeLevel() != null)
                .filter(subject -> {
                    // Include subject if it doesn't have 8 assignments yet
                    long subjectAssignmentCount = existingAssignments.stream()
                            .filter(ta -> ta.getSubject().getId().equals(subject.getId()))
                            .count();
                    return subjectAssignmentCount < 8;
                })
                .collect(Collectors.toList());
        
        if (availableSubjects.isEmpty()) {
            System.out.println("WARNING: No available subjects for " + teacher.getFullName());
            return;
        }
        
        // Create assignments: need 8 total (subject-section pairs)
        List<TeacherAssignment> assignmentsToCreate = new ArrayList<>();
        Set<String> usedCombinations = new HashSet<>();
        
        // Track which subjects we've used
        Map<Long, Integer> subjectUsageCount = new HashMap<>();
        
        // First, try to assign one section per subject (up to 8 different subjects)
        for (Subject subject : availableSubjects) {
            if (assignmentsToCreate.size() >= 8) {
                break;
            }
            
            Integer subjectGrade = subject.getGradeLevel();
            List<Section> matchingSections = sectionsByGrade.getOrDefault(subjectGrade, Collections.emptyList());
            
            if (matchingSections.isEmpty()) {
                continue; // Skip subjects with no matching sections
            }
            
            // Find a section for this subject that we haven't used yet
            for (Section section : matchingSections) {
                String combination = subject.getId() + "_" + section.getId();
                
                // Check if this combination already exists for this teacher
                boolean alreadyExists = existingAssignments.stream()
                        .anyMatch(ta -> ta.getSubject().getId().equals(subject.getId()) && 
                                      ta.getSection().getId().equals(section.getId()));
                
                if (!alreadyExists && !usedCombinations.contains(combination)) {
                    TeacherAssignment assignment = new TeacherAssignment(teacher, subject, section);
                    assignmentsToCreate.add(assignment);
                    usedCombinations.add(combination);
                    subjectUsageCount.put(subject.getId(), 
                            subjectUsageCount.getOrDefault(subject.getId(), 0) + 1);
                    break; // One section per subject for now
                }
            }
        }
        
        // If we still need more assignments, assign additional sections to existing subjects
        if (assignmentsToCreate.size() < 8) {
            for (Subject subject : availableSubjects) {
                if (assignmentsToCreate.size() >= 8) {
                    break;
                }
                
                Integer subjectGrade = subject.getGradeLevel();
                List<Section> matchingSections = sectionsByGrade.getOrDefault(subjectGrade, Collections.emptyList());
                
                for (Section section : matchingSections) {
                    if (assignmentsToCreate.size() >= 8) {
                        break;
                    }
                    
                    String combination = subject.getId() + "_" + section.getId();
                    
                    // Check if this combination already exists
                    boolean alreadyExists = existingAssignments.stream()
                            .anyMatch(ta -> ta.getSubject().getId().equals(subject.getId()) && 
                                          ta.getSection().getId().equals(section.getId()));
                    
                    if (!alreadyExists && !usedCombinations.contains(combination)) {
                        TeacherAssignment assignment = new TeacherAssignment(teacher, subject, section);
                        assignmentsToCreate.add(assignment);
                        usedCombinations.add(combination);
                        break;
                    }
                }
            }
        }
        
        // Save all assignments
        if (!assignmentsToCreate.isEmpty()) {
            teacherAssignmentRepository.saveAll(assignmentsToCreate);
            teacherAssignmentRepository.flush();
            
            System.out.println("✓ Assigned " + assignmentsToCreate.size() + " complete assignments to " + 
                    teacher.getFullName() + " (Total: " + (currentAssignmentCount + assignmentsToCreate.size()) + " assignments)");
            
            // Print details
            for (TeacherAssignment assignment : assignmentsToCreate) {
                Subject subject = assignment.getSubject();
                Section section = assignment.getSection();
                System.out.println("  - " + subject.getName() + " (Grade " + subject.getGradeLevel() + 
                        ") → " + section.getName() + " (" + section.getStrand() + " - Grade " + 
                        section.getGradeLevel() + ")");
            }
        } else {
            System.out.println("WARNING: Could not create any assignments for " + teacher.getFullName() + 
                    ". Check if subjects and sections with matching grade levels exist.");
        }
    }
}
