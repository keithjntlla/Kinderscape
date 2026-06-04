package com.enrollment.system.service;

import com.enrollment.system.model.Subject;
import com.enrollment.system.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectService {
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    // Expose repository for direct access when needed (e.g., in TransactionTemplate)
    public SubjectRepository getSubjectRepository() {
        return subjectRepository;
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getActiveSubjects() {
        // Get all subjects and filter in Java to handle NULL isActive values in SQLite
        // SQLite's boolean handling can be inconsistent with NULL values
        List<Subject> allSubjects = subjectRepository.findAll();
        return allSubjects.stream()
            .filter(s -> s.getIsActive() != null && s.getIsActive())
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByGradeLevel(Integer gradeLevel) {
        return subjectRepository.findByGradeLevel(gradeLevel);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getActiveSubjectsByGradeLevel(Integer gradeLevel) {
        return subjectRepository.findByGradeLevelAndIsActiveTrue(gradeLevel);
    }
    
    @Transactional(readOnly = true)
    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public Subject createSubject(String name, Integer gradeLevel, String description) {
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Subject name is required");
        }
        
        if (gradeLevel == null || (gradeLevel != 11 && gradeLevel != 12)) {
            throw new RuntimeException("Grade level must be 11 or 12");
        }
        
        // Check if subject with same name and grade level already exists
        if (subjectRepository.existsByNameAndGradeLevel(name.trim(), gradeLevel)) {
            throw new RuntimeException("Subject '" + name + "' already exists for Grade " + gradeLevel);
        }
        
        // Create subject exactly like DataInitializer does - let @PrePersist handle timestamps
        Subject subject = new Subject();
        subject.setName(name.trim());
        subject.setGradeLevel(gradeLevel);
        subject.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
        subject.setIsActive(true);
        subject.setIsCustom(true); // Subjects created via service are custom (manually added)
        
        // Don't set timestamps explicitly - let @PrePersist handle it
        // This matches how DataInitializer creates subjects successfully
        Subject saved = subjectRepository.save(subject);
        
        // Verify timestamps were set by @PrePersist
        if (saved.getCreatedAt() == null) {
            // If @PrePersist didn't fire, set timestamps manually as fallback
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            saved.setCreatedAt(now);
            saved.setUpdatedAt(now);
            saved = subjectRepository.save(saved);
        }
        
        return saved;
    }
    
    @Transactional
    public Subject updateSubject(Long id, String name, Integer gradeLevel, String description) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
        
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Subject name is required");
        }
        
        if (gradeLevel == null || (gradeLevel != 11 && gradeLevel != 12)) {
            throw new RuntimeException("Grade level must be 11 or 12");
        }
        
        // Check if another subject with same name and grade level already exists
        if (!name.trim().equals(subject.getName()) || !gradeLevel.equals(subject.getGradeLevel())) {
            if (subjectRepository.existsByNameAndGradeLevel(name.trim(), gradeLevel)) {
                throw new RuntimeException("Subject '" + name + "' already exists for Grade " + gradeLevel);
            }
        }
        
        subject.setName(name.trim());
        subject.setGradeLevel(gradeLevel);
        subject.setDescription(description != null && !description.trim().isEmpty() ? description.trim() : null);
        
        return subjectRepository.save(subject);
    }
    
    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
        subjectRepository.delete(subject);
    }
    
    @Transactional
    public void deactivateSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }
    
    /**
     * Ensures all subjects have isActive set (fixes NULL values).
     * This is useful for databases where isActive might be NULL.
     */
    @Transactional
    public void ensureAllSubjectsHaveActiveStatus() {
        List<Subject> allSubjects = subjectRepository.findAll();
        boolean updated = false;
        for (Subject subject : allSubjects) {
            if (subject.getIsActive() == null) {
                subject.setIsActive(true); // Default to active if NULL
                subjectRepository.save(subject);
                updated = true;
            }
        }
        if (updated) {
            System.out.println("✓ Fixed NULL isActive values for subjects");
        }
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByStrand(String strand) {
        return subjectRepository.findByStrand(strand);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsBySubjectType(String subjectType) {
        return subjectRepository.findBySubjectType(subjectType);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByGradeLevelAndSubjectType(Integer gradeLevel, String subjectType) {
        return subjectRepository.findByGradeLevelAndSubjectType(gradeLevel, subjectType);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByGradeLevelAndStrand(Integer gradeLevel, String strand) {
        return subjectRepository.findByGradeLevelAndStrand(gradeLevel, strand);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsBySubjectTypeAndStrand(String subjectType, String strand) {
        return subjectRepository.findBySubjectTypeAndStrand(subjectType, strand);
    }
    
    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByGradeLevelAndSubjectTypeAndStrand(Integer gradeLevel, String subjectType, String strand) {
        return subjectRepository.findByGradeLevelAndSubjectTypeAndStrand(gradeLevel, subjectType, strand);
    }
}

