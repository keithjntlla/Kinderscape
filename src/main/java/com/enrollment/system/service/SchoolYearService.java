package com.enrollment.system.service;

import com.enrollment.system.dto.SchoolYearDto;
import com.enrollment.system.model.SchoolYear;
import com.enrollment.system.repository.SchoolYearRepository;
import com.enrollment.system.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SchoolYearService {
    
    @Autowired
    private SchoolYearRepository schoolYearRepository;
    
    @Autowired(required = false)
    private StudentRepository studentRepository;
    
    @Autowired(required = false)
    private com.enrollment.system.service.SemesterService semesterService;
    
    @Autowired(required = false)
    private com.enrollment.system.repository.SemesterRepository semesterRepository;
    
    @Transactional(readOnly = true)
    public SchoolYearDto getCurrentSchoolYear() {
        SchoolYear current = schoolYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("No current school year is set. Please set a school year as current."));
        return SchoolYearDto.fromSchoolYear(current);
    }
    
    @Transactional(readOnly = true)
    public SchoolYear getCurrentSchoolYearEntity() {
        return schoolYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new RuntimeException("No current school year is set. Please set a school year as current."));
    }
    
    @Transactional
    public SchoolYearDto createSchoolYear(String year, LocalDate startDate, LocalDate endDate) {
        // Validate year format (basic validation)
        if (year == null || year.trim().isEmpty()) {
            throw new RuntimeException("School year cannot be empty");
        }
        
        // Check if year already exists
        if (schoolYearRepository.existsByYear(year.trim())) {
            throw new RuntimeException("School year " + year + " already exists");
        }
        
        // Validate dates
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Start date and end date are required");
        }
        
        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new RuntimeException("End date must be after start date");
        }
        
        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setYear(year.trim());
        schoolYear.setStartDate(startDate);
        schoolYear.setEndDate(endDate);
        schoolYear.setIsCurrent(false); // New school years are not current by default
        
        SchoolYear saved = schoolYearRepository.save(schoolYear);
        
        // Auto-create semesters for the new school year
        if (semesterService != null) {
            try {
                semesterService.createSemestersForSchoolYear(saved.getId());
            } catch (Exception e) {
                // Log error but don't fail school year creation
                System.err.println("Warning: Could not create semesters for school year: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return SchoolYearDto.fromSchoolYear(saved);
    }
    
    @Transactional
    public SchoolYearDto setCurrentSchoolYear(Long schoolYearId) {
        // Unset all current school years
        List<SchoolYear> allCurrent = schoolYearRepository.findAll().stream()
                .filter(sy -> Boolean.TRUE.equals(sy.getIsCurrent()))
                .collect(Collectors.toList());
        
        for (SchoolYear sy : allCurrent) {
            sy.setIsCurrent(false);
            schoolYearRepository.save(sy);
        }
        
        // Set the new current school year
        SchoolYear schoolYear = schoolYearRepository.findById(schoolYearId)
                .orElseThrow(() -> new RuntimeException("School year not found with id: " + schoolYearId));
        
        schoolYear.setIsCurrent(true);
        SchoolYear saved = schoolYearRepository.save(schoolYear);
        return SchoolYearDto.fromSchoolYear(saved);
    }
    
    @Transactional(readOnly = true)
    public List<SchoolYearDto> getAllSchoolYears() {
        return schoolYearRepository.findAllByOrderByStartDateDesc()
                .stream()
                .map(SchoolYearDto::fromSchoolYear)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SchoolYearDto getSchoolYearById(Long id) {
        SchoolYear schoolYear = schoolYearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("School year not found with id: " + id));
        return SchoolYearDto.fromSchoolYear(schoolYear);
    }
    
    @Transactional
    public SchoolYearDto updateSchoolYear(Long id, SchoolYearDto dto) {
        SchoolYear schoolYear = schoolYearRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("School year not found with id: " + id));
        
        // Don't allow changing isCurrent through update - use setCurrentSchoolYear instead
        if (dto.getYear() != null && !dto.getYear().equals(schoolYear.getYear())) {
            // Check if new year already exists
            if (schoolYearRepository.existsByYear(dto.getYear().trim())) {
                throw new RuntimeException("School year " + dto.getYear() + " already exists");
            }
            schoolYear.setYear(dto.getYear().trim());
        }
        
        if (dto.getStartDate() != null) {
            schoolYear.setStartDate(dto.getStartDate());
        }
        
        if (dto.getEndDate() != null) {
            schoolYear.setEndDate(dto.getEndDate());
        }
        
        // Validate dates
        if (schoolYear.getEndDate().isBefore(schoolYear.getStartDate()) || 
            schoolYear.getEndDate().isEqual(schoolYear.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }
        
        SchoolYear saved = schoolYearRepository.save(schoolYear);
        return SchoolYearDto.fromSchoolYear(saved);
    }
    
    @Transactional(readOnly = true)
    public SchoolYearDto getSchoolYearByYear(String year) {
        SchoolYear schoolYear = schoolYearRepository.findByYear(year)
                .orElseThrow(() -> new RuntimeException("School year not found: " + year));
        return SchoolYearDto.fromSchoolYear(schoolYear);
    }
    
    @Transactional(readOnly = true)
    public SchoolYear getPreviousSchoolYearEntity() {
        SchoolYear currentSchoolYear = getCurrentSchoolYearEntity();
        
        // Get all school years ordered by start date descending
        List<SchoolYear> allSchoolYears = schoolYearRepository.findAllByOrderByStartDateDesc();
        
        // Find the school year that ended just before the current one started
        for (SchoolYear sy : allSchoolYears) {
            if (sy.getEndDate().isBefore(currentSchoolYear.getStartDate()) || 
                sy.getEndDate().isEqual(currentSchoolYear.getStartDate().minusDays(1))) {
                return sy;
            }
        }
        
        // If no previous school year found, return null
        return null;
    }
    
    @Transactional
    public void deleteSchoolYear(Long schoolYearId) {
        SchoolYear schoolYear = schoolYearRepository.findById(schoolYearId)
                .orElseThrow(() -> new RuntimeException("School year not found with id: " + schoolYearId));
        
        // Cannot delete current school year
        if (Boolean.TRUE.equals(schoolYear.getIsCurrent())) {
            throw new RuntimeException("Cannot delete the current school year. Please set another school year as current first.");
        }
        
        // Check student count - can only delete if less than 2 students
        if (studentRepository != null) {
            long studentCount = studentRepository.findAll().stream()
                    .filter(student -> {
                        if (student.getSchoolYear() == null) {
                            return false;
                        }
                        return student.getSchoolYear().getId().equals(schoolYearId);
                    })
                    .count();
            
            if (studentCount >= 2) {
                throw new RuntimeException("Cannot delete school year " + schoolYear.getYear() + 
                        ". It has " + studentCount + " student(s). School years can only be deleted if they have less than 2 students.");
            }
        }
        
        // Delete all semesters associated with this school year
        if (semesterRepository != null) {
            try {
                List<com.enrollment.system.model.Semester> semesters = semesterRepository.findBySchoolYearId(schoolYearId);
                for (com.enrollment.system.model.Semester semester : semesters) {
                    semesterRepository.delete(semester);
                }
            } catch (Exception e) {
                // Log but don't fail deletion if semester repository is not available
                System.err.println("Warning: Could not delete semesters: " + e.getMessage());
            }
        }
        
        // Delete the school year
        schoolYearRepository.delete(schoolYear);
    }
}

