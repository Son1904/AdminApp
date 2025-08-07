package com.example.universalyoga.admin.models;

public class Course {
    private long id;
    private String dayOfWeek;
    private String time;
    private String type;
    // Add other properties if needed

    public Course(long id, String dayOfWeek, String time, String type) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.type = type;
    }

    // Getters
    public long getId() { return id; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getTime() { return time; }
    public String getType() { return type; }
}