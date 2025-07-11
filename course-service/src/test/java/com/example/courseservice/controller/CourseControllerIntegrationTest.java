package com.example.courseservice.controller;

import com.example.courseservice.dto.CourseRequest;
import com.example.courseservice.model.Course;
import com.example.courseservice.repository.CourseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void setUp() {
        // Ensure a clean state before each test method
        courseRepository.deleteAll();
    }

    @Test
    void createCourse_shouldReturnCreatedCourse() throws Exception {
        CourseRequest courseRequest = new CourseRequest("Spring Boot Basics", "Learn the fundamentals of Spring Boot.");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Spring Boot Basics"))
                .andExpect(jsonPath("$.description").value("Learn the fundamentals of Spring Boot."));

        assertThat(courseRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findByTitle("Spring Boot Basics")).isPresent();
    }



    @Test
    void getAllCourses_shouldReturnEmptyList_whenNoCourses() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllCourses_shouldReturnCourses_whenCoursesExist() throws Exception {
        Course course1 = new Course(null, "Java Fundamentals", "Deep dive into Java basics.");
        Course course2 = new Course(null, "Microservices with Spring", "Building distributed systems.");
        courseRepository.save(course1);
        courseRepository.save(course2);

        mockMvc.perform(get("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Java Fundamentals"))
                .andExpect(jsonPath("$[1].title").value("Microservices with Spring"));
    }

    @Test
    void getCourseById_shouldReturnCourse_whenExists() throws Exception {
        Course course = new Course(null, "REST API Design", "Principles of RESTful APIs.");
        Course savedCourse = courseRepository.save(course);

        mockMvc.perform(get("/api/courses/{id}", savedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedCourse.getId()))
                .andExpect(jsonPath("$.title").value("REST API Design"));
    }

    @Test
    void getCourseById_shouldReturnNotFound_whenNotExists() throws Exception {
        Long nonExistentId = 99L;
        mockMvc.perform(get("/api/courses/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Course not found with id: " + nonExistentId));
    }

    @Test
    void updateCourse_shouldReturnUpdatedCourse_whenExists() throws Exception {
        Course course = new Course(null, "Old Title", "Old Description with enough length.");
        Course savedCourse = courseRepository.save(course);

        CourseRequest updatedCourseRequest = new CourseRequest("New Title", "New Description with enough length.");

        mockMvc.perform(put("/api/courses/{id}", savedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCourseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedCourse.getId()))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.description").value("New Description with enough length."));

        assertThat(courseRepository.findById(savedCourse.getId()).get().getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateCourse_shouldReturnNotFound_whenNotExists() throws Exception {
        Long nonExistentId = 99L;
        CourseRequest updatedCourseRequest = new CourseRequest("Title", "Description with enough length.");

        mockMvc.perform(put("/api/courses/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCourseRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Course not found with id: " + nonExistentId));
    }

    @Test
    void deleteCourse_shouldReturnNoContent_whenExists() throws Exception {
        Course course = new Course(null, "Course to Delete", "Description to delete with enough length.");
        Course savedCourse = courseRepository.save(course);

        mockMvc.perform(delete("/api/courses/{id}", savedCourse.getId()))
                .andExpect(status().isNoContent());

        assertThat(courseRepository.findById(savedCourse.getId())).isEmpty();
    }

    @Test
    void deleteCourse_shouldReturnNotFound_whenNotExists() throws Exception {
        Long nonExistentId = 99L;
        mockMvc.perform(delete("/api/courses/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Course not found with id: " + nonExistentId));
    }
}