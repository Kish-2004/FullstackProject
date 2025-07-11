package com.example.studentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @ElementCollection
    @CollectionTable(name = "student_course_enrollments",
            joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "course_id")
    private Set<Long> courseIds = new HashSet<>();

    public Student(String firstName, String lastName, String email, Set<Long> courseIds) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.courseIds = new HashSet<>(courseIds != null ? courseIds : new HashSet<>());
    }

    public Student(Long id, String firstName, String lastName, String email, Set<Long> courseIds) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.courseIds = new HashSet<>(courseIds != null ? courseIds : new HashSet<>());
    }

    public void addCourse(Long courseId) {
        if (this.courseIds == null) {
            this.courseIds = new HashSet<>();
        }
        this.courseIds.add(courseId);
    }

    public void removeCourse(Long courseId) {
        if (this.courseIds != null) {
            this.courseIds.remove(courseId);
        }
    }
}