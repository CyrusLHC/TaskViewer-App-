package com.example.senior_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.AlarmManager;
import android.app.PendingIntent;

public class EditActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private EditText editTextTaskContent;
    private Switch switch1, noticeSwitch;
    private TimePicker fromTimePicker, toTimePicker;
    private Button buttonSave;
    private GridView calendarGrid;
    private TextView monthYearText;
    private ImageView dropdownArrow;
    private Calendar currentDate;
    private CalendarAdapter calendarAdapter;
    private List<TaskData> taskList;
    private boolean isDarkMode;
    private DatabaseReference databaseReference;
    private String calendarId;
    private int selectedDay = -1;
    private DataManager dataManager;
    private String taskId;
    private CoordinatorLayout mainLayout; // Add reference to the main layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://com4101-senior-project-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = firebaseDatabase.getReference("tasks");

        dataManager = DataManager.getInstance();

        initializeViews();
        updateThemeColors(isDarkMode);

        Intent intent = getIntent();
        calendarId = intent.getStringExtra("calendarId");
        if (calendarId == null) {
            calendarId = sharedPreferences.getString("lastCalendarId", null);
        }

        if (calendarId == null) {
            Toast.makeText(this, "Calendar ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskId = intent.getStringExtra("taskId");
        if (taskId != null) {
            editTextTaskContent.setText(intent.getStringExtra("taskContent"));
            String deadline = intent.getStringExtra("deadline");
            monthYearText.setText(deadline);
            switch1.setChecked(intent.getBooleanExtra("isImportant", false));

            String fromTime = intent.getStringExtra("fromTime");
            if (fromTime != null) {
                String[] fromParts = fromTime.split(":|\\s");
                int hour = Integer.parseInt(fromParts[0]);
                int minute = Integer.parseInt(fromParts[1]);
                if (fromParts.length > 2 && fromParts[2].equalsIgnoreCase("PM") && hour != 12) hour += 12;
                if (fromParts.length > 2 && fromParts[2].equalsIgnoreCase("AM") && hour == 12) hour = 0;
                fromTimePicker.setHour(hour);
                fromTimePicker.setMinute(minute);
            }

            String toTime = intent.getStringExtra("toTime");
            if (toTime != null) {
                String[] toParts = toTime.split(":|\\s");
                int hour = Integer.parseInt(toParts[0]);
                int minute = Integer.parseInt(toParts[1]);
                if (toParts.length > 2 && toParts[2].equalsIgnoreCase("PM") && hour != 12) hour += 12;
                if (toParts.length > 2 && toParts[2].equalsIgnoreCase("AM") && hour == 12) hour = 0;
                toTimePicker.setHour(hour);
                toTimePicker.setMinute(minute);
            }

            if (deadline != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = sdf.parse(deadline);
                    if (date != null) {
                        currentDate.setTime(date);
                        selectedDay = currentDate.get(Calendar.DAY_OF_MONTH);
                    }
                } catch (ParseException e) {
                    Log.e("EditActivity", "Error parsing deadline: " + e.getMessage());
                    selectedDay = -1;
                }
            }
        } else {
            selectedDay = intent.getIntExtra("SELECTED_DAY", -1);
        }

        dataManager.getTasksForCalendar(calendarId, taskList -> {
            if (taskList != null && !taskList.isEmpty()) {
                this.taskList = taskList;
                Log.d("EditActivity", "Data List retrieved: " + taskList.size() + " tasks");
            } else {
                this.taskList = new ArrayList<>();
                Log.d("EditActivity", "No data retrieved for calendar ID: " + calendarId);
            }
            runOnUiThread(this::updateCalendar);
        });

        buttonSave.setOnClickListener(v -> saveTask());

        LinearLayout monthYearLayout = findViewById(R.id.monthYearLayout);
        monthYearLayout.setOnClickListener(v -> showMonthYearPicker());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            String origin = getIntent().getStringExtra("originActivity");
            if (origin == null) {
                origin = "MainActivity";
            }
            AddScheduleBottomSheetDialog bottomSheetDialog = AddScheduleBottomSheetDialog.newInstance(calendarId, origin);
            bottomSheetDialog.setOnScheduleOptionSelectedListener(new AddScheduleBottomSheetDialog.OnScheduleOptionSelectedListener() {
                @Override
                public void onAddScheduleByText() {
                    Toast.makeText(EditActivity.this, "Add Schedule by Text clicked", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAddScheduleByVoice() {
                    Toast.makeText(EditActivity.this, "Add Schedule by Voice clicked", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onTaskParsed(TaskData taskData, boolean updateCalendar) {
                    Log.d("EditActivity", "Parsed TaskData from BottomSheet: " + taskData.toString());
                    runOnUiThread(() -> {
                        fillFieldsFromTaskData(taskData);
                        if (!updateCalendar) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date date = sdf.parse(taskData.getDeadline());
                                if (date != null) {
                                    currentDate.setTime(date);
                                    selectedDay = currentDate.get(Calendar.DAY_OF_MONTH);
                                    updateCalendar();
                                }
                            } catch (ParseException e) {
                                Log.e("EditActivity", "Error parsing TaskData deadline: " + e.getMessage());
                            }
                        } else {
                            taskList.add(taskData);
                            updateCalendar();
                        }
                    });
                }
            });
            bottomSheetDialog.show(getSupportFragmentManager(), "AddScheduleBottomSheetDialog");
        });

        fab.setImageBitmap(textAsBitmap("AI", 40, Color.WHITE));
    }

    private void initializeViews() {
        editTextTaskContent = findViewById(R.id.editTextTaskContent);
        fromTimePicker = findViewById(R.id.fromTimePicker);
        toTimePicker = findViewById(R.id.toTimePicker);
        switch1 = findViewById(R.id.switch1);
        noticeSwitch = findViewById(R.id.noticeSwitch);
        buttonSave = findViewById(R.id.buttonSave);
        monthYearText = findViewById(R.id.monthYearText);
        calendarGrid = findViewById(R.id.calendarGrid);
        dropdownArrow = findViewById(R.id.dropdownArrow);
        mainLayout = findViewById(R.id.coordinatorLayout); // Initialize the main layout
        currentDate = Calendar.getInstance();

        fromTimePicker.setIs24HourView(false);
        toTimePicker.setIs24HourView(false);
    }

    private void fillFieldsFromTaskData(TaskData taskData) {
        editTextTaskContent.setText(taskData.getTaskContent());
        monthYearText.setText(taskData.getDeadline());
        switch1.setChecked(taskData.isImportant());

        String[] fromParts = taskData.getFromTime().split(":");
        int fromHour = Integer.parseInt(fromParts[0]);
        int fromMinute = Integer.parseInt(fromParts[1]);
        fromTimePicker.setHour(fromHour);
        fromTimePicker.setMinute(fromMinute);

        String[] toParts = taskData.getToTime().split(":");
        int toHour = Integer.parseInt(toParts[0]);
        int toMinute = Integer.parseInt(toParts[1]);
        toTimePicker.setHour(toHour);
        toTimePicker.setMinute(toMinute);

        noticeSwitch.setChecked("important".equalsIgnoreCase(taskData.getType()));
    }

    private void saveTask() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskContent = editTextTaskContent.getText().toString();
        if (taskContent.isEmpty()) {
            Toast.makeText(this, "Task content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isImportant = switch1.isChecked();
        boolean needNotice = noticeSwitch.isChecked();

        int fromHour = fromTimePicker.getHour();
        int fromMinute = fromTimePicker.getMinute();
        String fromTime = formatTime(fromHour, fromMinute);

        int toHour = toTimePicker.getHour();
        int toMinute = toTimePicker.getMinute();
        String toTime = formatTime(toHour, toMinute);

        if (!isTimeValid(fromHour, fromMinute, toHour, toMinute)) {
            Toast.makeText(this, "Invalid time selection. 'From' time must be earlier than 'To' time.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (calendarId == null) {
            Toast.makeText(this, "Calendar ID not found. Please select a calendar first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedDay == -1) {
            Toast.makeText(this, "Please select a day for the task", Toast.LENGTH_SHORT).show();
            return;
        }

        String deadline = String.format(Locale.getDefault(), "%d-%02d-%02d",
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH) + 1,
                selectedDay);

        String taskType = isImportant ? "important" : "deadline";
        String importanceDisplay = isImportant ? "Important: " : "";

        String finalTaskId = (taskId != null) ? taskId : databaseReference.child(calendarId).push().getKey();
        TaskData taskData = new TaskData(finalTaskId, taskContent, deadline, fromTime, toTime, isImportant, importanceDisplay, taskType);

        databaseReference.child(calendarId).child(finalTaskId).setValue(taskData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    if (!taskList.contains(taskData)) {
                        taskList.add(taskData);
                    }
                    updateCalendar();
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        if (needNotice) {
            scheduleNotification(taskContent, deadline, fromTime);
        }
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d %s",
                (hour == 0 || hour == 12) ? 12 : hour % 12,
                minute,
                hour < 12 ? "AM" : "PM");
    }

    private boolean isTimeValid(int fromHour, int fromMinute, int toHour, int toMinute) {
        return (fromHour < toHour) || (fromHour == toHour && fromMinute < toMinute);
    }

    private void updateThemeColors(boolean isDark) {
        // Update the background color of the main layout
        mainLayout.setBackgroundColor(isDark ? Color.BLACK : Color.WHITE);

        // Update text and icon colors
        int textColor = isDark ? Color.WHITE : Color.BLACK;
        monthYearText.setTextColor(textColor);
        dropdownArrow.setColorFilter(textColor);
        editTextTaskContent.setTextColor(textColor);
        editTextTaskContent.setHintTextColor(isDark ? Color.LTGRAY : Color.GRAY);
        switch1.setTextColor(textColor);
        noticeSwitch.setTextColor(textColor);
        buttonSave.setTextColor(textColor);
        buttonSave.setBackgroundColor(isDark ? Color.DKGRAY : Color.LTGRAY);

        // Update TimePicker text color
        setTimePickerTextColor(fromTimePicker, isDark);
        setTimePickerTextColor(toTimePicker, isDark);

        // Refresh the calendar border by reapplying the drawable
        calendarGrid.setBackgroundResource(R.drawable.calendar_border);

        // Update the calendar adapter
        if (calendarAdapter != null) {
            calendarAdapter.setDarkMode(isDark);
        }
    }
    // Helper method to set TimePicker text color
    private void setTimePickerTextColor(TimePicker timePicker, boolean isDark) {
        int textColor = isDark ? Color.WHITE : Color.BLACK;
        try {
            // Access the NumberPickers inside TimePicker (hour and minute)
            for (int i = 0; i < timePicker.getChildCount(); i++) {
                View child = timePicker.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) child;
                    for (int j = 0; j < layout.getChildCount(); j++) {
                        View grandChild = layout.getChildAt(j);
                        if (grandChild instanceof NumberPicker) {
                            NumberPicker numberPicker = (NumberPicker) grandChild;
                            // Access the EditText inside NumberPicker
                            for (int k = 0; k < numberPicker.getChildCount(); k++) {
                                View pickerChild = numberPicker.getChildAt(k);
                                if (pickerChild instanceof EditText) {
                                    ((EditText) pickerChild).setTextColor(textColor);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("EditActivity", "Error setting TimePicker text color: " + e.getMessage());
        }
    }

    public void onDateSelected(int day) {
        selectedDay = day;
        currentDate.set(Calendar.DAY_OF_MONTH, day);
        String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH) + 1,
                day);
        monthYearText.setText(selectedDate);
        updateCalendar();
        Toast.makeText(this, "Selected date: " + selectedDate, Toast.LENGTH_SHORT).show();
    }

    private void updateCalendar() {
        if (taskList == null) {
            taskList = new ArrayList<>();
        }

        if (selectedDay != -1) {
            String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH) + 1,
                    selectedDay);
            monthYearText.setText(selectedDate);
        } else {
            String monthYear = String.format(Locale.getDefault(), "%d / %02d",
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH) + 1);
            monthYearText.setText(monthYear);
        }

        List<String> daysList = new ArrayList<>();
        String[] weekDays = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        for (String day : weekDays) {
            daysList.add(day);
        }

        Calendar calendar = (Calendar) currentDate.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 0; i < firstDayOfWeek; i++) {
            daysList.add("");
        }

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            daysList.add(String.valueOf(i));
        }

        if (calendarAdapter == null) {
            calendarAdapter = new CalendarAdapter(this, daysList, currentDate, taskList, selectedDay);
            calendarAdapter.setDarkMode(isDarkMode); // Ensure dark mode is set initially
            calendarGrid.setAdapter(calendarAdapter);
        } else {
            calendarAdapter.updateData(daysList, currentDate, taskList);
            calendarAdapter.setSelectedDate(selectedDay);
            calendarAdapter.setDarkMode(isDarkMode); // Update dark mode on refresh
            calendarAdapter.notifyDataSetChanged();
        }
    }

    private void showMonthYearPicker() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.month_year_picker, null);
        builder.setView(dialogView);

        final android.widget.NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        final android.widget.NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentDate.get(Calendar.MONTH) + 1);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear - 10);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentDate.get(Calendar.YEAR));

        builder.setPositiveButton("OK", (dialog, which) -> {
            currentDate.set(Calendar.YEAR, yearPicker.getValue());
            currentDate.set(Calendar.MONTH, monthPicker.getValue() - 1);
            if (selectedDay == -1 || selectedDay > currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                selectedDay = 1;
            }
            currentDate.set(Calendar.DAY_OF_MONTH, selectedDay);
            updateCalendar();
        });
        builder.setNegativeButton("Cancel", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Set dialog button text colors based on dark mode
        int buttonTextColor = isDarkMode ? Color.WHITE : Color.BLACK;
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(buttonTextColor); // "OK" button
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(buttonTextColor); // "Cancel" button
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

    private void scheduleNotification(String taskContent, String deadline, String fromTime) {
        try {
            // 解析日期和时间
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            
            // 组合日期和时间
            String dateTimeString = deadline + " " + fromTime;
            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date taskDateTime = fullFormat.parse(dateTimeString);
            
            if (taskDateTime != null) {
                // 创建通知意图
                Intent notificationIntent = new Intent(this, NotificationReceiver.class);
                notificationIntent.putExtra("taskContent", taskContent);
                
                // 创建PendingIntent
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    (int) System.currentTimeMillis(), // 使用当前时间作为请求码
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                // 设置AlarmManager
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                
                // 设置精确时间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            taskDateTime.getTime(),
                            pendingIntent
                        );
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        taskDateTime.getTime(),
                        pendingIntent
                    );
                }
                
                Toast.makeText(this, "notify set", Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            Log.e("EditActivity", "Error scheduling notification: " + e.getMessage());
            Toast.makeText(this, "notify set error", Toast.LENGTH_SHORT).show();
        }
    }
}