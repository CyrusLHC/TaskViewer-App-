package com.example.senior_project;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Random;

public class CalendarBottomSheetDialog extends BottomSheetDialogFragment {

    private OnCalendarSelectedListener listener;

    public interface OnCalendarSelectedListener {
        void onCalendarSelected(String calendarId);
    }

    public void setOnCalendarSelectedListener(OnCalendarSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar_bottom_sheet, container, false);

        Button createButton = view.findViewById(R.id.createCalendarButton);
        Button joinButton = view.findViewById(R.id.joinCalendarButton);

        createButton.setOnClickListener(v -> showCreateCalendarDialog());

        joinButton.setOnClickListener(v -> {
            JoinCalendarDialog joinCalendarDialog = new JoinCalendarDialog(getContext(), DataManager.getInstance());
            joinCalendarDialog.setOnCalendarJoinedListener(() -> {
                if (getActivity() instanceof PickCalendar) {
                    ((PickCalendar) getActivity()).loadUserCalendars();
                }
            });
            joinCalendarDialog.show();
            dismiss();
        });

        return view;
    }

    private void showCreateCalendarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_calendar, null);
        builder.setView(dialogView);

        EditText editTextCalendarName = dialogView.findViewById(R.id.editTextCalendarName);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonConfirm = dialogView.findViewById(R.id.buttonConfirm);

        AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            String calendarName = editTextCalendarName.getText().toString().trim();
            if (!calendarName.isEmpty()) {
                // Generate a random 16-digit ID
                String calendarId = String.format("%016d", new Random().nextLong() & Long.MAX_VALUE);

                // Create a new CalendarData object
                CalendarData calendarData = new CalendarData(calendarId, calendarName);

                // Add the calendar to Firebase
                DataManager.getInstance().addCalendar(calendarData);

                // Notify listener with the new calendar ID
                if (listener != null) {
                    listener.onCalendarSelected(calendarId);
                }
                dialog.dismiss();
                dismiss();
            } else {
                editTextCalendarName.setError("Calendar name cannot be empty");
            }
        });

        dialog.show();
    }
}