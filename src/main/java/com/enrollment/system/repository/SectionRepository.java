package com.enrollment.system.repository;

import com.enrollment.system.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    
    List<Section> findByStrand(String strand);
    
    List<Section> findByStrandAndGradeLevel(String strand, Integer gradeLevel);
    
    List<Section> findByStrandAndGradeLevelAndIsActiveTrue(String strand, Integer gradeLevel);
    
    List<Section> findByStrandAndIsActiveTrue(String strand);
    
    List<Section> findByIsActiveTrue();
    
    Optional<Section> findByNameAndStrandAndGradeLevel(String name, String strand, Integer gradeLevel);
    
    boolean existsByNameAndStrandAndGradeLevel(String name, String strand, Integer gradeLevel);
}

