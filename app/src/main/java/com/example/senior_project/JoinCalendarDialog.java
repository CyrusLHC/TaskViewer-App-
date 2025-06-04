package com.example.senior_project;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class JoinCalendarDialog {

    private Dialog dialog;
    private EditText editTextCalendarId;

    public interface OnCalendarJoinedListener {
        void onCalendarJoined();
    }

    private OnCalendarJoinedListener joinListener;

    public void setOnCalendarJoinedListener(OnCalendarJoinedListener listener) {
        this.joinListener = listener;
    }

    public JoinCalendarDialog(Context context, DataManager dataManager) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_import);
        editTextCalendarId = dialog.findViewById(R.id.editTextImportData);

        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonJoin = dialog.findViewById(R.id.buttonImport);

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonJoin.setOnClickListener(v -> {
            String calendarId = editTextCalendarId.getText().toString().trim();
            if (!calendarId.isEmpty()) {
                dataManager.joinCalendar(calendarId, success -> {
                    if (success) {
                        Toast.makeText(dialog.getContext(), "Successfully joined the calendar!", Toast.LENGTH_SHORT).show();
                        if (joinListener != null) {
                            joinListener.onCalendarJoined();
                        }
                    } else {
                        Toast.makeText(dialog.getContext(), "Failed to join the calendar. Please check the ID.", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.dismiss();
            } else {
                Toast.makeText(context, "Please enter a calendar ID.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void show() {
        dialog.show();
    }

    private void joinCalendar(String calendarId, DataManager dataManager) {
        dataManager.joinCalendar(calendarId, success -> {
            if (success) {
                Toast.makeText(dialog.getContext(), "Successfully joined the calendar!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(dialog.getContext(), "Failed to join the calendar. Please check the ID.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}