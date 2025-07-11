package com.example.courseservice.controller;

import com.example.courseservice.dto.CourseRequest;
import com.example.courseservice.dto.CourseResponse;
import com.example.courseservice.dto.CourseValidationRequest; // NEW IMPORT
import com.example.courseservice.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/byIds")
    public ResponseEntity<List<CourseResponse>> getCoursesByIds(@RequestBody CourseValidationRequest request) { // MODIFIED LINE
        List<CourseResponse> courses = courseService.getCoursesByIds(request.getCourseIds()); // MODIFIED LINE
        return new ResponseEntity<>(courses, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest courseRequest) {
        CourseResponse createdCourse = courseService.createCourse(courseRequest);
        return new ResponseEntity<>(createdCourse, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest courseRequest) {
        CourseResponse updatedCourse = courseService.updateCourse(id, courseRequest);
        return ResponseEntity.ok(updatedCourse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}