package com.example.lendahand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyNeeds extends AppCompatActivity {

    private static final String BASE_URL = "http://13.135.14.204/api/auth/";

    private TextView    addNeed;
    private TextView    tvPending, tvPartial, tvFulfilled;
    private LinearLayout cardsContainer;
    private final OkHttpClient httpClient = new OkHttpClient();
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_needs);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        addNeed       = findViewById(R.id.btnplusneed);
        tvPending     = findViewById(R.id.tvStatPending);
        tvPartial     = findViewById(R.id.tvStatPartial);
        tvFulfilled   = findViewById(R.id.tvStatFulfilled);
        cardsContainer = findViewById(R.id.cardsContainer);

        addNeed.setOnClickListener(v ->
                startActivity(new Intent(this, AddNeedActivity.class)));

        fetchNeeds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNeeds(); // refresh when returning from AddNeedActivity
    }

    private void fetchNeeds() {
        if (userId == -1) return;

        Request request = new Request.Builder()
                .url(BASE_URL + "my_needs.php?user_id=" + userId)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MyNeeds.this, "Could not load needs.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status"))) {
                            buildUI(json.getJSONArray("requests"));
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MyNeeds.this, "Unexpected response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void buildUI(JSONArray requests) throws JSONException {
        cardsContainer.removeAllViews();

        int pending = 0, partial = 0, fulfilled = 0;

        String currentSection = "";

        for (int i = 0; i < requests.length(); i++) {
            JSONObject r       = requests.getJSONObject(i);
            String status      = r.getString("status");
            String itemName    = r.getString("item_name");
            String unit        = r.optString("unit", "");
            int    quantity    = r.getInt("quantity_needed");
            String description = r.optString("description", "");

            // Count stats
            switch (status) {
                case "unfulfilled": pending++;   break;
                case "partial":     partial++;   break;
                case "fulfilled":   fulfilled++; break;
            }

            // Section header
            String sectionLabel = getSectionLabel(status);
            if (!sectionLabel.equals(currentSection)) {
                currentSection = sectionLabel;
                TextView header = new TextView(this);
                header.setText(sectionLabel);
                header.setTextSize(12);
                header.setTextColor(Color.parseColor("#5F5A55"));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 24, 0, 10);
                header.setLayoutParams(lp);
                header.setTypeface(null, android.graphics.Typeface.BOLD);
                cardsContainer.addView(header);
            }

            // Card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(dp(18), dp(18), dp(18), dp(18));
            card.setElevation(dp(3));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardParams);

            // Item name
            TextView tvName = new TextView(this);
            tvName.setText(itemName);
            tvName.setTextSize(16);
            tvName.setTextColor(Color.parseColor("#2B2B2B"));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(tvName);

            // Details
            TextView tvDetails = new TextView(this);
            String details = "Qty: " + quantity + (unit.isEmpty() ? "" : " " + unit);
            if (!description.isEmpty()) details += " · " + description;
            tvDetails.setText(details);
            tvDetails.setTextSize(13);
            tvDetails.setTextColor(Color.parseColor("#5F5A55"));
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            detailsParams.setMargins(0, dp(4), 0, 0);
            tvDetails.setLayoutParams(detailsParams);
            card.addView(tvDetails);

            // Status badge
            TextView tvStatus = new TextView(this);
            tvStatus.setText(getStatusLabel(status));
            tvStatus.setTextSize(12);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setTextColor(Color.parseColor(getStatusColor(status)));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            statusParams.setMargins(0, dp(10), 0, 0);
            tvStatus.setLayoutParams(statusParams);
            card.addView(tvStatus);

            cardsContainer.addView(card);
        }

        // Update stat cards
        tvPending.setText(pending + "\nPending");
        tvPartial.setText(partial + "\nIn Progress");
        tvFulfilled.setText(fulfilled + "\nFulfilled");

        // Empty state
        if (requests.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("You have no needs yet. Tap + Add Need to get started.");
            empty.setTextColor(Color.parseColor("#5F5A55"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, 0);
            cardsContainer.addView(empty);
        }
    }

    private String getSectionLabel(String status) {
        switch (status) {
            case "unfulfilled": return "WAITING FOR HELP";
            case "partial":     return "BEING FULFILLED";
            case "fulfilled":   return "RECEIVED — THANK YOU";
            default:            return "";
        }
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case "unfulfilled": return "● Pending";
            case "partial":     return "● Partially fulfilled";
            case "fulfilled":   return "✓ Fulfilled";
            default:            return status;
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "unfulfilled": return "#92400E";
            case "partial":     return "#1D4ED8";
            case "fulfilled":   return "#2F6B3E";
            default:            return "#000000";
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
