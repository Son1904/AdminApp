package com.example.universalyoga.admin.models;

/**
 * Model class to hold information for a search result.
 */
public class SearchResult {
    private final long courseId;
    private final String courseType;
    private final String instanceDate;
    private final String instanceTeacher;
    private final String dayOfWeek; // Add new field

    public SearchResult(long courseId, String courseType, String instanceDate, String instanceTeacher, String dayOfWeek) {
        this.courseId = courseId;
        this.courseType = courseType;
        this.instanceDate = instanceDate;
        this.instanceTeacher = instanceTeacher;
        this.dayOfWeek = dayOfWeek; // Update constructor
    }

    // Getters
    public long getCourseId() {
        return courseId;
    }
    public String getCourseType() {
        return courseType;
    }
    public String getInstanceDate() {
        return instanceDate;
    }
    public String getInstanceTeacher() {
        return instanceTeacher;
    }
    public String getDayOfWeek() { // Thêm getter mới
        return dayOfWeek;
    }
}