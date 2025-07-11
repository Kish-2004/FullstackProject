package com.example.courseservice.service;

import com.example.courseservice.dto.CourseRequest;
import com.example.courseservice.dto.CourseResponse;
import com.example.courseservice.exception.ResourceNotFoundException;
import com.example.courseservice.model.Course;
import com.example.courseservice.repository.CourseRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    public CourseServiceImpl(CourseRepository courseRepository, ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        return courses.stream()
                .map(course -> modelMapper.map(course, CourseResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return modelMapper.map(course, CourseResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByIds(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of(); // Return empty list if no IDs provided
        }
        List<Course> courses = courseRepository.findAllById(courseIds);
        return courses.stream()
                .map(course -> modelMapper.map(course, CourseResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest courseRequest) {
        Course course = modelMapper.map(courseRequest, Course.class);
        Course savedCourse = courseRepository.save(course);
        return modelMapper.map(savedCourse, CourseResponse.class);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest courseRequest) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));


        modelMapper.map(courseRequest, existingCourse);
        existingCourse.setId(id); // Ensure the ID remains the same for the update

        Course updatedCourse = courseRepository.save(existingCourse);
        return modelMapper.map(updatedCourse, CourseResponse.class);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }
}