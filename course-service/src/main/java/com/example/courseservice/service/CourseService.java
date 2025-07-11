package com.example.courseservice.service;

import com.example.courseservice.dto.CourseRequest;
import com.example.courseservice.dto.CourseResponse;

import java.util.List;
import java.util.Set;

public interface CourseService {
    List<CourseResponse> getAllCourses();
    CourseResponse getCourseById(Long id);
    List<CourseResponse> getCoursesByIds(Set<Long> courseIds);
    CourseResponse createCourse(CourseRequest courseRequest);
    CourseResponse updateCourse(Long id, CourseRequest courseRequest);
    void deleteCourse(Long id);
}