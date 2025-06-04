package com.example.senior_project;

public class TaskData {
    private String id;            // Unique ID of the task (for Firebase key reference)
    private String taskContent;   // Content of the task
    private String deadline;      // Deadline for the task
    private String fromTime;      // Start time for the task
    private String toTime;        // End time for the task
    private boolean isImportant;  // Importance flag for the task
    private String type;          // Type of task (e.g., deadline, important)
    private String importanceDisplay; // Importance display icon

    // Default constructor required for Firebase deserialization
    public TaskData() {
    }

    // Parameterized constructor to initialize TaskData
    public TaskData(String id, String taskContent, String deadline, String fromTime, String toTime, boolean isImportant, String importanceDisplay, String type) {
        this.id = id;
        this.taskContent = taskContent;
        this.deadline = deadline;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.isImportant = isImportant;
        this.importanceDisplay = importanceDisplay;
        this.type = type != null ? type : "general";  // Ensure no null value for type
    }

    // Getter and setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getFromTime() {
        return fromTime;
    }

    public void setFromTime(String fromTime) {
        this.fromTime = fromTime;
    }

    public String getToTime() {
        return toTime;
    }

    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    public String getImportanceDisplay() {
        return isImportant ? "★" : "☆";
    }

    public void setImportanceDisplay(String importanceDisplay) {
        this.importanceDisplay = importanceDisplay;
    }

    public String getType() {
        return type != null ? type : "general";  // Avoid returning null
    }

    public void setType(String type) {
        this.type = type;
    }

    // Method to return a string representation of the TaskData object
    @Override
    public String toString() {
        return "TaskData{" +
                "id='" + id + '\'' +
                ", taskContent='" + taskContent + '\'' +
                ", deadline='" + deadline + '\'' +
                ", fromTime='" + fromTime + '\'' +
                ", toTime='" + toTime + '\'' +
                ", isImportant=" + isImportant +
                ", type='" + type + '\'' +
                '}';
    }
}
