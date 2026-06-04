package com.enrollment.system.service;

import com.enrollment.system.model.Strand;
import com.enrollment.system.model.Student;
import com.enrollment.system.repository.StrandRepository;
import com.enrollment.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StrandService {
    
    @Autowired
    private StrandRepository strandRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    public List<Strand> getAllStrands() {
        return strandRepository.findAllByOrderByNameAsc();
    }
    
    public List<Strand> getActiveStrands() {
        return strandRepository.findByIsActiveTrue();
    }
    
    public Optional<Strand> getStrandById(Long id) {
        return strandRepository.findById(id);
    }
    
    public Optional<Strand> getStrandByName(String name) {
        return strandRepository.findByName(name);
    }
    
    @Transactional
    public Strand createStrand(String name, String description) {
        // Check if strand already exists
        if (strandRepository.existsByName(name)) {
            throw new IllegalArgumentException("Strand " + name + " already exists");
        }
        
        Strand strand = new Strand(name, description);
        return strandRepository.save(strand);
    }
    
    @Transactional
    public Strand updateStrand(Long id, String name, String description) {
        Strand strand = strandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Strand not found with id: " + id));
        
        // Check if new name conflicts with existing strand
        Optional<Strand> existing = strandRepository.findByName(name);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Strand " + name + " already exists");
        }
        
        strand.setName(name);
        strand.setDescription(description);
        
        return strandRepository.save(strand);
    }
    
    @Transactional
    public void deleteStrand(Long id) {
        Strand strand = strandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Strand not found with id: " + id));
        
        // Check if there are enrolled students in this strand - use the same normalization logic
        String strandName = strand.getName();
        if (strandName == null || strandName.trim().isEmpty()) {
            throw new IllegalArgumentException("Strand name is null or empty");
        }
        String normalizedStrandName = strandName.toUpperCase().replaceAll("\\s+", " ").trim();
        
        List<Student> enrolledStudents = studentRepository.findAll().stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    String enrollmentStatus = student.getEnrollmentStatus();
                    if (enrollmentStatus == null || !"Enrolled".equals(enrollmentStatus.trim())) {
                        return false;
                    }
                    String studentStrand = student.getStrand();
                    if (studentStrand == null || studentStrand.trim().isEmpty()) {
                        return false;
                    }
                    // Normalize and compare
                    String normalizedStudentStrand = studentStrand.toUpperCase().replaceAll("\\s+", " ").trim();
                    return normalizedStudentStrand.equals(normalizedStrandName);
                })
                .toList();
        
        if (!enrolledStudents.isEmpty()) {
            throw new IllegalStateException("Cannot delete strand " + strand.getName() + 
                    " because it has " + enrolledStudents.size() + " enrolled student(s). " +
                    "Please reassign or archive these students first.");
        }
        
        // Soft delete by setting isActive to false
        strand.setIsActive(false);
        strandRepository.save(strand);
    }
    
    @Transactional(readOnly = true)
    public boolean hasActiveStudents(String strandName) {
        if (strandName == null || strandName.trim().isEmpty()) {
            return false;
        }
        String trimmedStrandName = strandName.trim();
        // Normalize the strand name for comparison (remove extra spaces, convert to uppercase for comparison)
        String normalizedStrandName = trimmedStrandName.toUpperCase().replaceAll("\\s+", " ").trim();
        
        return studentRepository.findAll().stream()
                .anyMatch(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    String enrollmentStatus = student.getEnrollmentStatus();
                    if (enrollmentStatus == null || !"Enrolled".equals(enrollmentStatus.trim())) {
                        return false;
                    }
                    String studentStrand = student.getStrand();
                    if (studentStrand == null || studentStrand.trim().isEmpty()) {
                        return false;
                    }
                    // Normalize student strand for comparison
                    String normalizedStudentStrand = studentStrand.toUpperCase().replaceAll("\\s+", " ").trim();
                    // Use normalized comparison
                    return normalizedStudentStrand.equals(normalizedStrandName);
                });
    }
    
    @Transactional(readOnly = true)
    public java.util.List<Student> getEnrolledStudentsByStrand(String strandName) {
        if (strandName == null || strandName.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String trimmedStrandName = strandName.trim();
        String normalizedStrandName = trimmedStrandName.toUpperCase().replaceAll("\\s+", " ").trim();
        
        return studentRepository.findAll().stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    String enrollmentStatus = student.getEnrollmentStatus();
                    if (enrollmentStatus == null || !"Enrolled".equals(enrollmentStatus.trim())) {
                        return false;
                    }
                    String studentStrand = student.getStrand();
                    if (studentStrand == null || studentStrand.trim().isEmpty()) {
                        return false;
                    }
                    String normalizedStudentStrand = studentStrand.toUpperCase().replaceAll("\\s+", " ").trim();
                    return normalizedStudentStrand.equals(normalizedStrandName);
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional
    public void activateStrand(Long id) {
        Strand strand = strandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Strand not found with id: " + id));
        strand.setIsActive(true);
        strandRepository.save(strand);
    }
    
    @Transactional
    public void deletePermanently(Long id) {
        Strand strand = strandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Strand not found with id: " + id));
        
        // Check if there are enrolled students in this strand - use the same normalization logic
        String strandName = strand.getName();
        if (strandName == null || strandName.trim().isEmpty()) {
            throw new IllegalArgumentException("Strand name is null or empty");
        }
        String normalizedStrandName = strandName.toUpperCase().replaceAll("\\s+", " ").trim();
        
        List<Student> enrolledStudents = studentRepository.findAll().stream()
                .filter(student -> {
                    Boolean archived = student.getIsArchived();
                    if (archived != null && archived) {
                        return false;
                    }
                    String enrollmentStatus = student.getEnrollmentStatus();
                    if (enrollmentStatus == null || !"Enrolled".equals(enrollmentStatus.trim())) {
                        return false;
                    }
                    String studentStrand = student.getStrand();
                    if (studentStrand == null || studentStrand.trim().isEmpty()) {
                        return false;
                    }
                    // Normalize and compare
                    String normalizedStudentStrand = studentStrand.toUpperCase().replaceAll("\\s+", " ").trim();
                    return normalizedStudentStrand.equals(normalizedStrandName);
                })
                .toList();
        
        if (!enrolledStudents.isEmpty()) {
            throw new IllegalStateException("Cannot delete permanently strand " + strand.getName() + 
                    " because it has " + enrolledStudents.size() + " enrolled student(s). " +
                    "Please reassign or archive these students first.");
        }
        
        // Permanent delete - actually remove from database
        strandRepository.delete(strand);
    }
}

