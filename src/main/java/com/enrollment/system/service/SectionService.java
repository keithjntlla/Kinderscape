package com.enrollment.system.service;

import com.enrollment.system.model.Section;
import com.enrollment.system.model.Student;
import com.enrollment.system.repository.SectionRepository;
import com.enrollment.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {
    
    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }
    
    public List<Section> getActiveSections() {
        return sectionRepository.findByIsActiveTrue();
    }
    
    public List<Section> getSectionsByStrand(String strand) {
        return sectionRepository.findByStrand(strand);
    }
    
    public List<Section> getActiveSectionsByStrand(String strand) {
        return sectionRepository.findByStrandAndIsActiveTrue(strand);
    }
    
    public List<Section> getSectionsByStrandAndGradeLevel(String strand, Integer gradeLevel) {
        return sectionRepository.findByStrandAndGradeLevel(strand, gradeLevel);
    }
    
    public List<Section> getActiveSectionsByStrandAndGradeLevel(String strand, Integer gradeLevel) {
        return sectionRepository.findByStrandAndGradeLevelAndIsActiveTrue(strand, gradeLevel);
    }
    
    public Optional<Section> getSectionById(Long id) {
        return sectionRepository.findById(id);
    }
    
    @Transactional
    public Section saveSection(Section section) {
        return sectionRepository.save(section);
    }
    
    @Transactional
    public Section createSection(String name, String strand, Integer gradeLevel) {
        return createSection(name, strand, gradeLevel, null);
    }
    
    @Transactional
    public Section createSection(String name, String strand, Integer gradeLevel, Integer capacity) {
        // Check if section already exists
        if (sectionRepository.existsByNameAndStrandAndGradeLevel(name, strand, gradeLevel)) {
            throw new IllegalArgumentException("Section " + name + " already exists for " + strand + " Grade " + gradeLevel);
        }
        
        Section section = new Section(name, strand, gradeLevel);
        if (capacity != null) {
            section.setCapacity(capacity);
        }
        return sectionRepository.save(section);
    }
    
    @Transactional
    public void deleteSection(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + id));
        
        // Check if there are enrolled students in this section
        List<Student> enrolledStudents = studentRepository.findAll().stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    if (!"Enrolled".equals(student.getEnrollmentStatus())) {
                        return false;
                    }
                    if (student.getSection() == null) {
                        return false;
                    }
                    return student.getSection().getId().equals(id);
                })
                .toList();
        
        if (!enrolledStudents.isEmpty()) {
            throw new IllegalStateException("Cannot delete section " + section.getName() + 
                    " because it has " + enrolledStudents.size() + " enrolled student(s). " +
                    "Please reassign or archive these students first.");
        }
        
        // Soft delete by setting isActive to false
        section.setIsActive(false);
        sectionRepository.save(section);
    }
    
    @Transactional
    public Section updateSection(Long id, String name, String strand, Integer gradeLevel, Integer capacity) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + id));
        
        // Check if new name conflicts with existing section
        Optional<Section> existing = sectionRepository.findByNameAndStrandAndGradeLevel(name, strand, gradeLevel);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Section " + name + " already exists for " + strand + " Grade " + gradeLevel);
        }
        
        section.setName(name);
        section.setStrand(strand);
        section.setGradeLevel(gradeLevel);
        if (capacity != null) {
            section.setCapacity(capacity);
        }
        
        return sectionRepository.save(section);
    }
    
    @Transactional
    public void activateSection(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + id));
        section.setIsActive(true);
        sectionRepository.save(section);
    }
    
    @Transactional
    public void deletePermanently(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + id));
        
        // Check if there are enrolled students in this section
        List<Student> enrolledStudents = studentRepository.findAll().stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    if (!"Enrolled".equals(student.getEnrollmentStatus())) {
                        return false;
                    }
                    if (student.getSection() == null) {
                        return false;
                    }
                    return student.getSection().getId().equals(id);
                })
                .toList();
        
        if (!enrolledStudents.isEmpty()) {
            throw new IllegalStateException("Cannot delete permanently section " + section.getName() + 
                    " because it has " + enrolledStudents.size() + " enrolled student(s). " +
                    "Please reassign or archive these students first.");
        }
        
        // Permanent delete - actually remove from database
        sectionRepository.delete(section);
    }
    
    /**
     * Get the current number of enrolled students in a section
     */
    @Transactional(readOnly = true)
    public long getCurrentStudentCount(Long sectionId) {
        return studentRepository.countBySectionIdAndEnrolled(sectionId);
    }
    
    /**
     * Get the current number of enrolled students in a section for a specific semester
     */
    @Transactional(readOnly = true)
    public long getCurrentStudentCount(Long sectionId, Long semesterId) {
        if (semesterId == null) {
            return getCurrentStudentCount(sectionId);
        }
        return studentRepository.countBySectionIdAndSemesterIdAndEnrolled(sectionId, semesterId);
    }
    
    /**
     * Get the current number of enrolled students in a section for a specific semester,
     * verifying that students match the section's grade level and strand
     */
    @Transactional(readOnly = true)
    public long getCurrentStudentCount(Long sectionId, Long semesterId, Integer gradeLevel, String strand) {
        if (semesterId == null || gradeLevel == null || strand == null) {
            // Fall back to basic count if parameters are missing
            return getCurrentStudentCount(sectionId, semesterId);
        }
        return studentRepository.countBySectionIdAndSemesterIdAndGradeLevelAndStrandAndEnrolled(sectionId, semesterId, gradeLevel, strand);
    }
    
    /**
     * Check if a section has available capacity
     */
    @Transactional(readOnly = true)
    public boolean hasAvailableCapacity(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + sectionId));
        
        // Capacity must be set - if null, throw error (sections must have capacity defined)
        if (section.getCapacity() == null) {
            throw new IllegalStateException("Section " + section.getName() + " does not have a capacity set. Please set a capacity for this section before enrolling students.");
        }
        
        long currentCount = getCurrentStudentCount(sectionId);
        return currentCount < section.getCapacity();
    }
    
    /**
     * Check if a section has available capacity for a specific semester
     */
    @Transactional(readOnly = true)
    public boolean hasAvailableCapacity(Long sectionId, Long semesterId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + sectionId));
        
        // Capacity must be set - if null, throw error (sections must have capacity defined)
        if (section.getCapacity() == null) {
            throw new IllegalStateException("Section " + section.getName() + " does not have a capacity set. Please set a capacity for this section before enrolling students.");
        }
        
        // Use enhanced count that verifies grade level and strand match the section
        long currentCount = getCurrentStudentCount(sectionId, semesterId, section.getGradeLevel(), section.getStrand());
        // Check if current count is less than capacity (strictly less, not equal)
        // If currentCount is 40 and capacity is 40, then 40 < 40 is false, so no capacity available
        // If currentCount is 39 and capacity is 40, then 39 < 40 is true, so capacity available
        boolean hasCapacity = currentCount < section.getCapacity();
        
        return hasCapacity;
    }
    
    /**
     * Get available capacity (remaining slots) for a section
     */
    @Transactional(readOnly = true)
    public int getAvailableCapacity(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + sectionId));
        
        // If capacity is null, assume unlimited capacity
        if (section.getCapacity() == null) {
            return Integer.MAX_VALUE;
        }
        
        long currentCount = getCurrentStudentCount(sectionId);
        int available = section.getCapacity() - (int) currentCount;
        return Math.max(0, available);
    }
    
    /**
     * Get available capacity (remaining slots) for a section in a specific semester
     */
    @Transactional(readOnly = true)
    public int getAvailableCapacity(Long sectionId, Long semesterId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + sectionId));
        
        // If capacity is null, assume unlimited capacity
        if (section.getCapacity() == null) {
            return Integer.MAX_VALUE;
        }
        
        long currentCount = getCurrentStudentCount(sectionId, semesterId);
        int available = section.getCapacity() - (int) currentCount;
        return Math.max(0, available);
    }
    
    /**
     * Get alternative sections with available capacity for the same strand and grade level
     */
    @Transactional(readOnly = true)
    public List<Section> getAlternativeSections(String strand, Integer gradeLevel, Long excludeSectionId) {
        List<Section> allSections = getActiveSectionsByStrandAndGradeLevel(strand, gradeLevel);
        return allSections.stream()
                .filter(section -> !section.getId().equals(excludeSectionId))
                .filter(section -> hasAvailableCapacity(section.getId()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get alternative sections with available capacity for the same strand and grade level in a specific semester
     */
    @Transactional(readOnly = true)
    public List<Section> getAlternativeSections(String strand, Integer gradeLevel, Long excludeSectionId, Long semesterId) {
        List<Section> allSections = getActiveSectionsByStrandAndGradeLevel(strand, gradeLevel);
        return allSections.stream()
                .filter(section -> !section.getId().equals(excludeSectionId))
                .filter(section -> hasAvailableCapacity(section.getId(), semesterId))
                .collect(java.util.stream.Collectors.toList());
    }
}

