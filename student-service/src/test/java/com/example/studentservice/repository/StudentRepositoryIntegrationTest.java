package com.example.studentservice.repository;

import com.example.studentservice.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional; // Import this

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StudentRepositoryIntegrationTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    @Transactional // Ensure deleteAll runs in its own transaction and commits
    void setUp() {
        studentRepository.deleteAll();
        entityManager.flush(); // Ensure changes are written to DB
        entityManager.clear(); // Clear the persistence context
    }

    @Test
    void findByEmail_shouldReturnStudent_whenExists() {
        Student student = new Student("John", "Doe", "john.doe@example.com", Set.of(101L, 102L));
        entityManager.persistAndFlush(student);

        Optional<Student> foundStudent = studentRepository.findByEmail("john.doe@example.com");

        assertThat(foundStudent).isPresent();
        assertThat(foundStudent.get().getEmail()).isEqualTo("john.doe@example.com");
        // Use assertThat(foundStudent.get().getCourseIds()).containsExactlyInAnyOrder(101L, 102L); if order doesn't matter
        assertThat(foundStudent.get().getCourseIds()).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenNotExists() {
        Optional<Student> foundStudent = studentRepository.findByEmail("non.existent@example.com");
        assertThat(foundStudent).isEmpty();
    }

    @Test
    void saveStudent_shouldPersistStudent() {
        Student student = new Student("Jane", "Smith", "jane.smith@example.com", Set.of(201L));
        Student savedStudent = studentRepository.save(student);

        assertThat(savedStudent).isNotNull();
        assertThat(savedStudent.getId()).isNotNull();
        assertThat(savedStudent.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(savedStudent.getCourseIds()).containsExactly(201L);

        entityManager.clear();
        Optional<Student> retrievedStudent = studentRepository.findById(savedStudent.getId());
        assertThat(retrievedStudent).isPresent();
        assertThat(retrievedStudent.get().getEmail()).isEqualTo("jane.smith@example.com");
    }

    @Test
    void updateStudent_shouldModifyExistingStudent() {
        Student student = new Student("Original", "Name", "original@example.com", Set.of(301L));
        entityManager.persistAndFlush(student);
        Long studentId = student.getId();

        // Retrieve the managed entity to update it
        // This is safer than updating a detached entity then saving,
        // though save() usually handles it. Explicitly getting it is clearer.
        Student managedStudent = entityManager.find(Student.class, studentId);
        assertThat(managedStudent).isNotNull();

        managedStudent.setFirstName("Updated");
        managedStudent.setEmail("updated@example.com");
        // Clear and add new courses or use add/remove methods if available
        managedStudent.getCourseIds().clear(); // Clear existing
        managedStudent.getCourseIds().addAll(Set.of(302L, 303L)); // Add new

        entityManager.flush(); // Flush changes to the database
        entityManager.clear(); // Clear context to ensure fresh read

        Optional<Student> retrievedStudent = studentRepository.findById(studentId);
        assertThat(retrievedStudent).isPresent();
        assertThat(retrievedStudent.get().getFirstName()).isEqualTo("Updated");
        assertThat(retrievedStudent.get().getEmail()).isEqualTo("updated@example.com");
        assertThat(retrievedStudent.get().getCourseIds()).containsExactlyInAnyOrder(302L, 303L);
    }


    @Test
    void deleteStudent_shouldRemoveStudent() {
        Student student = new Student("Student", "ToDelete", "delete@example.com", Set.of());
        entityManager.persistAndFlush(student);
        Long studentId = student.getId();

        studentRepository.deleteById(studentId);
        entityManager.flush(); // Ensure the delete is flushed to the DB
        entityManager.clear(); // Clear context to ensure fresh read

        Optional<Student> retrievedStudent = studentRepository.findById(studentId);
        assertThat(retrievedStudent).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllStudents() {
        Student student1 = new Student("Alice", "A", "alice@example.com", Set.of(401L));
        Student student2 = new Student("Bob", "B", "bob@example.com", Set.of(402L, 403L));
        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.flush();

        entityManager.clear();
        List<Student> students = studentRepository.findAll();

        assertThat(students).hasSize(2);
        assertThat(students).extracting(Student::getFirstName).containsExactlyInAnyOrder("Alice", "Bob");
        assertThat(students).extracting(Student::getEmail).containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
    }
}