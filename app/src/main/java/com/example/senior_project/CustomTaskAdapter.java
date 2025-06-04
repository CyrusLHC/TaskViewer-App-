package com.example.senior_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;
import java.util.List;
import java.util.ArrayList;

public class CustomTaskAdapter extends ArrayAdapter<TaskData> {
    private boolean isSelectable = false;
    private List<TaskData> selectedTasks;
    private Context context;
    private List<TaskData> taskList;
    private boolean isDarkMode; // Add dark mode flag

    public CustomTaskAdapter(@NonNull Context context, @NonNull List<TaskData> objects, List<TaskData> selectedTasks) {
        super(context, R.layout.item_task, objects);
        this.selectedTasks = selectedTasks;
        this.context = context;
        this.taskList = objects;
        // Initialize dark mode state from SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("SharedPrefs", Context.MODE_PRIVATE);
        this.isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
    }

    public void setSelectable(boolean selectable) {
        this.isSelectable = selectable;
        notifyDataSetChanged();
    }

    public void updateTaskList(List<TaskData> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged();
    }

    // Add a method to update dark mode state
    public void setDarkMode(boolean isDark) {
        this.isDarkMode = isDark;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        }

        TaskData task = taskList.get(position);
        TextView importanceDisplay = convertView.findViewById(R.id.importanceDisplay);
        TextView taskContentTextView = convertView.findViewById(R.id.taskContentTextView);
        TextView taskTimeTextView = convertView.findViewById(R.id.taskTimeTextView);
        TextView fromTimeTextView = convertView.findViewById(R.id.fromTimeTextView);
        TextView toTimeTextView = convertView.findViewById(R.id.toTimeTextView);

        if (task != null) {
            importanceDisplay.setText(task.getImportanceDisplay());
            taskContentTextView.setText(task.getTaskContent());
            taskTimeTextView.setText("Deadline: " + task.getDeadline());
            fromTimeTextView.setText("From: " + task.getFromTime());
            toTimeTextView.setText("To: " + task.getToTime());

            // Highlight selected tasks
            if (isSelectable && selectedTasks.contains(task)) {
                convertView.setBackgroundColor(Color.LTGRAY); // Change background for selected
            } else {
                convertView.setBackgroundColor(Color.TRANSPARENT); // Default background
            }

            // Set text color based on dark mode
            int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
            importanceDisplay.setTextColor(Color.RED); // Keep importance in red for visibility
            taskContentTextView.setTextColor(textColor);
            taskTimeTextView.setTextColor(textColor);
            fromTimeTextView.setTextColor(textColor);
            toTimeTextView.setTextColor(textColor);
        }

        convertView.setOnClickListener(v -> {
            if (isSelectable) {
                if (selectedTasks.contains(task)) {
                    selectedTasks.remove(task);
                } else {
                    selectedTasks.add(task);
                }
                notifyDataSetChanged();
            }
        });

        return convertView;
    }
}