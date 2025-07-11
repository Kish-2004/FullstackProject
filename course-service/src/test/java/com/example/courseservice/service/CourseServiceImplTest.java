package com.example.courseservice.service;

import com.example.courseservice.dto.CourseRequest;
import com.example.courseservice.dto.CourseResponse;
import com.example.courseservice.exception.ResourceNotFoundException;
import com.example.courseservice.model.Course;
import com.example.courseservice.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach; // Keep BeforeEach for potential shared setup if any
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModelMapper modelMapper; // Still mock ModelMapper

    @InjectMocks
    private CourseServiceImpl courseService;

    // No general ModelMapper stubbing in @BeforeEach anymore,
    // as it caused UnnecessaryStubbingException.
    // Each test will now stub ModelMapper as needed.
    @BeforeEach
    void setUp() {
        // Any other shared setup that doesn't involve stubbing
        // that's not always used by all tests could go here.
        // For now, this method can be empty or removed if not needed.
    }

    @Test
    @DisplayName("Should return all courses when they exist")
    void getAllCourses_shouldReturnAllCourses() {
        // Arrange
        Course course1 = new Course(1L, "Math", "Calculus");
        Course course2 = new Course(2L, "Physics", "Mechanics");
        List<Course> courses = Arrays.asList(course1, course2);

        CourseResponse response1 = new CourseResponse(1L, "Math", "Calculus");
        CourseResponse response2 = new CourseResponse(2L, "Physics", "Mechanics");

        when(courseRepository.findAll()).thenReturn(courses);
        // Moved ModelMapper stubbing here, as it's used only by this test
        when(modelMapper.map(course1, CourseResponse.class)).thenReturn(response1);
        when(modelMapper.map(course2, CourseResponse.class)).thenReturn(response2);


        // Act
        List<CourseResponse> actualCourses = courseService.getAllCourses();

        // Assert
        assertThat(actualCourses).isNotEmpty();
        assertThat(actualCourses).hasSize(2);
        assertThat(actualCourses).containsExactlyInAnyOrder(response1, response2);
        verify(courseRepository, times(1)).findAll();
        verify(modelMapper, times(1)).map(course1, CourseResponse.class);
        verify(modelMapper, times(1)).map(course2, CourseResponse.class);
    }

    @Test
    @DisplayName("Should return empty list when no courses exist")
    void getAllCourses_shouldReturnEmptyList_whenNoCoursesExist() {
        // Arrange
        when(courseRepository.findAll()).thenReturn(List.of());

        // Act
        List<CourseResponse> actualCourses = courseService.getAllCourses();

        // Assert
        assertThat(actualCourses).isEmpty();
        verify(courseRepository, times(1)).findAll();
        verifyNoInteractions(modelMapper); // No mapping should occur if list is empty
    }

    @Test
    @DisplayName("Should return course by ID when it exists")
    void getCourseById_shouldReturnCourse_whenExists() {
        // Arrange
        Long courseId = 1L;
        Course course = new Course(courseId, "History", "Ancient History");
        CourseResponse expectedResponse = new CourseResponse(courseId, "History", "Ancient History");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        // Moved ModelMapper stubbing here
        when(modelMapper.map(course, CourseResponse.class)).thenReturn(expectedResponse);

        // Act
        CourseResponse actualCourse = courseService.getCourseById(courseId);

        // Assert
        assertThat(actualCourse).isEqualTo(expectedResponse);
        verify(courseRepository, times(1)).findById(courseId);
        verify(modelMapper, times(1)).map(course, CourseResponse.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when course by ID does not exist")
    void getCourseById_shouldThrowResourceNotFoundException_whenNotExists() {
        // Arrange
        Long courseId = 1L;
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                courseService.getCourseById(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course not found with id: " + courseId);
        verify(courseRepository, times(1)).findById(courseId);
        verifyNoInteractions(modelMapper); // No mapping if course not found
    }

    @Test
    @DisplayName("Should return courses by IDs when they exist")
    void getCoursesByIds_shouldReturnCourses_whenAllExist() {
        // Arrange
        Set<Long> courseIds = new HashSet<>(Arrays.asList(1L, 2L));
        Course course1 = new Course(1L, "Math", "Calculus");
        Course course2 = new Course(2L, "Physics", "Mechanics");
        List<Course> courses = Arrays.asList(course1, course2);

        CourseResponse response1 = new CourseResponse(1L, "Math", "Calculus");
        CourseResponse response2 = new CourseResponse(2L, "Physics", "Mechanics");

        when(courseRepository.findAllById(courseIds)).thenReturn(courses);
        // Moved ModelMapper stubbing here
        when(modelMapper.map(course1, CourseResponse.class)).thenReturn(response1);
        when(modelMapper.map(course2, CourseResponse.class)).thenReturn(response2);

        // Act
        List<CourseResponse> actualCourses = courseService.getCoursesByIds(courseIds);

        // Assert
        assertThat(actualCourses).isNotEmpty();
        assertThat(actualCourses).hasSize(2);
        assertThat(actualCourses).containsExactlyInAnyOrder(response1, response2);
        verify(courseRepository, times(1)).findAllById(courseIds);
        verify(modelMapper, times(1)).map(course1, CourseResponse.class);
        verify(modelMapper, times(1)).map(course2, CourseResponse.class);
    }

    @Test
    @DisplayName("Should return empty list when no courses found for given IDs")
    void getCoursesByIds_shouldReturnEmptyList_whenNoneExist() {
        // Arrange
        Set<Long> courseIds = new HashSet<>(Arrays.asList(1L, 2L));
        when(courseRepository.findAllById(courseIds)).thenReturn(List.of());

        // Act
        List<CourseResponse> actualCourses = courseService.getCoursesByIds(courseIds);

        // Assert
        assertThat(actualCourses).isEmpty();
        verify(courseRepository, times(1)).findAllById(courseIds);
        verifyNoInteractions(modelMapper);
    }

    @Test
    @DisplayName("Should return empty list when input set of IDs is empty")
    void getCoursesByIds_shouldReturnEmptyList_whenIdsSetIsEmpty() {
        // Arrange
        Set<Long> courseIds = new HashSet<>();

        // Act
        List<CourseResponse> actualCourses = courseService.getCoursesByIds(courseIds);

        // Assert
        assertThat(actualCourses).isEmpty();
        verifyNoInteractions(courseRepository);
        verifyNoInteractions(modelMapper);
    }

    @Test
    @DisplayName("Should return empty list when input set of IDs is null")
    void getCoursesByIds_shouldReturnEmptyList_whenIdsSetIsNull() {
        // Act
        List<CourseResponse> actualCourses = courseService.getCoursesByIds(null);

        // Assert
        assertThat(actualCourses).isEmpty();
        verifyNoInteractions(courseRepository);
        verifyNoInteractions(modelMapper);
    }

    @Test
    @DisplayName("Should successfully create a new course")
    void createCourse_shouldCreateCourseSuccessfully() {
        // Arrange
        CourseRequest courseRequest = new CourseRequest("Art", "Drawing basics");
        Course courseToSave = new Course(null, "Art", "Drawing basics");
        Course savedCourse = new Course(1L, "Art", "Drawing basics");
        CourseResponse expectedResponse = new CourseResponse(1L, "Art", "Drawing basics");

        // Moved ModelMapper stubbing here
        when(modelMapper.map(courseRequest, Course.class)).thenReturn(courseToSave);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        when(modelMapper.map(savedCourse, CourseResponse.class)).thenReturn(expectedResponse);


        // Act
        CourseResponse actualResponse = courseService.createCourse(courseRequest);

        // Assert
        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(modelMapper, times(1)).map(courseRequest, Course.class);
        verify(courseRepository, times(1)).save(courseToSave);
        verify(modelMapper, times(1)).map(savedCourse, CourseResponse.class);
    }

    @Test
    @DisplayName("Should successfully update an existing course")
    void updateCourse_shouldUpdateCourseSuccessfully() {
        // Arrange
        Long courseId = 1L;
        CourseRequest courseRequest = new CourseRequest("Updated Course", "Updated description");
        Course existingCourse = new Course(courseId, "Old Title", "Old Description");
        Course updatedCourseEntity = new Course(courseId, "Updated Course", "Updated description");
        CourseResponse expectedResponse = new CourseResponse(courseId, "Updated Course", "Updated description");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        // Moved ModelMapper stubbing here
        doAnswer(invocation -> {
            CourseRequest req = invocation.getArgument(0);
            Course existing = invocation.getArgument(1);
            existing.setTitle(req.getTitle());
            existing.setDescription(req.getDescription());
            return null;
        }).when(modelMapper).map(courseRequest, existingCourse);

        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourseEntity);
        when(modelMapper.map(updatedCourseEntity, CourseResponse.class)).thenReturn(expectedResponse);


        // Act
        CourseResponse actualResponse = courseService.updateCourse(courseId, courseRequest);

        // Assert
        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(courseRepository, times(1)).findById(courseId);
        verify(modelMapper, times(1)).map(courseRequest, existingCourse);
        verify(courseRepository, times(1)).save(existingCourse);
        verify(modelMapper, times(1)).map(updatedCourseEntity, CourseResponse.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent course")
    void updateCourse_shouldThrowResourceNotFoundException_whenCourseNotExists() {
        // Arrange
        Long courseId = 99L;
        CourseRequest courseRequest = new CourseRequest("Non Existent", "Description");
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                courseService.updateCourse(courseId, courseRequest));

        assertThat(exception.getMessage()).isEqualTo("Course not found with id: " + courseId);
        verify(courseRepository, times(1)).findById(courseId);
        verifyNoMoreInteractions(courseRepository);
        verifyNoInteractions(modelMapper);
    }

    @Test
    @DisplayName("Should successfully delete an existing course")
    void deleteCourse_shouldDeleteCourseSuccessfully() {
        // Arrange
        Long courseId = 1L;
        when(courseRepository.existsById(courseId)).thenReturn(true);
        doNothing().when(courseRepository).deleteById(courseId);

        // Act
        courseService.deleteCourse(courseId);

        // Assert
        verify(courseRepository, times(1)).existsById(courseId);
        verify(courseRepository, times(1)).deleteById(courseId);
        verifyNoInteractions(modelMapper); // No mapping is done in delete
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent course")
    void deleteCourse_shouldThrowResourceNotFoundException_whenCourseNotExists() {
        // Arrange
        Long courseId = 99L;
        when(courseRepository.existsById(courseId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                courseService.deleteCourse(courseId));

        assertThat(exception.getMessage()).isEqualTo("Course not found with id: " + courseId);
        verify(courseRepository, times(1)).existsById(courseId);
        verifyNoMoreInteractions(courseRepository);
        verifyNoInteractions(modelMapper);
    }
}