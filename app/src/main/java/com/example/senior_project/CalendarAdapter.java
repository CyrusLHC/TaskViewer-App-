package com.example.senior_project;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CalendarAdapter extends BaseAdapter {
    private boolean isDarkMode;
    private Context context;
    private List<String> daysList;
    private Calendar currentDate;
    private List<TaskData> taskList;
    private int selectedPosition = -1;
    private Map<Integer, String> taskContentMap = new HashMap<>();
    private int selectedDay;

    public CalendarAdapter(Context context, List<String> daysList, Calendar currentDate, List<TaskData> taskList, int selectedDay) {
        this.context = context;
        this.daysList = daysList;
        this.currentDate = currentDate;
        this.taskList = taskList;
        this.isDarkMode = false; // Initialize as false, will be set by the activity
        this.selectedDay = selectedDay;

        // Set initial selectedPosition based on selectedDay
        if (selectedDay != -1) {
            setSelectedDate(selectedDay);
        }
    }

    @Override
    public int getCount() {
        return daysList.size();
    }

    @Override
    public Object getItem(int position) {
        return daysList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View cellView;
        if (convertView == null) {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 200));

            TextView dayView = new TextView(context);
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dayView.setLayoutParams(dayParams);
            dayView.setGravity(Gravity.CENTER);
            dayView.setTextSize(16);
            dayView.setPadding(5, 5, 5, 5);
            dayView.setId(R.id.day_text_view);

            TextView taskView = new TextView(context);
            LinearLayout.LayoutParams taskParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            taskView.setLayoutParams(taskParams);
            taskView.setGravity(Gravity.CENTER);
            taskView.setTextSize(12);
            taskView.setPadding(5, 0, 5, 5);
            taskView.setId(R.id.task_text_view);

            layout.addView(dayView);
            layout.addView(taskView);

            cellView = layout;
        } else {
            cellView = convertView;
        }

        String day = daysList.get(position);
        TextView dayView = cellView.findViewById(R.id.day_text_view);
        TextView taskView = cellView.findViewById(R.id.task_text_view);

        dayView.setText(day);

        // Define default text and background colors based on dark mode
        int defaultTextColor = isDarkMode ? Color.WHITE : Color.BLACK;
        int defaultBackgroundColor = isDarkMode ? Color.BLACK : Color.WHITE;

        // Set default colors for the cell
        dayView.setTextColor(defaultTextColor);
        taskView.setTextColor(defaultTextColor);
        taskView.setVisibility(View.GONE);

        // Get current date
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentYear = today.get(Calendar.YEAR);

        // Determine the background color based on the cell's state
        int backgroundColor = defaultBackgroundColor;

        if (position < 7) {
            // Weekday headers (SUN, MON, etc.)
            backgroundColor = isDarkMode ? Color.DKGRAY : Color.LTGRAY;
            dayView.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
        } else if (TextUtils.isEmpty(day)) {
            // Empty cells before month starts
            backgroundColor = defaultBackgroundColor;
            dayView.setTextColor(defaultTextColor);
        } else {
            int dayInt = Integer.parseInt(day);
            boolean isCurrentDay = dayInt == currentDay &&
                    currentMonth == (currentDate.get(Calendar.MONTH) + 1) &&
                    currentYear == currentDate.get(Calendar.YEAR);

            // Process tasks for this day
            boolean hasImportant = false;
            boolean hasDeadline = false;
            StringBuilder taskContentBuilder = new StringBuilder();

            if (taskList != null) {
                for (TaskData task : taskList) {
                    if (matchesDate(task.getDeadline(), dayInt)) {
                        if (taskContentBuilder.length() > 0) {
                            taskContentBuilder.append("\n");
                        }
                        taskContentBuilder.append(task.getTaskContent());

                        String taskType = task.getType();
                        if ("important".equalsIgnoreCase(taskType)) {
                            hasImportant = true;
                        } else if ("deadline".equalsIgnoreCase(taskType)) {
                            hasDeadline = true;
                        }
                    }
                }
            }

            // Set task content
            if (taskContentBuilder.length() > 0) {
                taskView.setVisibility(View.VISIBLE);
                taskView.setText(taskContentBuilder.toString());
                taskView.setTextColor(defaultTextColor);
            }

            // Apply colors with priority: Selected > Current > Important > Deadline > No schedule
            if (position == selectedPosition) {
                backgroundColor = Color.parseColor("#ADD8E6"); // Light blue for selected
                dayView.setTextColor(Color.BLACK);
                taskView.setTextColor(Color.BLACK);
            } else if (isCurrentDay) {
                backgroundColor = Color.YELLOW;
                dayView.setTextColor(Color.BLACK);
                taskView.setTextColor(Color.BLACK);
            } else if (hasImportant) {
                backgroundColor = Color.parseColor("#FFB6C1"); // Light red
                dayView.setTextColor(Color.BLACK);
                taskView.setTextColor(Color.BLACK);
            } else if (hasDeadline) {
                backgroundColor = Color.parseColor("#90EE90"); // Light green
                dayView.setTextColor(Color.BLACK);
                taskView.setTextColor(Color.BLACK);
            } else {
                backgroundColor = defaultBackgroundColor;
                dayView.setTextColor(defaultTextColor);
                taskView.setTextColor(defaultTextColor);
            }
        }

        // Create a LayerDrawable to combine the background color and border
        Drawable colorDrawable = new ColorDrawable(backgroundColor);
        Drawable borderDrawable = null;
        try {
            int borderResId = isDarkMode ? R.drawable.calendar_cell_border_dark : R.drawable.calendar_cell_border_light;
            try {
                borderDrawable = ContextCompat.getDrawable(context, borderResId);
                if (borderDrawable == null) {
                    Log.e("CalendarAdapter", "Failed to load calendar_cell_border drawable");
                } else {
                    Log.d("CalendarAdapter", "Successfully loaded calendar_cell_border drawable");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("CalendarAdapter", "Drawable resource not found: " + e.getMessage());
                borderDrawable = new ColorDrawable(Color.TRANSPARENT); // Fallback
            }

            if (borderDrawable == null) {
                Log.e("CalendarAdapter", "Failed to load calendar_cell_border drawable");
            } else {
                Log.d("CalendarAdapter", "Successfully loaded calendar_cell_border drawable");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("CalendarAdapter", "Drawable resource not found: " + e.getMessage());
            // Fallback to a default border if the resource is not found
            borderDrawable = new ColorDrawable(Color.TRANSPARENT); // Or create a default shape programmatically
        }

        Drawable[] layers = {colorDrawable, borderDrawable};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // Apply the layered background to the cell
        cellView.setBackground(layerDrawable);

        // Click listener for date selection
        cellView.setOnClickListener(v -> {
            if (position >= 7 && !TextUtils.isEmpty(day)) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (context instanceof EditActivity) {
                    ((EditActivity) context).onDateSelected(Integer.parseInt(day));
                }
            }
        });

        return cellView;
    }
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setDarkMode(boolean isDark) {
        this.isDarkMode = isDark;
        notifyDataSetChanged();
    }

    public void updateData(List<String> newDaysList, Calendar newCurrentDate, List<TaskData> newTaskList) {
        this.daysList = newDaysList;
        this.currentDate = newCurrentDate;
        this.taskList = newTaskList;
        notifyDataSetChanged(); // Notify adapter to refresh the view
    }

    public void setSelectedDate(int day) {
        for (int i = 7; i < daysList.size(); i++) {
            if (daysList.get(i).equals(String.valueOf(day))) {
                selectedPosition = i;
                break;
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesDate(String deadline, int day) {
        try {
            String[] parts = deadline.split("[/-]");
            int taskYear = Integer.parseInt(parts[0]);
            int taskMonth = Integer.parseInt(parts[1]);
            int taskDay = Integer.parseInt(parts[2]);

            return taskYear == currentDate.get(Calendar.YEAR) &&
                    taskMonth == (currentDate.get(Calendar.MONTH) + 1) &&
                    taskDay == day;
        } catch (Exception e) {
            Log.e("CalendarAdapter", "Error matching date: " + e.getMessage());
            return false;
        }
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void addTaskContentForDay(int day, String taskContent) {
        taskContentMap.put(day, taskContent);
        notifyDataSetChanged();
    }
}