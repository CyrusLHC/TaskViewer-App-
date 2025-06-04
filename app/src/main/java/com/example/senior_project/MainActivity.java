package com.example.senior_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private Switch darkModeSwitch;
    private SharedPreferences sharedPreferences;
    private Button editButton, viewButton, btnSignOut, btnBack;
    private boolean isDarkMode;
    private GridView calendarGrid;
    private TextView monthYearText;
    private FirebaseAuth mAuth;
    private List<TaskData> dataList;
    private Calendar currentDate;
    private ImageView dropdownArrow;
    private DataManager dataManager;
    private String calendarId;
    private CalendarAdapter calendarAdapter;
    private ConstraintLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        // Retrieve calendar ID from intent or shared preferences
        calendarId = getIntent().getStringExtra("calendarId");
        if (calendarId == null) {
            calendarId = sharedPreferences.getString("lastCalendarId", null);
        }

        if (calendarId == null) {
            Toast.makeText(this, "Calendar ID is missing. Redirecting to login.", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Retrieve data from Firebase
        dataManager = DataManager.getInstance();
        dataManager.getTasksForCalendar(calendarId, taskList -> {
            if (taskList != null && !taskList.isEmpty()) {
                dataList = taskList;
                Log.d("MainActivity", "Data List retrieved: " + dataList);
            } else {
                Log.d("MainActivity", "No data retrieved for calendar ID: " + calendarId);
            }
            updateCalendar();
        });

        // Update UI elements based on the current theme
        updateThemeColors(isDarkMode);

        darkModeSwitch.setChecked(isDarkMode);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDarkMode = isChecked;
            sharedPreferences.edit().putBoolean("isDarkMode", isChecked).apply();
            updateThemeColors(isDarkMode);
            calendarAdapter.setDarkMode(isDarkMode);
            recreate(); // Recreate the activity to apply theme changes
        });

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditActivity.class);
            intent.putExtra("calendarId", calendarId);
            intent.putExtra("originActivity", "MainActivity");
            startActivity(intent);
        });
        viewButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewDataActivity.class);
            intent.putExtra("calendarId", calendarId);
            startActivity(intent);
        });
        btnSignOut.setOnClickListener(v -> signOut());
        btnBack.setOnClickListener(v -> navigateToPickCalendar());

        LinearLayout monthYearLayout = findViewById(R.id.monthYearLayout);
        monthYearLayout.setOnClickListener(v -> showMonthYearPicker());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setImageBitmap(textAsBitmap("AI", 40, isDarkMode ? Color.WHITE : Color.BLACK));
        fab.setOnClickListener(v -> {
            AddScheduleBottomSheetDialog bottomSheetDialog = AddScheduleBottomSheetDialog.newInstance(calendarId, "MainActivity");
            bottomSheetDialog.show(getSupportFragmentManager(), "AddScheduleBottomSheetDialog");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataManager.getTasksForCalendar(calendarId, taskList -> {
            if (taskList != null && !taskList.isEmpty()) {
                dataList = taskList;
                Log.d("MainActivity", "Data List refreshed: " + dataList);
            } else {
                Log.d("MainActivity", "No data retrieved for calendar ID: " + calendarId);
            }
            updateCalendar();
        });
    }

    private void initializeViews() {
        monthYearText = findViewById(R.id.monthYearText);
        calendarGrid = findViewById(R.id.calendarGrid);
        darkModeSwitch = findViewById(R.id.switch1);
        editButton = findViewById(R.id.btnEdit);
        viewButton = findViewById(R.id.btnView);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnBack = findViewById(R.id.btnBack);
        dropdownArrow = findViewById(R.id.dropdownArrow);
        mainLayout = findViewById(R.id.mainLayout);

        currentDate = Calendar.getInstance();
    }

    private void updateThemeColors(boolean isDark) {
        // Update the background color of the main layout
        mainLayout.setBackgroundColor(isDark ? Color.BLACK : Color.WHITE);

        // Update text and button colors
        monthYearText.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        darkModeSwitch.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        editButton.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        viewButton.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        btnSignOut.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        btnBack.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        int buttonBackgroundColor = isDark ? Color.DKGRAY : Color.LTGRAY;
        editButton.setBackgroundColor(buttonBackgroundColor);
        viewButton.setBackgroundColor(buttonBackgroundColor);
        btnSignOut.setBackgroundColor(buttonBackgroundColor);
        btnBack.setBackgroundColor(buttonBackgroundColor);
        dropdownArrow.setColorFilter(isDark ? Color.WHITE : Color.BLACK);

        // Refresh the calendar border by reapplying the drawable
        calendarGrid.setBackgroundResource(R.drawable.calendar_border);
    }
    private void updateCalendar() {
        String monthYear = String.format("%d / %d", currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH) + 1);
        monthYearText.setText(monthYear);

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
            calendarAdapter = new CalendarAdapter(this, daysList, currentDate, dataList, -1);
            calendarAdapter.setDarkMode(isDarkMode);
            calendarGrid.setAdapter(calendarAdapter);
        } else {
            calendarAdapter.updateData(daysList, currentDate, dataList);
            calendarAdapter.setDarkMode(isDarkMode);
            calendarAdapter.notifyDataSetChanged();
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToPickCalendar() {
        Intent intent = new Intent(MainActivity.this, PickCalendar.class);
        startActivity(intent);
        finish();
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
    private Bitmap textAsBitmap(String text, float textSize, int textColor) {
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