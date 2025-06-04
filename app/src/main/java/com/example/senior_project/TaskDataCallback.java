package com.example.senior_project;

import java.util.List;

public interface TaskDataCallback {
    void onDataReceived(List<TaskData> taskList);
}