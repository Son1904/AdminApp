package com.example.universalyoga.admin.data.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Name and version of the database
    private static final String DATABASE_NAME = "universalyoga.db";
    private static final int DATABASE_VERSION = 1;

    // Define table name and columns for the Courses table
    public static final String TABLE_COURSES = "courses";
    public static final String COLUMN_COURSE_ID = "_id";
    public static final String COLUMN_COURSE_DAY_OF_WEEK = "day_of_week";
    public static final String COLUMN_COURSE_TIME = "time";
    public static final String COLUMN_COURSE_CAPACITY = "capacity";
    public static final String COLUMN_COURSE_DURATION = "duration";
    public static final String COLUMN_COURSE_PRICE = "price";
    public static final String COLUMN_COURSE_TYPE = "type";
    public static final String COLUMN_COURSE_DESCRIPTION = "description";

    // Define table name and columns for the Class Instances table
    public static final String TABLE_INSTANCES = "class_instances";
    public static final String COLUMN_INSTANCE_ID = "_id";
    public static final String COLUMN_INSTANCE_COURSE_ID = "course_id"; // Foreign key linking to the courses table
    public static final String COLUMN_INSTANCE_DATE = "date";
    public static final String COLUMN_INSTANCE_TEACHER = "teacher";
    public static final String COLUMN_INSTANCE_COMMENTS = "comments";

    // SQL statement to create the Courses table
    private static final String SQL_CREATE_TABLE_COURSES = "CREATE TABLE " + TABLE_COURSES + " (" +
            COLUMN_COURSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_COURSE_DAY_OF_WEEK + " TEXT NOT NULL," +
            COLUMN_COURSE_TIME + " TEXT NOT NULL," +
            COLUMN_COURSE_CAPACITY + " INTEGER NOT NULL," +
            COLUMN_COURSE_DURATION + " INTEGER NOT NULL," +
            COLUMN_COURSE_PRICE + " REAL NOT NULL," +
            COLUMN_COURSE_TYPE + " TEXT NOT NULL," +
            COLUMN_COURSE_DESCRIPTION + " TEXT);";

    // SQL statement to create the Class Instances table
    private static final String SQL_CREATE_TABLE_INSTANCES = "CREATE TABLE " + TABLE_INSTANCES + " (" +
            COLUMN_INSTANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_INSTANCE_COURSE_ID + " INTEGER NOT NULL," +
            COLUMN_INSTANCE_DATE + " TEXT NOT NULL," +
            COLUMN_INSTANCE_TEACHER + " TEXT NOT NULL," +
            COLUMN_INSTANCE_COMMENTS + " TEXT," +
            "FOREIGN KEY(" + COLUMN_INSTANCE_COURSE_ID + ") REFERENCES " + TABLE_COURSES + "(" + COLUMN_COURSE_ID + "));";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Execute SQL statements to create tables when the database is first created
        db.execSQL(SQL_CREATE_TABLE_COURSES);
        db.execSQL(SQL_CREATE_TABLE_INSTANCES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist and recreate them
        // In a real-world application, you would need a better data migration strategy
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTANCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
        onCreate(db);
    }
}