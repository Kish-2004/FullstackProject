package com.example.studentservice.service;

import com.example.studentservice.dto.StudentRequest;
import com.example.studentservice.dto.StudentResponse;

import java.util.List;

public interface StudentService {
    List<StudentResponse> getAllStudents();
    StudentResponse getStudentById(Long id);
    StudentResponse createStudent(StudentRequest studentRequest);
    StudentResponse updateStudent(Long id, StudentRequest studentRequest);
    void deleteStudent(Long id);

}