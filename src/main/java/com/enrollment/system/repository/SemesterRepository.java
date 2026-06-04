package com.enrollment.system.repository;

import com.enrollment.system.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    
    @Query("SELECT s FROM Semester s LEFT JOIN FETCH s.schoolYear sy WHERE sy.id = :schoolYearId")
    List<Semester> findBySchoolYearId(@Param("schoolYearId") Long schoolYearId);
    
    List<Semester> findBySchoolYearIdAndGradeLevel(Long schoolYearId, Integer gradeLevel);
    
    Optional<Semester> findBySchoolYearIdAndGradeLevelAndSemesterNumber(
        Long schoolYearId, Integer gradeLevel, Integer semesterNumber);
    
    boolean existsBySchoolYearIdAndGradeLevelAndSemesterNumber(
        Long schoolYearId, Integer gradeLevel, Integer semesterNumber);
    
    @Query("SELECT s FROM Semester s LEFT JOIN FETCH s.schoolYear sy " +
           "WHERE s.isActive = true " +
           "ORDER BY sy.startDate DESC, s.gradeLevel ASC, s.semesterNumber ASC")
    List<Semester> findAllActiveOrderedBySchoolYearAndGrade();
    
    @Query("SELECT s FROM Semester s LEFT JOIN FETCH s.schoolYear sy " +
           "ORDER BY sy.startDate DESC, s.gradeLevel ASC, s.semesterNumber ASC")
    List<Semester> findAllOrderedBySchoolYearAndGrade();
    
    @Query("SELECT s FROM Semester s LEFT JOIN FETCH s.schoolYear sy " +
           "WHERE sy.isCurrent = true AND s.isActive = true " +
           "ORDER BY s.gradeLevel ASC, s.semesterNumber ASC")
    List<Semester> findAllActiveForCurrentSchoolYear();
}

