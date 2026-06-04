package com.enrollment.system.repository;

import com.enrollment.system.model.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    
    @Query("SELECT ta FROM TeacherAssignment ta " +
           "LEFT JOIN FETCH ta.subject " +
           "LEFT JOIN FETCH ta.section " +
           "WHERE ta.teacher.id = :teacherId")
    List<TeacherAssignment> findByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT ta FROM TeacherAssignment ta " +
           "LEFT JOIN FETCH ta.subject " +
           "LEFT JOIN FETCH ta.section " +
           "WHERE ta.teacher.id = :teacherId AND ta.subject.id = :subjectId")
    List<TeacherAssignment> findByTeacherIdAndSubjectId(@Param("teacherId") Long teacherId, @Param("subjectId") Long subjectId);
    
    List<TeacherAssignment> findBySubjectId(Long subjectId);
    
    List<TeacherAssignment> findBySectionId(Long sectionId);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.teacher.id = :teacherId AND ta.subject.id = :subjectId AND ta.section.id = :sectionId")
    Optional<TeacherAssignment> findByTeacherAndSubjectAndSection(
        @Param("teacherId") Long teacherId,
        @Param("subjectId") Long subjectId,
        @Param("sectionId") Long sectionId
    );
    
    void deleteByTeacherId(Long teacherId);
    
    void deleteByTeacherIdAndSubjectId(Long teacherId, Long subjectId);
    
    void deleteByTeacherIdAndSubjectIdAndSectionId(Long teacherId, Long subjectId, Long sectionId);
    
    @Query("SELECT COUNT(DISTINCT ta.subject.id) FROM TeacherAssignment ta WHERE ta.teacher.id = :teacherId")
    long countDistinctSubjectsByTeacherId(@Param("teacherId") Long teacherId);
    
    @Query("SELECT COUNT(ta) FROM TeacherAssignment ta WHERE ta.teacher.id = :teacherId")
    long countTotalAssignmentsByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * Finds a teacher assignment for a specific subject-section combination, excluding a specific teacher.
     * Used to check if the same subject-section is already assigned to a different teacher.
     * 
     * @param subjectId The subject ID
     * @param sectionId The section ID
     * @param excludeTeacherId The teacher ID to exclude from the search
     * @return Optional TeacherAssignment if found, empty if not found
     */
    @Query("SELECT ta FROM TeacherAssignment ta " +
           "LEFT JOIN FETCH ta.teacher " +
           "LEFT JOIN FETCH ta.subject " +
           "LEFT JOIN FETCH ta.section " +
           "WHERE ta.subject.id = :subjectId AND ta.section.id = :sectionId AND ta.teacher.id != :excludeTeacherId")
    Optional<TeacherAssignment> findBySubjectIdAndSectionIdExcludingTeacher(
        @Param("subjectId") Long subjectId,
        @Param("sectionId") Long sectionId,
        @Param("excludeTeacherId") Long excludeTeacherId
    );
}
