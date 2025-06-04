package com.example.senior_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PickCalendar extends AppCompatActivity implements CalendarOptionsBottomSheetDialog.OnCalendarDeletedListener {

    private ListView calendarListView;
    private TextView emptyView;
    private Button addCalendarButton;
    private Button signOutButton;
    private ArrayAdapter<String> adapter;
    private List<String> calendarNames;
    private List<String> calendarIds;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_calendar);

        // Initialize UI components
        calendarListView = findViewById(R.id.calendarListView);
        emptyView = findViewById(R.id.emptyView);
        addCalendarButton = findViewById(R.id.addCalendarButton);
        signOutButton = findViewById(R.id.signOutButton);

        // Initialize the list and adapter
        calendarNames = new ArrayList<>();
        calendarIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, calendarNames);
        calendarListView.setAdapter(adapter);
        calendarListView.setEmptyView(emptyView);

        // Initialize DataManager
        dataManager = DataManager.getInstance();

        // Load calendars for the current user
        loadUserCalendars();

        // Set up the add button
        setupAddButton();

        // Set up item click listener
        setupCalendarClickListener();

        // Set up the sign out button
        signOutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(PickCalendar.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("PickCalendar", "Resuming activity, refreshing calendar list.");
        loadUserCalendars();  // Refresh calendar list when returning to the screen
    }

    public void loadUserCalendars() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            dataManager.getCalendars(userId, calendarList -> {
                calendarNames.clear();
                calendarIds.clear();
                for (CalendarData calendar : calendarList) {
                    calendarNames.add(calendar.getCalendarName());
                    calendarIds.add(calendar.getCalendarId());
                }
                adapter.notifyDataSetChanged();
                Log.d("PickCalendar", "Calendar list refreshed with " + calendarNames.size() + " items.");
            });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAddButton() {
        addCalendarButton.setOnClickListener(v -> {
            CalendarBottomSheetDialog bottomSheetDialog = new CalendarBottomSheetDialog();
            bottomSheetDialog.show(getSupportFragmentManager(), "CalendarBottomSheetDialog");

            bottomSheetDialog.setOnCalendarSelectedListener(calendarId -> {
                loadUserCalendars(); // Refresh the calendar list
                Log.d("PickCalendar", "New calendar added or joined. Refreshing list...");
            });
        });
    }


    private void setupCalendarClickListener() {
        calendarListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCalendarId = calendarIds.get(position);
            SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("lastCalendarId", selectedCalendarId);
            editor.apply();

            Log.d("PickCalendar", "Selected calendar: " + selectedCalendarId);

            Intent intent = new Intent(PickCalendar.this, MainActivity.class);
            intent.putExtra("calendarId", selectedCalendarId);
            startActivity(intent);
            finish();
        });

        calendarListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedCalendarId = calendarIds.get(position);
            CalendarOptionsBottomSheetDialog bottomSheetDialog = CalendarOptionsBottomSheetDialog.newInstance(selectedCalendarId);
            bottomSheetDialog.setOnCalendarDeletedListener(this);
            bottomSheetDialog.show(getSupportFragmentManager(), "CalendarOptionsBottomSheetDialog");
            return true; // Indicate that the long press was handled
        });
    }

    @Override
    public void onCalendarDeleted() {
        Log.d("PickCalendar", "Calendar deleted, refreshing list.");
        loadUserCalendars(); // Refresh the calendar list
    }
}
