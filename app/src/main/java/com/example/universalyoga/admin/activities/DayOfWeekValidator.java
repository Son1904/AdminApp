package com.example.universalyoga.admin.activities;

import android.os.Parcel;
import com.google.android.material.datepicker.CalendarConstraints;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * A custom DateValidator for the MaterialDatePicker.
 * This class implements a rule that only allows dates that fall on a specific
 * day of the week (e.g., only Mondays).
 * It must implement Parcelable to be passed within the CalendarConstraints.
 */
public class DayOfWeekValidator implements CalendarConstraints.DateValidator {

    // The required day of the week, using Calendar constants (e.g., Calendar.MONDAY).
    private final int requiredDayOfWeek;

    /**
     * Constructor for the DayOfWeekValidator.
     * @param requiredDayOfWeek The day of the week to allow, using a Calendar constant.
     */
    public DayOfWeekValidator(int requiredDayOfWeek) {
        this.requiredDayOfWeek = requiredDayOfWeek;
    }

    /**
     * Private constructor used for recreating the object from a Parcel.
     * @param source The Parcel to read the object's data from.
     */
    private DayOfWeekValidator(Parcel source) {
        requiredDayOfWeek = source.readInt();
    }

    /**
     * The core validation method. It checks if a given date is valid according to the rule.
     * @param date The date to validate, represented in UTC milliseconds from the epoch.
     * @return true if the date's day of the week matches the required day, false otherwise.
     */
    @Override
    public boolean isValid(long date) {
        // Use a UTC calendar to avoid timezone issues with the date picker.
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(date);
        // Compare the day of the week of the given date with the required day.
        return calendar.get(Calendar.DAY_OF_WEEK) == requiredDayOfWeek;
    }

    // --- Parcelable Implementation ---
    // The following methods are required to make this class Parcelable,
    // which allows it to be passed between components (like Activities and Dialogs).

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(requiredDayOfWeek);
    }

    public static final Creator<DayOfWeekValidator> CREATOR = new Creator<DayOfWeekValidator>() {
        @Override
        public DayOfWeekValidator createFromParcel(Parcel source) {
            return new DayOfWeekValidator(source);
        }

        @Override
        public DayOfWeekValidator[] newArray(int size) {
            return new DayOfWeekValidator[size];
        }
    };
}