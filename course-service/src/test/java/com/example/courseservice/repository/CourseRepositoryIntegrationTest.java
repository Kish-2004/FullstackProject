package com.example.courseservice.repository;

import com.example.courseservice.model.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase; // <-- Add this import
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // Configures only JPA components
@ActiveProfiles("test") // Activate the "test" profile to load application-test.properties
// IMPORTANT: Tell DataJpaTest NOT to replace your configured datasource
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CourseRepositoryIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager; // Helper to manage entities in tests

    @BeforeEach
    void setUp() {
        // Clear data before each test for isolated tests
        // With 'create-drop' on, the schema is rebuilt for each test run
        // so explicit deleteAllInBatch might not be strictly necessary per test,
        // but it's good for immediate cleanup if needed.
        // For @DataJpaTest, the context might be reused across tests within the same class,
        // so deleteAllInBatch is still a good safety measure for truly isolated test methods.
        courseRepository.deleteAllInBatch();
        entityManager.flush(); // Ensure changes are written before next test
    }

    @Test
    void findByTitle_shouldReturnCourse_whenExists() {
        Course course = new Course(null, "Test Title", "Test Description");
        entityManager.persistAndFlush(course); // Persist directly to DB

        Optional<Course> foundCourse = courseRepository.findByTitle("Test Title");

        assertThat(foundCourse).isPresent();
        assertThat(foundCourse.get().getTitle()).isEqualTo("Test Title");
    }

    @Test
    void findByTitle_shouldReturnEmpty_whenNotExists() {
        Optional<Course> foundCourse = courseRepository.findByTitle("Non Existent Title");
        assertThat(foundCourse).isEmpty();
    }

    @Test
    void saveCourse_shouldPersistCourse() {
        Course course = new Course(null, "New Course", "Details for new course");
        Course savedCourse = courseRepository.save(course);

        assertThat(savedCourse).isNotNull();
        assertThat(savedCourse.getId()).isNotNull();
        assertThat(savedCourse.getTitle()).isEqualTo("New Course");

        // Verify it's actually in the database
        Optional<Course> retrievedCourse = courseRepository.findById(savedCourse.getId());
        assertThat(retrievedCourse).isPresent();
        assertThat(retrievedCourse.get().getTitle()).isEqualTo("New Course");
    }

    @Test
    void updateCourse_shouldModifyExistingCourse() {
        Course course = new Course(null, "Original Title", "Original Desc");
        entityManager.persistAndFlush(course);

        course.setTitle("Updated Title");
        course.setDescription("Updated Desc");
        Course updatedCourse = courseRepository.save(course);

        assertThat(updatedCourse.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedCourse.getDescription()).isEqualTo("Updated Desc");

        Optional<Course> retrievedCourse = courseRepository.findById(course.getId());
        assertThat(retrievedCourse).isPresent();
        assertThat(retrievedCourse.get().getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void deleteCourse_shouldRemoveCourse() {
        Course course = new Course(null, "Course To Delete", "Description");
        entityManager.persistAndFlush(course);
        Long courseId = course.getId();

        courseRepository.deleteById(courseId);

        Optional<Course> retrievedCourse = courseRepository.findById(courseId);
        assertThat(retrievedCourse).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllCourses() {
        Course course1 = new Course(null, "Course 1", "Desc 1");
        Course course2 = new Course(null, "Course 2", "Desc 2");
        entityManager.persist(course1);
        entityManager.persist(course2);
        entityManager.flush();

        List<Course> courses = courseRepository.findAll();

        assertThat(courses).hasSize(2);
        assertThat(courses).extracting(Course::getTitle).containsExactlyInAnyOrder("Course 1", "Course 2");
    }
}