package com.enrollment.system.repository;

import com.enrollment.system.model.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long> {
    
    Optional<SchoolYear> findByIsCurrentTrue();
    
    Optional<SchoolYear> findByYear(String year);
    
    List<SchoolYear> findAllByOrderByStartDateDesc();
    
    boolean existsByYear(String year);
    
    @Query("SELECT sy FROM SchoolYear sy WHERE sy.isCurrent = true")
    Optional<SchoolYear> findCurrentSchoolYear();
}

