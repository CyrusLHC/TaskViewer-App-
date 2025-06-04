package com.example.senior_project;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataManager {
    private static DataManager instance;
    private List<TaskData> dataList;
    private DatabaseReference databaseReference;
    private List<CalendarData> userCalendars; // List to hold user calendars

    private DataManager() {
        dataList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("tasks");
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // Add a new task for the current user
    public void addData(TaskData taskData) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userTasksRef = databaseReference.child(userId);
        String taskId = userTasksRef.push().getKey(); // Generate a unique ID for the task
        userTasksRef.child(taskId).setValue(taskData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DataManager", "Task saved successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.e("DataManager", "Failed to save task: " + e.getMessage());
                });
    }

    // Retrieve tasks for a specific user
    public void retrieveData(String userId, DataCallback callback) {
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    TaskData taskData = snapshot.getValue(TaskData.class);
                    if (taskData != null) {
                        dataList.add(taskData);
                    }
                }
                callback.onDataReceived(dataList); // Notify the callback with the retrieved data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DataManager", "Error retrieving data: " + databaseError.getMessage());
                callback.onDataReceived(new ArrayList<>()); // Return an empty list on error
            }
        });
    }

    // Listen for real-time updates to the user's tasks
    public void getDataList(DataCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userTasksRef = databaseReference.child(userId);

        userTasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataList.clear(); // Clear the list before adding new data
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    TaskData taskData = snapshot.getValue(TaskData.class);
                    if (taskData != null) {
                        dataList.add(taskData);
                    }
                }
                callback.onDataReceived(dataList); // Notify the callback with the retrieved data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DataManager", "Error retrieving data: " + databaseError.getMessage());
                callback.onDataReceived(new ArrayList<>()); // Return an empty list on error
            }
        });
    }

    // Callback interface for data retrieval
    public interface DataCallback<T> {
        void onDataReceived(List<T> dataList);
    }

    public void deleteData(TaskData taskData) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userTasksRef = databaseReference.child(userId);

        // Find the task by its content or any unique identifier
        userTasksRef.orderByChild("taskContent").equalTo(taskData.getTaskContent()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue() // Remove the task from the database
                            .addOnSuccessListener(aVoid -> {
                                Log.d("DataManager", "Task deleted successfully.");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("DataManager", "Failed to delete task: " + e.getMessage());
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DataManager", "Error deleting task: " + databaseError.getMessage());
            }
        });
    }

    // Add a new method to add a calendar
    public void addCalendar(CalendarData calendarData) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userCalendarsRef = databaseReference.child("calendars").child(userId);
        String calendarId = userCalendarsRef.push().getKey();
        userCalendarsRef.child(calendarId).setValue(calendarData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DataManager", "Calendar saved successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.e("DataManager", "Failed to save calendar: " + e.getMessage());
                });
    }

    // Add a method to retrieve calendars
    public void getCalendars(String userId, CalendarDataCallback callback) {
        DatabaseReference userCalendarsRef = databaseReference.child("calendars").child(userId);

        userCalendarsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<CalendarData> calendarList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CalendarData calendarData = snapshot.getValue(CalendarData.class);
                    if (calendarData != null) {
                        calendarList.add(calendarData);
                    }
                }
                Log.d("DataManager", "Calendar list updated: " + calendarList.size());
                callback.onDataReceived(calendarList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DataManager", "Error retrieving calendars: " + databaseError.getMessage());
                callback.onDataReceived(new ArrayList<>());
            }
        });
    }


    // Callback interface for task list retrieval
    public interface TaskListCallback {
        void onTaskListReceived(List<TaskData> taskList);
    }

    public void getTasksForCalendar(String calendarId, TaskListCallback callback) {
        DatabaseReference calendarTasksRef = databaseReference.child(calendarId);
        calendarTasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<TaskData> taskList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    TaskData task = snapshot.getValue(TaskData.class);
                    if (task != null) {
                        taskList.add(task);
                    }
                }
                Log.d("DataManager", "Tasks retrieved for calendar ID: " + calendarId + ", Count: " + taskList.size());
                callback.onTaskListReceived(taskList); // Notify the callback with the retrieved task list
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DataManager", "Error retrieving tasks: " + databaseError.getMessage());
                callback.onTaskListReceived(new ArrayList<>()); // Return an empty list on error
            }
        });
    }

    public void deleteCalendarForUser(String calendarId) {
        // Remove the calendar from the local list
        if (userCalendars != null) {
            for (Iterator<CalendarData> iterator = userCalendars.iterator(); iterator.hasNext(); ) {
                CalendarData calendar = iterator.next();
                if (calendar.getCalendarId().equals(calendarId)) {
                    iterator.remove(); // Remove the calendar from the list
                    Log.d("DataManager", "Calendar removed from user list successfully.");
                    break;
                }
            }
        }
    }

    public void deleteCalendarForEveryone(String calendarId) {
        DatabaseReference calendarRef = databaseReference.child("calendars").child(calendarId);
        calendarRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("DataManager", "Calendar deleted for everyone successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.e("DataManager", "Failed to delete calendar for everyone: " + e.getMessage());
                });
    }

    public void joinCalendar(String calendarId, CalendarJoinCallback callback) {
        DatabaseReference calendarsRef = databaseReference.child("calendars");

        calendarsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean calendarFound = false;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot calendarSnapshot : userSnapshot.getChildren()) {
                        CalendarData calendarData = calendarSnapshot.getValue(CalendarData.class);
                        if (calendarData != null && calendarData.getCalendarId().equals(calendarId)) {
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            DatabaseReference userCalendarsRef = databaseReference.child("calendars").child(userId);

                            userCalendarsRef.child(calendarSnapshot.getKey()).setValue(calendarData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("DataManager", "Successfully joined calendar: " + calendarId);
                                        callback.onJoinSuccess(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("DataManager", "Failed to join calendar: " + e.getMessage());
                                        callback.onJoinSuccess(false);
                                    });

                            calendarFound = true;
                            break;
                        }
                    }
                    if (calendarFound) break;
                }

                if (!calendarFound) {
                    Log.e("DataManager", "Calendar ID not found: " + calendarId);
                    callback.onJoinSuccess(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DataManager", "Error retrieving calendar: " + error.getMessage());
                callback.onJoinSuccess(false);
            }
        });
    }



    public interface CalendarJoinCallback {
        void onJoinSuccess(boolean success);
    }
}