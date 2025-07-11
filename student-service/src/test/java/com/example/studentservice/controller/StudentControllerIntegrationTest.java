package com.example.studentservice.controller;

import com.example.studentservice.dto.CourseDto; // Import CourseDto
import com.example.studentservice.dto.StudentRequest;
import com.example.studentservice.dto.StudentResponse;
import com.example.studentservice.exception.ResourceNotFoundException;
import com.example.studentservice.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections; // For Collections.emptySet()
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class) // Focuses on StudentController
public class StudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // For making HTTP requests

    @MockBean // Mocks the service layer, isolating the controller
    private StudentService studentService;

    @Autowired
    private ObjectMapper objectMapper; // For JSON serialization/deserialization

    private StudentResponse studentResponse1;
    private StudentResponse studentResponse2;
    private StudentRequest studentRequest;

    @BeforeEach
    void setUp() {
        // Corrected: Pass an empty Set<CourseDto> for courses
        studentResponse1 = new StudentResponse(1L, "Alice", "Smith", "alice@example.com", Collections.emptySet());
        studentResponse2 = new StudentResponse(2L, "Bob", "Johnson", "bob@example.com", Collections.emptySet());
        studentRequest = new StudentRequest("New", "Student", "new.student@example.com", Set.of(101L, 102L));
    }

    @Test
    void getAllStudents_shouldReturnListOfStudents() throws Exception {
        List<StudentResponse> allStudents = Arrays.asList(studentResponse1, studentResponse2);
        when(studentService.getAllStudents()).thenReturn(allStudents);

        mockMvc.perform(get("/api/students")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName", is("Alice")))
                .andExpect(jsonPath("$[1].lastName", is("Johnson")));

        verify(studentService, times(1)).getAllStudents();
    }

    @Test
    void getStudentById_shouldReturnStudent_whenExists() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(studentResponse1);

        mockMvc.perform(get("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstName", is("Alice")));

        verify(studentService, times(1)).getStudentById(1L);
    }

    @Test
    void getStudentById_shouldReturnNotFound_whenNotExists() throws Exception {
        when(studentService.getStudentById(99L)).thenThrow(new ResourceNotFoundException("Student not found"));

        mockMvc.perform(get("/api/students/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                // UPDATED: Expect JSON response
                .andExpect(jsonPath("$.message", is("Student not found")));

        verify(studentService, times(1)).getStudentById(99L);
    }

    @Test
    void createStudent_shouldReturnCreatedStudent() throws Exception {
        // Corrected: Pass an empty Set<CourseDto> for courses in the expected response
        StudentResponse createdResponse = new StudentResponse(3L, studentRequest.getFirstName(), studentRequest.getLastName(), studentRequest.getEmail(), Collections.emptySet());
        when(studentService.createStudent(any(StudentRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.email", is(studentRequest.getEmail())));

        verify(studentService, times(1)).createStudent(any(StudentRequest.class));
    }

    @Test
    void createStudent_shouldReturnBadRequest_whenValidationFails() throws Exception {
        StudentRequest invalidRequest = new StudentRequest("A", "B", "invalid-email", Set.of()); // Invalid first name, last name, email
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                // UPDATED: Expect JSON structure for validation errors
                .andExpect(jsonPath("$.errors.firstName", is("First name must be between 2 and 50 characters")))
                .andExpect(jsonPath("$.errors.lastName", is("Last name must be between 2 and 50 characters"))) // Added this as it's also invalid
                .andExpect(jsonPath("$.errors.email", is("Email should be valid")));
    }

    @Test
    void createStudent_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        when(studentService.createStudent(any(StudentRequest.class)))
                .thenThrow(new IllegalArgumentException("Student with email " + studentRequest.getEmail() + " already exists."));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isBadRequest()) // Or HttpStatus.CONFLICT if you map IllegalArgumentException to 409
                // UPDATED: Expect JSON response
                .andExpect(jsonPath("$.message", is("Student with email " + studentRequest.getEmail() + " already exists.")));

        verify(studentService, times(1)).createStudent(any(StudentRequest.class));
    }

    @Test
    void updateStudent_shouldReturnUpdatedStudent_whenExists() throws Exception {
        StudentRequest updatedRequest = new StudentRequest("Updated", "Name", "updated@example.com", Set.of(201L, 202L));
        // Corrected: Pass an empty Set<CourseDto> for courses in the expected response
        StudentResponse updatedResponse = new StudentResponse(1L, "Updated", "Name", "updated@example.com", Collections.emptySet());

        when(studentService.updateStudent(eq(1L), any(StudentRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.email", is("updated@example.com")));

        verify(studentService, times(1)).updateStudent(eq(1L), any(StudentRequest.class));
    }

    @Test
    void updateStudent_shouldReturnNotFound_whenNotExists() throws Exception {
        StudentRequest updatedRequest = new StudentRequest("Updated", "Name", "updated@example.com", Set.of());
        when(studentService.updateStudent(eq(99L), any(StudentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Student not found"));

        mockMvc.perform(put("/api/students/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRequest)))
                .andExpect(status().isNotFound())
                // UPDATED: Expect JSON response
                .andExpect(jsonPath("$.message", is("Student not found")));

        verify(studentService, times(1)).updateStudent(eq(99L), any(StudentRequest.class));
    }

    @Test
    void deleteStudent_shouldReturnNoContent_whenExists() throws Exception {
        doNothing().when(studentService).deleteStudent(1L); // Mock void method

        mockMvc.perform(delete("/api/students/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(studentService, times(1)).deleteStudent(1L);
    }

    @Test
    void deleteStudent_shouldReturnNotFound_whenNotExists() throws Exception {
        doThrow(new ResourceNotFoundException("Student not found"))
                .when(studentService).deleteStudent(99L);

        mockMvc.perform(delete("/api/students/{id}", 99L))
                .andExpect(status().isNotFound())
                // UPDATED: Expect JSON response
                .andExpect(jsonPath("$.message", is("Student not found")));

        verify(studentService, times(1)).deleteStudent(99L);
    }
}