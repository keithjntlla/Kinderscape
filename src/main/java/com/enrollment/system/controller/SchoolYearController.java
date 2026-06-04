package com.enrollment.system.controller;

import com.enrollment.system.dto.SchoolYearDto;
import com.enrollment.system.service.SchoolYearService;
import com.enrollment.system.service.SchoolYearTransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/school-years")
@CrossOrigin(origins = "*")
public class SchoolYearController {
    
    @Autowired
    private SchoolYearService schoolYearService;
    
    @Autowired
    private SchoolYearTransitionService transitionService;
    
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSchoolYear() {
        try {
            SchoolYearDto current = schoolYearService.getCurrentSchoolYear();
            return ResponseEntity.ok(current);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<SchoolYearDto>> getAllSchoolYears() {
        List<SchoolYearDto> schoolYears = schoolYearService.getAllSchoolYears();
        return ResponseEntity.ok(schoolYears);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getSchoolYearById(@PathVariable Long id) {
        try {
            SchoolYearDto schoolYear = schoolYearService.getSchoolYearById(id);
            return ResponseEntity.ok(schoolYear);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createSchoolYear(@RequestBody SchoolYearDto dto) {
        try {
            SchoolYearDto created = schoolYearService.createSchoolYear(
                dto.getYear(),
                dto.getStartDate(),
                dto.getEndDate()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchoolYear(@PathVariable Long id, @RequestBody SchoolYearDto dto) {
        try {
            SchoolYearDto updated = schoolYearService.updateSchoolYear(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PutMapping("/{id}/set-current")
    public ResponseEntity<?> setCurrentSchoolYear(@PathVariable Long id) {
        try {
            SchoolYearDto current = schoolYearService.setCurrentSchoolYear(id);
            return ResponseEntity.ok(current);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/transition")
    public ResponseEntity<?> transitionToNewSchoolYear(@RequestBody Map<String, Object> request) {
        try {
            Long newSchoolYearId = Long.valueOf(request.get("newSchoolYearId").toString());
            boolean carryOverEnrolled = Boolean.parseBoolean(request.getOrDefault("carryOverEnrolled", true).toString());
            boolean carryOverPending = Boolean.parseBoolean(request.getOrDefault("carryOverPending", true).toString());
            
            SchoolYearTransitionService.TransitionResult result = 
                transitionService.transitionToNewSchoolYear(newSchoolYearId, carryOverEnrolled, carryOverPending);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("enrolledCarriedOver", result.getEnrolledCarriedOver());
            response.put("pendingCarriedOver", result.getPendingCarriedOver());
            response.put("skipped", result.getSkipped());
            response.put("totalCarriedOver", result.getTotalCarriedOver());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/transition/preview")
    public ResponseEntity<?> getTransitionPreview(
            @RequestParam Long newSchoolYearId,
            @RequestParam(defaultValue = "true") boolean carryOverEnrolled,
            @RequestParam(defaultValue = "true") boolean carryOverPending) {
        try {
            SchoolYearTransitionService.TransitionPreview preview = 
                transitionService.getTransitionPreview(newSchoolYearId, carryOverEnrolled, carryOverPending);
            
            Map<String, Object> response = new HashMap<>();
            response.put("enrolledCount", preview.getEnrolledCount());
            response.put("pendingCount", preview.getPendingCount());
            response.put("skippedCount", preview.getSkippedCount());
            response.put("totalCount", preview.getTotalCount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

