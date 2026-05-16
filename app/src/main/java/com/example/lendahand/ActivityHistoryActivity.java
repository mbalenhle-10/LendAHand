package com.example.lendahand;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ActivityHistoryActivity extends AppCompatActivity {

    // Centralized URL management
    private static final String BASE_URL = "https://wmc.ms.wits.ac.za/students/s2562712/api/";
    private static final String SCRIPT_NAME = "activity_history.php";

    private final OkHttpClient client = new OkHttpClient();
    private LinearLayout historyContainer;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_history);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        historyContainer = findViewById(R.id.historyContainer);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadSimpleList();
    }

    private void loadSimpleList() {
        // Constructing the full URL dynamically
        String fullUrl = BASE_URL + SCRIPT_NAME;

        RequestBody body = new FormBody.Builder()
                .add("user_id", String.valueOf(userId))
                .build();

        Request request = new Request.Builder()
                .url(fullUrl)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ActivityHistoryActivity.this,
                        "Network Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;

                byte[] bytes = response.body().bytes();
                String raw = new String(bytes, "UTF-8");

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(raw);
                        JSONArray array = json.getJSONArray("transactions");
                        historyContainer.removeAllViews();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            addSimpleRow(
                                    obj.getString("item_name"),
                                    obj.getString("info"),
                                    obj.getString("date")
                            );
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("API_ERROR", "Parse Error: " + raw);
                    }
                });
            }
        });
    }

    private void addSimpleRow(String title, String info, String date) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        row.setBackgroundColor(0xFFFFFFFF);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFF2F3E46);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(info + " • " + date);
        tvDetails.setTextSize(13f);
        tvDetails.setTextColor(0xFF5A524A);

        row.addView(tvTitle);
        row.addView(tvDetails);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(lp);

        historyContainer.addView(row);
    }

    private int dp(int dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        );
    }
}