package com.enrollment.system.service;

import com.enrollment.system.dto.SemesterDto;
import com.enrollment.system.model.Semester;
import com.enrollment.system.model.SchoolYear;
import com.enrollment.system.repository.SemesterRepository;
import com.enrollment.system.repository.SchoolYearRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SemesterService {
    
    @Autowired
    private SemesterRepository semesterRepository;
    
    @Autowired
    private SchoolYearRepository schoolYearRepository;
    
    @Transactional
    public List<SemesterDto> createSemestersForSchoolYear(Long schoolYearId) {
        SchoolYear schoolYear = schoolYearRepository.findById(schoolYearId)
                .orElseThrow(() -> new RuntimeException("School year not found with id: " + schoolYearId));
        
        List<SemesterDto> createdSemesters = new ArrayList<>();
        
        // Create 4 semesters: Grade 11 Sem 1, Grade 11 Sem 2, Grade 12 Sem 1, Grade 12 Sem 2
        int[] gradeLevels = {11, 12};
        int[] semesterNumbers = {1, 2};
        
        for (int gradeLevel : gradeLevels) {
            for (int semesterNumber : semesterNumbers) {
                // Check if semester already exists
                if (!semesterRepository.existsBySchoolYearIdAndGradeLevelAndSemesterNumber(
                        schoolYearId, gradeLevel, semesterNumber)) {
                    
                    Semester semester = new Semester();
                    semester.setSchoolYear(schoolYear);
                    semester.setGradeLevel(gradeLevel);
                    semester.setSemesterNumber(semesterNumber);
                    semester.setName("Grade " + gradeLevel + " - Semester " + semesterNumber);
                    semester.setIsActive(true);
                    
                    Semester saved = semesterRepository.save(semester);
                    createdSemesters.add(SemesterDto.fromSemester(saved));
                }
            }
        }
        
        return createdSemesters;
    }
    
    @Transactional(readOnly = true)
    public List<SemesterDto> getSemestersBySchoolYear(Long schoolYearId) {
        return semesterRepository.findBySchoolYearId(schoolYearId)
                .stream()
                .map(SemesterDto::fromSemester)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SemesterDto> getSemestersBySchoolYearAndGrade(Long schoolYearId, Integer gradeLevel) {
        return semesterRepository.findBySchoolYearIdAndGradeLevel(schoolYearId, gradeLevel)
                .stream()
                .map(SemesterDto::fromSemester)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SemesterDto> getAllSemestersForDropdown() {
        // Return ALL semesters from ALL school years (including non-current and inactive)
        // This is for Add Student page where users can select any school year
        // Order by school year start date DESC (newest first), then grade, then semester
        return semesterRepository.findAllOrderedBySchoolYearAndGrade()
                .stream()
                .map(SemesterDto::fromSemester)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SemesterDto> getSemestersForCurrentSchoolYear() {
        // Get semesters specifically for the current school year
        // First try the optimized query
        try {
            List<Semester> semesters = semesterRepository.findAllActiveForCurrentSchoolYear();
            if (!semesters.isEmpty()) {
                return semesters.stream()
                        .map(SemesterDto::fromSemester)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Warning: Error using optimized query for current school year semesters: " + e.getMessage());
        }
        
        // Fallback: Get current school year and then its semesters
        Optional<SchoolYear> currentSchoolYear = schoolYearRepository.findByIsCurrentTrue();
        if (currentSchoolYear.isPresent()) {
            Long currentSchoolYearId = currentSchoolYear.get().getId();
            List<Semester> semesters = semesterRepository.findBySchoolYearId(currentSchoolYearId);
            return semesters.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive())) // Only active semesters
                    .map(SemesterDto::fromSemester)
                    .collect(Collectors.toList());
        } else {
            System.err.println("Warning: No current school year found. Cannot load semesters for filter.");
        }
        return new ArrayList<>();
    }
    
    @Transactional(readOnly = true)
    public SemesterDto getSemesterById(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester not found with id: " + id));
        return SemesterDto.fromSemester(semester);
    }
    
    @Transactional(readOnly = true)
    public Semester getSemesterEntityById(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public Optional<Semester> findSemesterBySchoolYearAndGradeAndSemester(
            Long schoolYearId, Integer gradeLevel, Integer semesterNumber) {
        return semesterRepository.findBySchoolYearIdAndGradeLevelAndSemesterNumber(
                schoolYearId, gradeLevel, semesterNumber);
    }
    
    @Transactional
    public void ensureSemestersExistForAllSchoolYears() {
        // Get all school years
        List<SchoolYear> allSchoolYears = schoolYearRepository.findAll();
        
        for (SchoolYear schoolYear : allSchoolYears) {
            // Check if semesters exist for this school year
            List<Semester> existingSemesters = semesterRepository.findBySchoolYearId(schoolYear.getId());
            if (existingSemesters.isEmpty()) {
                // Create semesters for this school year
                try {
                    createSemestersForSchoolYear(schoolYear.getId());
                    System.out.println("✓ Created semesters for school year " + schoolYear.getYear());
                } catch (Exception e) {
                    System.err.println("⚠ Warning: Could not create semesters for " + schoolYear.getYear() + ": " + e.getMessage());
                }
            }
        }
    }
}

