package com.example.senior_project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;
import java.util.List;

public class TaskBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_TASK_LIST = "task_list";

    public static TaskBottomSheetDialog newInstance(List<TaskData> tasks) {
        TaskBottomSheetDialog fragment = new TaskBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK_LIST, (Serializable) tasks);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_bottom_sheet, container, false);
        LinearLayout taskContainer = view.findViewById(R.id.taskContainer);

        if (getArguments() != null) {
            List<TaskData> tasks = (List<TaskData>) getArguments().getSerializable(ARG_TASK_LIST);
            for (TaskData task : tasks) {
                View taskView = inflater.inflate(R.layout.item_task, taskContainer, false);
                TextView taskContentTextView = taskView.findViewById(R.id.taskContentTextView);
                TextView taskTimeTextView = taskView.findViewById(R.id.taskTimeTextView);

                taskContentTextView.setText(task.getTaskContent());
                taskTimeTextView.setText("From: " + task.getFromTime() + " To: " + task.getToTime());

                taskContainer.addView(taskView);
            }
        }

        return view;
    }
}