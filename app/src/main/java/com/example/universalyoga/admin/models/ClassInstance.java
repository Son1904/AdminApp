package com.example.universalyoga.admin.models;

public class ClassInstance {
    private final long id;
    private final String date;
    private final String teacher;
    private final String comments;
    public ClassInstance(long id, String date, String teacher, String comments) {
        this.id = id;
        this.date = date;
        this.teacher = teacher;
        this.comments = comments;
    }

    public long getId() { return id; }
    public String getDate() { return date; }
    public String getTeacher() { return teacher; }
    public String getComments() { return comments; }
}