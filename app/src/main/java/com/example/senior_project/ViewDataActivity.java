package com.example.senior_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewDataActivity extends AppCompatActivity {

    private CustomTaskAdapter adapter;
    private List<TaskData> taskList = new ArrayList<>();
    private List<TaskData> displayedTaskList = new ArrayList<>();
    private List<TaskData> selectedTasks = new ArrayList<>();
    private Switch switchEdit;
    private Button buttonDelete, buttonBack, buttonEdit;
    private ToggleButton toggleExpired;
    private DatabaseReference databaseReference;
    private String calendarId;
    private boolean isDarkMode; // Add dark mode flag
    private LinearLayout mainLayout; // Add reference to the main layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ViewDataActivity", "onCreate called");
        SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_view_data);

        initializeUI();
        loadPreferences();
        updateThemeColors(isDarkMode); // Update UI colors

        calendarId = getIntent().getStringExtra("calendarId");
        Log.d("ViewDataActivity", "Received calendarId: " + calendarId);
        if (calendarId == null || calendarId.isEmpty()) {
            calendarId = sharedPreferences.getString("lastCalendarId", null);
            Log.d("ViewDataActivity", "Using stored calendarId: " + calendarId);
        }

        if (calendarId == null || calendarId.isEmpty()) {
            Log.e("ViewDataActivity", "Calendar ID is null or empty. Cannot fetch tasks.");
            Toast.makeText(this, "Calendar ID not provided.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            fetchTasksForCalendar(calendarId);
        }
        setupListeners();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setImageBitmap(textAsBitmap("AI", 40, Color.WHITE));
        fab.setOnClickListener(v -> {
            Toast.makeText(ViewDataActivity.this, "FAB clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void initializeUI() {
        Log.d("ViewDataActivity", "Initializing UI components");
        switchEdit = findViewById(R.id.switchEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonBack = findViewById(R.id.buttonBack);
        buttonEdit = findViewById(R.id.buttonEdit);
        toggleExpired = findViewById(R.id.toggleExpired);
        mainLayout = findViewById(R.id.mainLayout); // Initialize the main layout

        buttonDelete.setVisibility(View.GONE);
        buttonEdit.setVisibility(View.GONE);

        adapter = new CustomTaskAdapter(this, displayedTaskList, selectedTasks);
        adapter.setDarkMode(isDarkMode); // Set initial dark mode state
        ((ListView) findViewById(R.id.listViewData)).setAdapter(adapter);
    }

    private void loadPreferences() {
        Log.d("ViewDataActivity", "Loading preferences");
        SharedPreferences taskPrefs = getSharedPreferences("TaskPrefs", MODE_PRIVATE);
        boolean isExpiredShown = taskPrefs.getBoolean("isExpiredShown", false);
        toggleExpired.setChecked(isExpiredShown);
    }

    private void updateThemeColors(boolean isDark) {
        // Update the background color of the main layout
        mainLayout.setBackgroundColor(isDark ? Color.BLACK : Color.WHITE);

        // Update text and button colors
        int textColor = isDark ? Color.WHITE : Color.BLACK;
        switchEdit.setTextColor(textColor);
        buttonDelete.setTextColor(textColor);
        buttonBack.setTextColor(textColor);
        buttonEdit.setTextColor(textColor);
        toggleExpired.setTextColor(textColor);
        int buttonBackgroundColor = isDark ? Color.DKGRAY : Color.LTGRAY;
        buttonDelete.setBackgroundColor(buttonBackgroundColor);
        buttonBack.setBackgroundColor(buttonBackgroundColor);
        buttonEdit.setBackgroundColor(buttonBackgroundColor);
        toggleExpired.setBackgroundColor(buttonBackgroundColor);
    }

    private void fetchTasksForCalendar(String calendarId) {
        Log.d("ViewDataActivity", "Fetching tasks for calendarId: " + calendarId);
        databaseReference = FirebaseDatabase.getInstance().getReference("tasks").child(calendarId);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                taskList.clear();
                Log.d("ViewDataActivity", "Tasks retrieved for calendar: " + calendarId);
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    TaskData task = taskSnapshot.getValue(TaskData.class);
                    if (task != null && task.getTaskContent() != null && task.getDeadline() != null
                            && task.getFromTime() != null && task.getToTime() != null) {
                        task.setId(taskSnapshot.getKey());
                        taskList.add(task);
                        Log.d("ViewDataActivity", "Task loaded with ID: " + task.getId() + " Content: " + task.getTaskContent());
                    } else {
                        Log.e("ViewDataActivity", "Task data is null or incomplete");
                    }
                }

                // Sort taskList in descending order by deadline and then by fromTime (12-hour format)
                Collections.sort(taskList, new Comparator<TaskData>() {
                    @Override
                    public int compare(TaskData task1, TaskData task2) {
                        // Compare deadlines in ascending order
                        int dateComparison = task1.getDeadline().compareTo(task2.getDeadline());
                        if (dateComparison != 0) {
                            return dateComparison;
                        }
                        // If deadlines are equal, compare fromTime in ascending order (12-hour format)
                        try {
                            SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
                            Date time1 = sdf12.parse(task1.getFromTime());
                            Date time2 = sdf12.parse(task2.getFromTime());
                            return time1.compareTo(time2); // Ascending order
                        } catch (ParseException e) {
                            Log.e("ViewDataActivity", "Failed to parse times: " + task1.getFromTime() + " or " + task2.getFromTime(), e);
                            return 0; // Fallback to no change in order
                        }
                    }
                });
                filterTasksByExpiration(toggleExpired.isChecked());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("ViewDataActivity", "Firebase error: " + error.getMessage());
                Toast.makeText(ViewDataActivity.this, "Failed to load tasks.", Toast.LENGTH_SHORT).show();
            }
        });
    }    private void filterTasksByExpiration(boolean showExpired) {
        displayedTaskList.clear();
        for (TaskData task : taskList) {
            boolean isExpired = isTaskExpired(task);
            if (showExpired == isExpired) {
                displayedTaskList.add(task);
            }
        }
        Log.d("ViewDataActivity", "Filtered tasks count: " + displayedTaskList.size());
        adapter.notifyDataSetChanged();
    }

    private boolean isTaskExpired(TaskData task) {
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            Date currentDate = new Date();
            String currentDateString = sdfDate.format(currentDate);
            String currentTimeString = sdfTime.format(currentDate);

            Date taskDeadline = sdfDate.parse(task.getDeadline());
            Date taskToTime = sdfTime.parse(task.getToTime());

            Date todayDate = sdfDate.parse(currentDateString);
            Date nowTime = sdfTime.parse(currentTimeString);

            if (taskDeadline.before(todayDate)) {
                return true;
            } else if (taskDeadline.equals(todayDate)) {
                return nowTime.after(taskToTime);
            }
            return false;
        } catch (Exception e) {
            Log.e("ViewDataActivity", "Error parsing date/time", e);
            return false;
        }
    }

    private void setupListeners() {
        Log.d("ViewDataActivity", "Setting up listeners");
        switchEdit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonDelete.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            buttonEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            adapter.setSelectable(isChecked);
            if (!isChecked) selectedTasks.clear();
        });

        toggleExpired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterTasksByExpiration(isChecked);
            SharedPreferences.Editor editor = getSharedPreferences("TaskPrefs", MODE_PRIVATE).edit();
            editor.putBoolean("isExpiredShown", isChecked);
            editor.apply();
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedTasks.isEmpty()) {
                Toast.makeText(ViewDataActivity.this, "No tasks selected for deletion.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create and show confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(ViewDataActivity.this);
            builder.setTitle("Confirm Deletion");
            builder.setMessage("Are you sure you want to delete the selected tasks?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                // Proceed with deletion
                for (TaskData task : new ArrayList<>(selectedTasks)) { // Create a copy to avoid ConcurrentModificationException
                    if (task.getId() == null || task.getId().isEmpty()) {
                        Log.e("ViewDataActivity", "Task ID is null or empty for task: " + task.getTaskContent());
                        continue;
                    }

                    Log.d("ViewDataActivity", "Attempting to delete task with ID: " + task.getId());
                    databaseReference.child(task.getId()).removeValue().addOnSuccessListener(aVoid -> {
                        Log.d("ViewDataActivity", "Task deleted successfully: " + task.getId());
                        selectedTasks.remove(task);
                        taskList.remove(task); // Update taskList to reflect deletion
                        filterTasksByExpiration(toggleExpired.isChecked()); // Refresh displayed tasks
                        adapter.notifyDataSetChanged();
                        Toast.makeText(ViewDataActivity.this, "Task(s) deleted successfully.", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Log.e("ViewDataActivity", "Failed to delete task: " + e.getMessage());
                        Toast.makeText(ViewDataActivity.this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                dialog.dismiss(); // Close dialog without action
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            // Set dialog button text colors based on dark mode
            int buttonTextColor = isDarkMode ? Color.WHITE : Color.BLACK;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonTextColor); // "Yes" button
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonTextColor); // "No" button
        });

        buttonEdit.setOnClickListener(v -> {
            if (selectedTasks.size() != 1) {
                Toast.makeText(ViewDataActivity.this, "Please select exactly one task to edit.", Toast.LENGTH_SHORT).show();
                return;
            }

            TaskData taskToEdit = selectedTasks.get(0);
            Intent intent = new Intent(ViewDataActivity.this, EditActivity.class);
            intent.putExtra("calendarId", calendarId);
            intent.putExtra("taskId", taskToEdit.getId());
            intent.putExtra("taskContent", taskToEdit.getTaskContent());
            intent.putExtra("deadline", taskToEdit.getDeadline());
            intent.putExtra("fromTime", taskToEdit.getFromTime());
            intent.putExtra("toTime", taskToEdit.getToTime());
            intent.putExtra("isImportant", taskToEdit.isImportant());
            intent.putExtra("originActivity", "ViewDataActivity");
            startActivityForResult(intent, 1);
        });

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(ViewDataActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            fetchTasksForCalendar(calendarId);
        }
    }

    private Bitmap textAsBitmap(String text, int textSize, int textColor) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);

        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.0f);
        int height = (int) (baseline + paint.descent() + 0.0f);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }
}