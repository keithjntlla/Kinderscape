package com.enrollment.system.repository;

import com.enrollment.system.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    List<Student> findAllByOrderByNameAsc();
    
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.section LEFT JOIN FETCH s.schoolYear LEFT JOIN FETCH s.semester ORDER BY s.name ASC")
    List<Student> findAllWithSectionByOrderByNameAsc();
    
    Optional<Student> findByLrn(String lrn);
    
    boolean existsByLrn(String lrn);
    
    List<Student> findByGradeLevel(Integer gradeLevel);
    
    List<Student> findByStrand(String strand);
    
    List<Student> findByEnrollmentStatus(String enrollmentStatus);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.section.id = :sectionId AND s.enrollmentStatus = 'Enrolled' AND (s.isArchived IS NULL OR s.isArchived = false)")
    long countBySectionIdAndEnrolled(Long sectionId);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.section.id = :sectionId AND s.semester.id = :semesterId AND s.enrollmentStatus = 'Enrolled' AND (s.isArchived IS NULL OR s.isArchived = false)")
    long countBySectionIdAndSemesterIdAndEnrolled(Long sectionId, Long semesterId);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.section.id = :sectionId AND s.semester.id = :semesterId AND s.enrollmentStatus = 'Enrolled' AND (s.isArchived IS NULL OR s.isArchived = false) AND s.gradeLevel = :gradeLevel AND s.strand = :strand")
    long countBySectionIdAndSemesterIdAndGradeLevelAndStrandAndEnrolled(Long sectionId, Long semesterId, Integer gradeLevel, String strand);
    
    Optional<Student> findByNameIgnoreCase(String name);
    
    boolean existsByNameIgnoreCase(String name);
    
    @Query("SELECT s FROM Student s WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name)) AND s.id != :excludeId")
    Optional<Student> findByNameIgnoreCaseExcludingId(String name, Long excludeId);
    
    @Query("SELECT COUNT(s) > 0 FROM Student s WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name)) AND s.id != :excludeId")
    boolean existsByNameIgnoreCaseExcludingId(String name, Long excludeId);
}
