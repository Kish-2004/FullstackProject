// src/test/java/com/example/studentservice/service/StudentServiceInterServiceTest.java
package com.example.studentservice.service;

import com.example.studentservice.dto.CourseDto;
import com.example.studentservice.dto.StudentRequest;
import com.example.studentservice.dto.StudentResponse;
import com.example.studentservice.exception.ResourceNotFoundException;
import com.example.studentservice.model.Student;
import com.example.studentservice.repository.StudentRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyLong;

@SpringBootTest
public class StudentServiceInterServiceTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @MockBean
    private StudentRepository studentRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ModelMapper modelMapper;

    private StudentServiceImpl studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl(studentRepository, modelMapper, restTemplate);
    }

    @Test
    void getStudentById_shouldFetchCourses_whenCourseIdsExist() throws Exception {
        Long studentId = 1L;
        Set<Long> courseIds = Set.of(101L, 102L);
        Student student = new Student(studentId, "John", "Doe", "john@example.com", courseIds);

        CourseDto course101 = new CourseDto(101L, "Math Basics", "Introductory math course");
        CourseDto course102 = new CourseDto(102L, "Physics Fun", "Fun with physics");
        List<CourseDto> mockCourses = Arrays.asList(course101, course102);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // CORRECTED: Mock postForEntity for fetchCoursesByIds
        when(restTemplate.postForEntity(
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class), // Use any(HttpEntity.class) for the request body
                eq(CourseDto[].class)
        )).thenReturn(new ResponseEntity<>(mockCourses.toArray(new CourseDto[0]), HttpStatus.OK));

        StudentResponse response = studentService.getStudentById(studentId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(studentId);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getCourses()).containsExactlyInAnyOrderElementsOf(mockCourses);

        verify(restTemplate, times(1)).postForEntity( // Changed to postForEntity
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class), // Changed to any(HttpEntity.class)
                eq(CourseDto[].class)
        );
        verify(studentRepository, times(1)).findById(studentId);
    }

    @Test
    void createStudent_shouldValidateCoursesExist_whenCourseIdsProvided() throws Exception {
        Set<Long> courseIds = Set.of(101L, 102L);
        StudentRequest studentRequest = new StudentRequest("Jane", "Doe", "jane@example.com", courseIds);
        Student savedStudent = new Student(2L, "Jane", "Doe", "jane@example.com", courseIds);

        CourseDto course101 = new CourseDto(101L, "Math Basics", "Introductory math course");
        CourseDto course102 = new CourseDto(102L, "Physics Fun", "Fun with physics");
        List<CourseDto> mockCourses = Arrays.asList(course101, course102);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        // CORRECTED: Mock postForEntity for validateCoursesExist and fetchCoursesByIds
        when(restTemplate.postForEntity(
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class),
                eq(CourseDto[].class)
        )).thenReturn(new ResponseEntity<>(mockCourses.toArray(new CourseDto[0]), HttpStatus.OK));


        StudentResponse response = studentService.createStudent(studentRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getCourses()).containsExactlyInAnyOrderElementsOf(mockCourses);

        // Verify postForEntity is called twice (once for validation, once for fetching)
        verify(restTemplate, times(2)).postForEntity(
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class),
                eq(CourseDto[].class)
        );
        verify(studentRepository, times(1)).findByEmail(studentRequest.getEmail());
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    void createStudent_shouldThrowException_whenCourseIdsAreInvalid() throws Exception {
        Set<Long> requestedCourseIds = Set.of(101L, 999L);
        StudentRequest studentRequest = new StudentRequest("Jane", "Doe", "jane@example.com", requestedCourseIds);

        CourseDto course101 = new CourseDto(101L, "Math Basics", "Introductory math course");
        List<CourseDto> mockValidCoursesReturnedByCourseService = Collections.singletonList(course101);

        when(studentRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // CORRECTED: Mock postForEntity
        when(restTemplate.postForEntity(
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class),
                eq(CourseDto[].class)
        )).thenReturn(new ResponseEntity<>(mockValidCoursesReturnedByCourseService.toArray(new CourseDto[0]), HttpStatus.OK));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                studentService.createStudent(studentRequest)
        );

        assertThat(thrown.getMessage()).contains("One or more courses with IDs [999] do not exist."); // Ensure exact message matches
        verify(studentRepository, never()).save(any(Student.class));

        verify(restTemplate, times(1)).postForEntity( // Changed to postForEntity
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class), // Changed to any(HttpEntity.class)
                eq(CourseDto[].class)
        );
        verify(studentRepository, times(1)).findByEmail(studentRequest.getEmail());
    }

    @Test
    void getStudentById_shouldHandleCourseServiceDown() throws Exception {
        Long studentId = 1L;
        Set<Long> courseIds = Set.of(101L);
        Student student = new Student(studentId, "John", "Doe", "john@example.com", courseIds);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // Simulate the Course Service throwing an exception (e.g., connection refused, 500 error)
        // CORRECTED: Mock postForEntity
        when(restTemplate.postForEntity(
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class),
                eq(CourseDto[].class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated 500 from Course Service"));

        StudentResponse response = studentService.getStudentById(studentId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(studentId);
        // Assert that courses are empty because the service handled the external API error gracefully
        assertThat(response.getCourses()).isEmpty();

        verify(restTemplate, times(1)).postForEntity( // Changed to postForEntity
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class), // Changed to any(HttpEntity.class)
                eq(CourseDto[].class)
        );
        verify(studentRepository, times(1)).findById(studentId);
    }

    @Test
    void createStudent_shouldThrowException_whenEmailAlreadyExists() {
        StudentRequest studentRequest = new StudentRequest("John", "Doe", "john.doe@example.com", Set.of(1L));
        when(studentRepository.findByEmail(studentRequest.getEmail())).thenReturn(Optional.of(new Student()));

        assertThrows(IllegalArgumentException.class, () ->
                studentService.createStudent(studentRequest)
        );
        verify(studentRepository, never()).save(any(Student.class));
        verify(studentRepository, times(1)).findByEmail(studentRequest.getEmail());
    }

    @Test
    void updateStudent_shouldUpdateStudentAndValidateCourses() throws Exception {
        Long studentId = 1L;
        Set<Long> oldCourseIds = Set.of(101L);
        Set<Long> newCourseIds = Set.of(102L, 103L);
        Student existingStudent = new Student(studentId, "Old", "Name", "old@example.com", oldCourseIds);
        StudentRequest updateRequest = new StudentRequest("New", "Name", "new@example.com", newCourseIds);
        Student updatedStudent = new Student(studentId, "New", "Name", "new@example.com", newCourseIds);

        CourseDto course102 = new CourseDto(102L, "Chemistry", "Basic Chemistry");
        CourseDto course103 = new CourseDto(103L, "Biology", "Introduction to Biology");
        List<CourseDto> mockValidCourses = Arrays.asList(course102, course103);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.findByEmail(updateRequest.getEmail())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenReturn(updatedStudent);

        // CORRECTED: Mock postForEntity (will be called twice: validate and fetch)
        when(restTemplate.postForEntity(
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class),
                eq(CourseDto[].class)
        )).thenReturn(new ResponseEntity<>(mockValidCourses.toArray(new CourseDto[0]), HttpStatus.OK));

        StudentResponse response = studentService.updateStudent(studentId, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(studentId);
        assertThat(response.getFirstName()).isEqualTo("New");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getCourses()).containsExactlyInAnyOrderElementsOf(mockValidCourses);

        verify(studentRepository, times(1)).findById(studentId);
        verify(studentRepository, times(1)).findByEmail(updateRequest.getEmail());
        verify(studentRepository, times(1)).save(any(Student.class));
        verify(restTemplate, times(2)).postForEntity( // Changed to postForEntity, and expecting 2 calls
                eq("http://COURSE-SERVICE/api/courses/byIds"),
                any(HttpEntity.class),
                eq(CourseDto[].class)
        );
    }

    @Test
    void updateStudent_shouldThrowException_whenStudentNotFound() {
        Long nonExistentId = 99L;
        StudentRequest updateRequest = new StudentRequest("Non", "Existent", "non@example.com", Set.of(1L));

        when(studentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                studentService.updateStudent(nonExistentId, updateRequest)
        );
        verify(studentRepository, never()).save(any(Student.class));
        verify(studentRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void deleteStudent_shouldDeleteStudent() {
        Long studentId = 1L;
        when(studentRepository.existsById(studentId)).thenReturn(true);
        doNothing().when(studentRepository).deleteById(studentId);

        studentService.deleteStudent(studentId);

        verify(studentRepository, times(1)).deleteById(studentId);
        verify(studentRepository, times(1)).existsById(studentId);
    }

    @Test
    void deleteStudent_shouldThrowException_whenStudentNotFound() {
        Long nonExistentId = 99L;
        when(studentRepository.existsById(nonExistentId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                studentService.deleteStudent(nonExistentId)
        );
        verify(studentRepository, never()).deleteById(anyLong());
        verify(studentRepository, times(1)).existsById(nonExistentId);
    }
}