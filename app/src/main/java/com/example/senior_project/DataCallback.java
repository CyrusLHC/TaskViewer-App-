package com.example.senior_project;

import java.util.List;

public interface DataCallback<T> {
    void onDataReceived(List<T> dataList);
}