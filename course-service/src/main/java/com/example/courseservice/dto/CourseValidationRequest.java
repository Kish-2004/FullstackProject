package com.example.courseservice.dto;

import java.util.Objects;
import java.util.Set;

public class CourseValidationRequest {
    private Set<Long> courseIds;

    public CourseValidationRequest() {}

    public CourseValidationRequest(Set<Long> courseIds) {
        this.courseIds = courseIds;
    }

    public Set<Long> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(Set<Long> courseIds) {
        this.courseIds = courseIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseValidationRequest that = (CourseValidationRequest) o;
        return Objects.equals(courseIds, that.courseIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseIds);
    }

    @Override
    public String toString() {
        return "CourseValidationRequest{" +
                "courseIds=" + courseIds +
                '}';
    }
}