package com.example.senior_project;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddScheduleBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String TAG = "AddScheduleBottomSheetDialog";
    private static final String ARG_CALENDAR_ID = "calendar_id";
    private static final String ARG_ORIGIN_ACTIVITY = "origin_activity";

    public interface OnScheduleOptionSelectedListener {
        void onAddScheduleByText();
        void onAddScheduleByVoice();
        void onTaskParsed(TaskData taskData, boolean updateCalendar);
    }

    private OnScheduleOptionSelectedListener listener;
    private String calendarId;
    private String originActivity;
    private DatabaseReference databaseReference;
    private Context appContext;

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private EditText voiceInputText;
    private AlertDialog voiceDialog;
    private AlertDialog textDialog;

    public static AddScheduleBottomSheetDialog newInstance(String calendarId, String originActivity) {
        AddScheduleBottomSheetDialog fragment = new AddScheduleBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CALENDAR_ID, calendarId);
        args.putString(ARG_ORIGIN_ACTIVITY, originActivity);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnScheduleOptionSelectedListener(OnScheduleOptionSelectedListener listener) {
        this.listener = listener;
    }

    private static final String API_KEY = "sk-7896ed6d903d4344b68bad8cf7aeabac";
    private static final String BASE_URL = "http://10.0.2.2:8080";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            calendarId = getArguments().getString(ARG_CALENDAR_ID);
            originActivity = getArguments().getString(ARG_ORIGIN_ACTIVITY);
        }
        databaseReference = FirebaseDatabase.getInstance("https://com4101-senior-project-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("tasks");
        appContext = getContext() != null ? getContext().getApplicationContext() : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_schedule, container, false);

        Button addTextButton = view.findViewById(R.id.addTextButton);
        Button addVoiceButton = view.findViewById(R.id.addVoiceButton);

        addTextButton.setOnClickListener(v -> showAddScheduleTextDialog());

        addVoiceButton.setOnClickListener(v -> {
            checkPermission();
            showVoiceInputDialog();
        });

        return view;
    }

    private void showVoiceInputDialog() {
        if (getActivity() == null || getActivity().isFinishing()) {
            Log.w(TAG, "Activity is null or finishing, cannot show voice dialog");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.voice_input, null);

        EditText editTextScheduleContent = dialogView.findViewById(R.id.voiceInputText);
        CheckBox checkBoxDoubleCheck = dialogView.findViewById(R.id.checkBoxDoubleCheck);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonAdd = dialogView.findViewById(R.id.buttonAdd);
        ProgressBar progressBarLoading = dialogView.findViewById(R.id.progressBarLoading);
        Button recordButton = dialogView.findViewById(R.id.recordButton);

        voiceDialog = builder.setView(dialogView)
                .setCancelable(true)
                .create();

        voiceInputText = editTextScheduleContent;

        recordButton.setOnClickListener(v -> {
            if (!editTextScheduleContent.getText().toString().isEmpty()) {
                editTextScheduleContent.setText("");
            }
            startVoiceRecognition();
        });

        buttonCancel.setOnClickListener(v -> {
            voiceDialog.dismiss();
            dismiss();
        });

        buttonAdd.setOnClickListener(v -> {
            String scheduleContent = editTextScheduleContent.getText().toString().trim();
            if (!scheduleContent.isEmpty()) {
                Log.d(TAG, "Voice schedule content: " + scheduleContent);
                progressBarLoading.setVisibility(View.VISIBLE);
                buttonAdd.setEnabled(false);
                requestChatCompletion(scheduleContent, voiceDialog, checkBoxDoubleCheck.isChecked(), progressBarLoading, buttonAdd);
            } else {
                editTextScheduleContent.setError("Please record something first");
            }
        });

        voiceDialog.show();
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Your device doesn't support Speech to Text", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddScheduleTextDialog() {
        if (getActivity() == null || getActivity().isFinishing()) {
            Log.w(TAG, "Activity is null or finishing, cannot show text dialog");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_schedule_text, null);
        builder.setView(dialogView);

        EditText editTextScheduleContent = dialogView.findViewById(R.id.editTextScheduleContent);
        CheckBox checkBoxDoubleCheck = dialogView.findViewById(R.id.checkBoxDoubleCheck);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonAdd = dialogView.findViewById(R.id.buttonAdd);
        ProgressBar progressBarLoading = dialogView.findViewById(R.id.progressBarLoading);

        textDialog = builder.create();

        buttonCancel.setOnClickListener(v -> {
            textDialog.dismiss();
            dismiss();
        });

        buttonAdd.setOnClickListener(v -> {
            String scheduleContent = editTextScheduleContent.getText().toString().trim();
            if (!scheduleContent.isEmpty()) {
                Log.d(TAG, "Text schedule content: " + scheduleContent);
                progressBarLoading.setVisibility(View.VISIBLE);
                buttonAdd.setEnabled(false);
                requestChatCompletion(scheduleContent, textDialog, checkBoxDoubleCheck.isChecked(), progressBarLoading, buttonAdd);
            } else {
                editTextScheduleContent.setError("Please enter schedule details");
            }
        });

        textDialog.show();
    }

    private void requestChatCompletion(String query, AlertDialog dialog, boolean doubleCheck, ProgressBar progressBarLoading, Button buttonAdd) {
        String url = BASE_URL + "/api/chat/completions";
        JSONObject json = new JSONObject();

        LocalDate currentDate = LocalDate.now();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String taskId = databaseReference.child(calendarId).push().getKey();

        try {
            json.put("model", "deepseek-r1:8b");
            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            String systemPrompt = "Transform the user's sentence into the following structured format:\n\n" +
                    "Current Date: " + formattedDate + "\n\n" +
                    "Task ID: " + taskId + "\n\n" +
                    "Input Sentence:\n\n\"I have an English exam tomorrow at 3:30 pm to 6:00 pm.\"\n\n" +
                    "Output Format:\n\n" +
                    "TaskData{\n" +
                    "  id='[use the provided Task ID]',\n" +
                    "  taskContent='user_input',\n" +
                    "  deadline='user_input',\n" +
                    "  fromTime='user_input',\n" +
                    "  toTime='user_input',\n" +
                    "  isImportant=user_input,\n" +
                    "  type='user_input'\n" +
                    "}\n\n" +
                    "Instructions:\n" +
                    "Use the Current Date provided above as a reference to interpret relative dates (e.g., 'tomorrow' is Current Date + 1 day).\n" +
                    "Use the Task ID provided above for the id field.\n" +
                    "Replace user_input with relevant details extracted from the input sentence.\n" +
                    "For taskContent, extract the task description (e.g., 'English exam').\n" +
                    "For deadline, extract the explicit date if provided (e.g., '27 April' means '2025-04-27' if in the future relative to Current Date), or calculate based on relative terms (e.g., 'tomorrow' means Current Date + 1 day). Format as YYYY-MM-DD.\n" +
                    "For fromTime and toTime, extract the times and convert to 24-hour format (HH:MM). For example, '3:30 pm' becomes '15:30', '12:00 pm' becomes '12:00'.\n" +
                    "For isImportant, set to true if the task seems critical (e.g., exams, rehearsals, deadlines) or false otherwise.\n" +
                    "For type, set to 'important' if isImportant is true, otherwise set to 'deadline'.\n" +
                    "Ensure all fields are populated accurately, and times are in strict 24-hour HH:MM format.\n\n" +
                    "Example Output (assuming Current Date is 2025-03-05 and Task ID is -OKVPcCbbhNbSEyTemqk):\n\n" +
                    "TaskData{\n" +
                    "  id='-OKVPcCbbhNbSEyTemqk',\n" +
                    "  taskContent='English exam',\n" +
                    "  deadline='2025-03-06',\n" +
                    "  fromTime='15:30',\n" +
                    "  toTime='18:00',\n" +
                    "  isImportant=true,\n" +
                    "  type='important'\n" +
                    "}";
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.put(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            String userContent = "Input Sentence:\n\n\"" + query + "\"";
            userMsg.put("content", userContent);
            messages.put(userMsg);

            json.put("messages", messages);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Error: " + e.getMessage());
            handleError(dialog, "JSON Error: " + e.getMessage(), progressBarLoading, buttonAdd);
            dismiss();
            return;
        }

        Log.d(TAG, "Request JSON: " + json.toString());

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        postWithRetry(request, dialog, doubleCheck, progressBarLoading, buttonAdd, 5);
    }

    private void postWithRetry(Request request, AlertDialog dialog, boolean doubleCheck, ProgressBar progressBarLoading, Button buttonAdd, int maxRetries) {
        final int[] retryCount = {0};
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (e instanceof SocketTimeoutException && retryCount[0] < maxRetries) {
                    retryCount[0]++;
                    Log.w(TAG, "Request timed out. Retry attempt " + retryCount[0] + " of " + maxRetries);
                    mainHandler.postDelayed(() -> {
                        client.newCall(request).enqueue(this);
                    }, 1000);
                } else {
                    String errorMessage = e instanceof SocketTimeoutException ?
                            "Request timed out after " + maxRetries + " retries. Please check your network." :
                            "Request failed: " + e.getMessage();
                    Log.e(TAG, errorMessage);
                    mainHandler.post(() -> handleError(dialog, errorMessage, progressBarLoading, buttonAdd));
                    mainHandler.post(() -> dismiss());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response data: " + responseData);

                if (getActivity() == null || getActivity().isFinishing()) {
                    Log.e(TAG, "Activity is null or finishing in onResponse, cannot update UI");
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        dismiss();
                    });
                    return;
                }

                mainHandler.post(() -> {
                    progressBarLoading.setVisibility(View.GONE);
                    buttonAdd.setEnabled(true);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() == 0) {
                            throw new JSONException("No choices in response");
                        }
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        String content = message.getString("content");

                        Log.d(TAG, "Content extracted: " + content);

                        int taskDataStart = content.indexOf("TaskData{");
                        if (taskDataStart == -1) {
                            throw new JSONException("TaskData block not found");
                        }
                        String taskDataStr = content.substring(taskDataStart);
                        Log.d(TAG, "TaskData block: " + taskDataStr);

                        String id = extractValue(taskDataStr, "id='", "'");
                        String taskContent = extractValue(taskDataStr, "taskContent='", "'");
                        String deadline = extractValue(taskDataStr, "deadline='", "'");
                        String fromTime = extractValue(taskDataStr, "fromTime='", "'");
                        String toTime = extractValue(taskDataStr, "toTime='", "'");
                        String isImportant = extractValue(taskDataStr, "isImportant=", ",");
                        String type = extractValue(taskDataStr, "type='", "'");

                        // Normalize time to 24-hour format
                        if (!isValid24HourFormat(fromTime)) {
                            Log.w(TAG, "Invalid 24-hour format for fromTime: " + fromTime + ". Attempting to convert.");
                            fromTime = normalizeTimeTo24Hour(fromTime);
                        }
                        if (!isValid24HourFormat(toTime)) {
                            Log.w(TAG, "Invalid 24-hour format for toTime: " + toTime + ". Attempting to convert.");
                            toTime = normalizeTimeTo24Hour(toTime);
                        }

                        // Convert to 12-hour format for display in EditActivity
                        String fromTime12 = convertTo12HourFormat(fromTime);
                        String toTime12 = convertTo12HourFormat(toTime);

                        Log.d(TAG, "Parsed - id: " + id + ", taskContent: " + taskContent +
                                ", deadline: " + deadline + ", fromTime: " + fromTime +
                                ", toTime: " + toTime + ", fromTime12: " + fromTime12 +
                                ", toTime12: " + toTime12 + ", isImportant: " + isImportant +
                                ", type: " + type);

                        TaskData taskData = new TaskData(
                                id, taskContent, deadline, fromTime12, toTime12,
                                Boolean.parseBoolean(isImportant),
                                Boolean.parseBoolean(isImportant) ? "★" : "",
                                type
                        );

                        if (doubleCheck && listener != null) {
                            listener.onTaskParsed(taskData, false);
                            Log.d(TAG, "Task passed to listener for double-check with 12-hour format: " + fromTime12 + " to " + toTime12);
                            dialog.dismiss();
                            dismiss();
                        } else {
                            databaseReference.child(calendarId).child(id).setValue(taskData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Task saved to Firebase: " + id + " with times " + fromTime12 + " to " + toTime12);
                                        if (appContext != null) {
                                            Toast.makeText(appContext, "Schedule added successfully!", Toast.LENGTH_SHORT).show();
                                        }
                                        dialog.dismiss();
                                        dismiss();
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        startActivity(intent);
                                        getActivity().finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to save task: " + e.getMessage());
                                        if (appContext != null) {
                                            Toast.makeText(appContext, "Failed to save to Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        dialog.dismiss();
                                        dismiss();
                                    });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response: " + e.getMessage(), e);
                        if (appContext != null) {
                            Toast.makeText(appContext, "Error processing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                        dismiss();
                    }
                });
            }
        };

        client.newCall(request).enqueue(callback);
    }

    private boolean isValid24HourFormat(String time) {
        if (time == null || time.isEmpty()) return false;
        return time.matches("\\d{2}:\\d{2}") &&
                Integer.parseInt(time.split(":")[0]) <= 23 &&
                Integer.parseInt(time.split(":")[1]) <= 59;
    }

    private String normalizeTimeTo24Hour(String time) {
        if (time == null || time.isEmpty()) return "00:00";
        try {
            // Handle 12-hour format with AM/PM
            if (time.matches("\\d{1,2}:\\d{2}\\s*[APap][Mm]")) {
                SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
                SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date = sdf12.parse(time.trim());
                return sdf24.format(date);
            }
            // Handle plain HH:MM format
            if (isValid24HourFormat(time)) {
                return time;
            }
            // Fallback for invalid formats
            Log.w(TAG, "Invalid time format: " + time + ". Attempting to parse.");
            SimpleDateFormat sdfFlexible = new SimpleDateFormat("h:mm", Locale.getDefault());
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = sdfFlexible.parse(time.trim());
            return sdf24.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to normalize time: " + time + ". Error: " + e.getMessage());
            return "00:00";
        }
    }

    private String convertTo12HourFormat(String time24) {
        if (!isValid24HourFormat(time24)) {
            Log.w(TAG, "Time '" + time24 + "' is not in valid 24-hour format. Attempting to parse.");
            try {
                SimpleDateFormat sdfFlexible = new SimpleDateFormat("h:mm", Locale.getDefault());
                SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
                Date date = sdfFlexible.parse(time24);
                return sdf12.format(date);
            } catch (ParseException e) {
                Log.e(TAG, "Failed to convert time to 12-hour format: " + time24 + ". Error: " + e.getMessage());
                return time24;
            }
        }
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = sdf24.parse(time24);
            return sdf12.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to convert time to 12-hour format: " + time24 + ". Error: " + e.getMessage());
            return time24;
        }
    }

    private void handleError(AlertDialog dialog, String errorMessage, ProgressBar progressBarLoading, Button buttonAdd) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            progressBarLoading.setVisibility(View.GONE);
            buttonAdd.setEnabled(true);
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        } else if (appContext != null) {
            Toast.makeText(appContext, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private String extractValue(String text, String startDelimiter, String endDelimiter) {
        try {
            int startIndex = text.indexOf(startDelimiter);
            if (startIndex == -1) {
                Log.e(TAG, "Start delimiter '" + startDelimiter + "' not found in: " + text);
                return "";
            }
            startIndex += startDelimiter.length();
            int endIndex = text.indexOf(endDelimiter, startIndex);
            if (endIndex == -1) {
                Log.e(TAG, "End delimiter '" + endDelimiter + "' not found in: " + text);
                return "";
            }
            String value = text.substring(startIndex, endIndex).trim();
            Log.d(TAG, "Extracted value for '" + startDelimiter + "': " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting value between '" + startDelimiter + "' and '" + endDelimiter + "': " + e.getMessage());
            return "";
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty() && voiceInputText != null) {
                String currentText = voiceInputText.getText().toString();
                String newText = currentText.isEmpty() ? matches.get(0) : currentText + "\n" + matches.get(0);
                voiceInputText.setText(newText);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (voiceDialog != null && voiceDialog.isShowing()) {
            voiceDialog.dismiss();
        }
        if (textDialog != null && textDialog.isShowing()) {
            textDialog.dismiss();
        }
    }
}
