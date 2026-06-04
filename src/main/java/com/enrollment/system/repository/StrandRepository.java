package com.enrollment.system.repository;

import com.enrollment.system.model.Strand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StrandRepository extends JpaRepository<Strand, Long> {
    
    Optional<Strand> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Strand> findByIsActiveTrue();
    
    List<Strand> findAllByOrderByNameAsc();
}

