package com.example.senior_project;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CalendarOptionsBottomSheetDialog extends BottomSheetDialogFragment {

    private String calendarId;
    private static final String TAG = "CalendarOptions";
    private ProgressDialog progressDialog;

    public static CalendarOptionsBottomSheetDialog newInstance(String calendarId) {
        CalendarOptionsBottomSheetDialog fragment = new CalendarOptionsBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString("calendarId", calendarId);
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnCalendarDeletedListener {
        void onCalendarDeleted();
    }

    private OnCalendarDeletedListener deleteListener;

    public void setOnCalendarDeletedListener(OnCalendarDeletedListener listener) {
        this.deleteListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            calendarId = getArguments().getString("calendarId");
            Log.d(TAG, "Loaded calendarId: " + calendarId);
        }
        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Deleting calendar...");
        progressDialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_options_bottom_sheet, container, false);

        Button shareButton = view.findViewById(R.id.shareButton);
        Button deleteForMeButton = view.findViewById(R.id.deleteForMeButton);
        Button deleteForEveryoneButton = view.findViewById(R.id.deleteForEveryoneButton);

        shareButton.setOnClickListener(v -> shareCalendarId());
        deleteForMeButton.setOnClickListener(v -> showDeleteConfirmation(false));
        deleteForEveryoneButton.setOnClickListener(v -> showDeleteConfirmation(true));

        return view;
    }

    private void shareCalendarId() {
        Log.d(TAG, "Sharing calendarId: " + calendarId);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, calendarId);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Calendar ID via"));
    }

    private void deleteCalendarForUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userCalendarsRef = FirebaseDatabase.getInstance()
                .getReference("tasks/calendars").child(userId);

        progressDialog.show();
        userCalendarsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean calendarFound = false;
                for (DataSnapshot calendarSnapshot : snapshot.getChildren()) {
                    String storedCalendarId = calendarSnapshot.child("calendarId").getValue(String.class);
                    if (storedCalendarId != null && storedCalendarId.equals(calendarId)) {
                        calendarSnapshot.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Calendar removed from your list.", Toast.LENGTH_SHORT).show();
                                    if (deleteListener != null) {
                                        deleteListener.onCalendarDeleted();
                                    }
                                    dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Failed to remove calendar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        calendarFound = true;
                        break;
                    }
                }
                if (!calendarFound) {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Calendar not found in your list.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e(TAG, "Firebase error: " + error.getMessage());
                Toast.makeText(getContext(), "Error accessing calendar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCalendarForEveryone() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference calendarsRef = FirebaseDatabase.getInstance().getReference("tasks/calendars");
        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("tasks").child(calendarId);

        progressDialog.show();
        Log.d(TAG, "Attempting to delete calendar with ID: " + calendarId);

        // Step 1: Delete all tasks associated with the calendar
        tasksRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Tasks deleted successfully for calendar: " + calendarId);
                // Step 2: Delete calendar references for all users
                calendarsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean calendarDeleted = false;
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            for (DataSnapshot calendarSnapshot : userSnapshot.getChildren()) {
                                String storedCalendarId = calendarSnapshot.child("calendarId").getValue(String.class);
                                if (storedCalendarId != null && storedCalendarId.equals(calendarId)) {
                                    String calendarKey = calendarSnapshot.getKey();
                                    Log.d(TAG, "Deleting calendar with key: " + calendarKey + " for user: " + userSnapshot.getKey());
                                    calendarSnapshot.getRef().removeValue()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Calendar reference deleted for user: " + userSnapshot.getKey());
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Failed to delete calendar reference for user: " + userSnapshot.getKey() + ", error: " + e.getMessage());
                                            });
                                    calendarDeleted = true;
                                }
                            }
                        }
                        progressDialog.dismiss();
                        if (calendarDeleted) {
                            Toast.makeText(getContext(), "Calendar and its contents deleted for everyone.", Toast.LENGTH_SHORT).show();
                            if (deleteListener != null) {
                                deleteListener.onCalendarDeleted();
                            }
                            dismiss();
                        } else {
                            Toast.makeText(getContext(), "Calendar not found.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Calendar with ID " + calendarId + " not found.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Failed to access calendars: " + error.getMessage());
                        Toast.makeText(getContext(), "Error deleting calendar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to delete tasks: " + task.getException().getMessage());
                Toast.makeText(getContext(), "Failed to delete calendar tasks: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(boolean deleteForEveryone) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Deletion")
                .setMessage(deleteForEveryone ? "Are you sure you want to delete this calendar for everyone?" : "Are you sure you want to remove this calendar from your list?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (deleteForEveryone) {
                        deleteCalendarForEveryone();
                    } else {
                        deleteCalendarForUser();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}