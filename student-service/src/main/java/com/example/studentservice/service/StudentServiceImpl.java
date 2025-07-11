// src/main/java/com/example/studentservice/service/StudentServiceImpl.java
package com.example.studentservice.service;

import com.example.studentservice.dto.CourseDto;
import com.example.studentservice.dto.StudentRequest;
import com.example.studentservice.dto.StudentResponse;
import com.example.studentservice.dto.CourseValidationRequest;
import com.example.studentservice.exception.ResourceNotFoundException;
import com.example.studentservice.model.Student;
import com.example.studentservice.repository.StudentRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceImpl.class);

    private final StudentRepository studentRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;

    private static final String COURSE_SERVICE_NAME = "COURSE-SERVICE"; // This is the Eureka Service ID

    public StudentServiceImpl(StudentRepository studentRepository, ModelMapper modelMapper,
                              RestTemplate restTemplate) {
        this.studentRepository = studentRepository;
        this.modelMapper = modelMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public StudentResponse createStudent(StudentRequest studentRequest) {
        if (studentRepository.findByEmail(studentRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Student with email " + studentRequest.getEmail() + " already exists.");
        }

        Set<Long> courseIds = studentRequest.getCourseIds();
        if (courseIds != null && !courseIds.isEmpty()) {
            validateCoursesExist(courseIds);
        }

        Student student = modelMapper.map(studentRequest, Student.class);
        Student savedStudent = studentRepository.save(student);

        Set<CourseDto> courses = new HashSet<>();
        if (savedStudent.getCourseIds() != null && !savedStudent.getCourseIds().isEmpty()) {
            courses = fetchCoursesByIds(savedStudent.getCourseIds());
        }

        StudentResponse response = modelMapper.map(savedStudent, StudentResponse.class);
        response.setCourses(courses);
        return response;
    }

    @Override
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(student -> {
                    Set<CourseDto> courses = fetchCoursesByIds(student.getCourseIds());
                    StudentResponse response = modelMapper.map(student, StudentResponse.class);
                    response.setCourses(courses);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        Set<CourseDto> courses = fetchCoursesByIds(student.getCourseIds());
        StudentResponse response = modelMapper.map(student, StudentResponse.class);
        response.setCourses(courses);
        return response;
    }

    @Override
    public StudentResponse updateStudent(Long id, StudentRequest studentRequest) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (!existingStudent.getEmail().equals(studentRequest.getEmail()) &&
                studentRepository.findByEmail(studentRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Student with email " + studentRequest.getEmail() + " already exists.");
        }

        Set<Long> courseIds = studentRequest.getCourseIds();
        if (courseIds != null && !courseIds.isEmpty()) {
            validateCoursesExist(courseIds);
        }

        existingStudent.setFirstName(studentRequest.getFirstName());
        existingStudent.setLastName(studentRequest.getLastName());
        existingStudent.setEmail(studentRequest.getEmail());
        existingStudent.setCourseIds(studentRequest.getCourseIds());

        Student updatedStudent = studentRepository.save(existingStudent);

        Set<CourseDto> courses = new HashSet<>();
        if (updatedStudent.getCourseIds() != null && !updatedStudent.getCourseIds().isEmpty()) {
            courses = fetchCoursesByIds(updatedStudent.getCourseIds());
        }

        StudentResponse response = modelMapper.map(updatedStudent, StudentResponse.class);
        response.setCourses(courses);
        return response;
    }

    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    private void validateCoursesExist(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }

        String url = "http://" + COURSE_SERVICE_NAME + "/api/courses/byIds";

        CourseValidationRequest requestBody = new CourseValidationRequest(courseIds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CourseValidationRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<CourseDto[]> responseEntity = restTemplate.postForEntity(url, requestEntity, CourseDto[].class);

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                Set<Long> validCourseIds = Arrays.stream(responseEntity.getBody())
                        .map(CourseDto::getId)
                        .collect(Collectors.toSet());

                Set<Long> invalidCourseIds = courseIds.stream()
                        .filter(id -> !validCourseIds.contains(id))
                        .collect(Collectors.toSet());

                if (!invalidCourseIds.isEmpty()) {
                    // This is the specific IllegalArgumentException you want to throw
                    throw new IllegalArgumentException("One or more courses with IDs " + invalidCourseIds + " do not exist.");
                }
            } else {
                log.error("Failed to validate courses with Course Service. Status: {}", responseEntity.getStatusCode());
                throw new RuntimeException("Error validating courses with Course Service: Unexpected response status " + responseEntity.getStatusCode());
            }
        } catch (IllegalArgumentException e) {
            // Re-throw IllegalArgumentException directly, as it's a known business validation error
            log.error("Invalid course IDs provided: {}", e.getMessage());
            throw e;
        } catch (HttpClientErrorException e) {
            // Catch specific HTTP client errors (e.g., 4xx from Course Service)
            log.error("Client error calling Course Service for validation (Status: {}): {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Error validating courses with Course Service: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            // Catch network/connection issues (e.g., Course Service is down or unreachable)
            log.error("I/O error on POST request for \"{}\": {}", url, e.getMessage());
            throw new RuntimeException("Error validating courses with Course Service: Could not reach Course Service at \"" + COURSE_SERVICE_NAME + "\". Please ensure it's running and registered with Eureka.", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions and log them fully for debugging
            log.error("An unexpected error occurred during course validation: {}", e.getMessage(), e); // Log stack trace
            throw new RuntimeException("An unexpected error occurred during course validation. Please check service logs for details.", e);
        }
    }

    private Set<CourseDto> fetchCoursesByIds(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptySet();
        }

        String url = "http://" + COURSE_SERVICE_NAME + "/api/courses/byIds";

        CourseValidationRequest requestBody = new CourseValidationRequest(courseIds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CourseValidationRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<CourseDto[]> response = restTemplate.postForEntity(url, requestEntity, CourseDto[].class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Arrays.stream(response.getBody()).collect(Collectors.toSet());
            } else {
                log.warn("Failed to fetch courses from Course Service. Status: {}", response.getStatusCode());
                return Collections.emptySet();
            }
        } catch (HttpClientErrorException e) {
            log.error("Client error fetching courses from Course Service (Status: {}): {}", e.getStatusCode(), e.getMessage());
            return Collections.emptySet();
        } catch (ResourceAccessException e) {
            log.error("I/O error fetching courses from Course Service: {}", e.getMessage());
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching courses: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}