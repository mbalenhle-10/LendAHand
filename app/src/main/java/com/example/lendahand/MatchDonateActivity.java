package com.example.lendahand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MatchDonateActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://wmc.ms.wits.ac.za/students/sgroup2685/public/api/auth/";

    // ---------------------------------------------------------------
    // Intent extras — passed in from DonateActivity on success
    // ---------------------------------------------------------------
    public static final String EXTRA_DONATION_ID = "donation_id";
    public static final String EXTRA_ITEM_ID = "item_id";
    public static final String EXTRA_ITEM_NAME = "item_name";
    public static final String EXTRA_UNIT = "unit";
    public static final String EXTRA_QUANTITY = "quantity";

    // Donation info
    private int donationId;
    private int itemId;
    private String itemName;
    private String unit;
    private int totalAvailable;
    private int donorUserId;

    // Recipients loaded from server
    // Each map: request_id, receiver_id, fullname, quantity_needed
    private final List<Map<String, Object>> recipients = new ArrayList<>();

    // Allocated quantity per request_id (key = request_id)
    private final HashMap<Integer, Integer> allocations = new HashMap<>();

    // Views
    private TextView tvItemSummary;
    private ProgressBar progressAllocation;
    private TextView tvAllocProgress;
    private LinearLayout recipientContainer;
    private TextView tvEmpty;
    private EditText etDonationNote;
    private LinearLayout btnConfirmAllocations;
    private LinearLayout btnBack;

    private final OkHttpClient httpClient = new OkHttpClient();

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.match_donate);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        donorUserId = prefs.getInt("user_id", -1);

        readIntent();
        bindViews();
        populateItemSummary();
        fetchRecipients();

        btnBack.setOnClickListener(v -> finish());
        btnConfirmAllocations.setOnClickListener(v -> onConfirmClicked());
    }

    // ---------------------------------------------------------------
    // Read Intent extras
    // ---------------------------------------------------------------

    private void readIntent() {
        Intent i = getIntent();
        donationId = i.getIntExtra(EXTRA_DONATION_ID, -1);
        itemId = i.getIntExtra(EXTRA_ITEM_ID, -1);
        itemName = i.getStringExtra(EXTRA_ITEM_NAME);
        unit = i.getStringExtra(EXTRA_UNIT);
        totalAvailable = i.getIntExtra(EXTRA_QUANTITY, 0);
    }

    // ---------------------------------------------------------------
    // View binding
    // ---------------------------------------------------------------

    private void bindViews() {
        tvItemSummary = findViewById(R.id.tvItemSummary);
        progressAllocation = findViewById(R.id.progressAllocation);
        tvAllocProgress = findViewById(R.id.tvAllocProgress);
        recipientContainer = findViewById(R.id.recipientContainer);
        tvEmpty = findViewById(R.id.tvEmpty);
        etDonationNote = findViewById(R.id.etDonationNote);
        btnConfirmAllocations = findViewById(R.id.btnConfirmAllocations);
        btnBack = findViewById(R.id.btnBack);
    }

    private void populateItemSummary() {
        tvItemSummary.setText(itemName + "  ·  " + totalAvailable + " " + unit);
    }

    // ---------------------------------------------------------------
    // Step 1 — Fetch open requests for this item
    // ---------------------------------------------------------------

    private void fetchRecipients() {
        String url = BASE_URL + "get_recipients.php"
                + "?item_id=" + itemId
                + "&donor_id=" + donorUserId;

        Request request = new Request.Builder().url(url).get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MatchDonateActivity.this,
                                "Failed to load recipients.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                android.util.Log.d("MATCH_RECIPIENTS", body);
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status"))) {
                            parseRecipients(json.getJSONArray("recipients"));
                            buildRecipientCards();
                        } else {
                            Toast.makeText(MatchDonateActivity.this,
                                    json.optString("message", "Could not load recipients."),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MatchDonateActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void parseRecipients(JSONArray array) throws JSONException {
        recipients.clear();
        allocations.clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            Map<String, Object> r = new HashMap<>();
            r.put("request_id", obj.getInt("request_id"));
            r.put("receiver_id", obj.getInt("receiver_id"));
            r.put("fullname", obj.getString("fullname"));
            r.put("quantity_needed", obj.getInt("quantity_needed"));
            recipients.add(r);
            allocations.put(obj.getInt("request_id"), 0); // start at 0
        }
    }

    // ---------------------------------------------------------------
    // Step 2 — Build a recipient card for each open request
    // ---------------------------------------------------------------

    private void buildRecipientCards() {
        recipientContainer.removeAllViews();

        if (recipients.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        for (Map<String, Object> r : recipients) {
            recipientContainer.addView(makeRecipientCard(r));
        }
        refreshProgress();
    }

    private LinearLayout makeRecipientCard(Map<String, Object> recipient) {
        int requestId = (int) recipient.get("request_id");
        String fullname = (String) recipient.get("fullname");
        int quantityNeeded = (int) recipient.get("quantity_needed");

        // Card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);

        // Name
        TextView tvName = new TextView(this);
        tvName.setText(fullname);
        tvName.setTextSize(14f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        card.addView(tvName);

        // Needs label
        TextView tvNeeds = new TextView(this);
        tvNeeds.setText("Needs " + quantityNeeded + " " + unit);
        tvNeeds.setTextSize(12f);
        tvNeeds.setTextColor(Color.parseColor("#8A7F78"));
        LinearLayout.LayoutParams needsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        needsLp.topMargin = dp(2);
        needsLp.bottomMargin = dp(10);
        tvNeeds.setLayoutParams(needsLp);
        card.addView(tvNeeds);

        // Stepper row  [−]  [n]  [+]
        LinearLayout stepperRow = new LinearLayout(this);
        stepperRow.setOrientation(LinearLayout.HORIZONTAL);
        stepperRow.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);

        TextView btnMinus = makeStepperBtn("−");
        TextView tvVal = makeStepperDisplay("0");
        TextView btnPlus = makeStepperBtn("+");

        stepperRow.addView(btnMinus);
        stepperRow.addView(tvVal);
        stepperRow.addView(btnPlus);
        card.addView(stepperRow);

        // Stepper logic
        btnMinus.setOnClickListener(v -> {
            int current = allocations.get(requestId);
            if (current > 0) {
                allocations.put(requestId, current - 1);
                tvVal.setText(String.valueOf(current - 1));
                refreshProgress();
            }
        });

        btnPlus.setOnClickListener(v -> {
            int current = allocations.get(requestId);
            int remaining = totalAvailable - getTotalAllocated();
            // Can't exceed what this person needs, and can't exceed what's left in the donation
            if (current < quantityNeeded && remaining > 0) {
                allocations.put(requestId, current + 1);
                tvVal.setText(String.valueOf(current + 1));
                refreshProgress();
            } else if (remaining <= 0) {
                Toast.makeText(this,
                        "You've allocated all " + totalAvailable + " " + unit + ".",
                        Toast.LENGTH_SHORT).show();
            }
        });

        return card;
    }

    // ---------------------------------------------------------------
    // Stepper helpers
    // ---------------------------------------------------------------

    private TextView makeStepperBtn(String label) {
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTextSize(20f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(Color.parseColor("#2F3E46"));
        btn.setBackgroundColor(Color.parseColor("#F4F1EA"));
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setClickable(true);
        btn.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
        btn.setLayoutParams(lp);
        return btn;
    }

    private TextView makeStepperDisplay(String value) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(16f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#2F3E46"));
        tv.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(40));
        lp.setMarginStart(dp(6));
        lp.setMarginEnd(dp(6));
        tv.setLayoutParams(lp);
        return tv;
    }

    // ---------------------------------------------------------------
    // Progress bar
    // ---------------------------------------------------------------

    private int getTotalAllocated() {
        int sum = 0;
        for (int v : allocations.values()) sum += v;
        return sum;
    }

    private void refreshProgress() {
        int allocated = getTotalAllocated();
        int percent = totalAvailable > 0 ? (allocated * 100) / totalAvailable : 0;
        progressAllocation.setProgress(percent);
        tvAllocProgress.setText(allocated + " of " + totalAvailable + " " + unit + " allocated");
    }

    // ---------------------------------------------------------------
    // Step 3 — Confirm: validate then submit
    // ---------------------------------------------------------------

    private void onConfirmClicked() {
        if (getTotalAllocated() == 0) {
            Toast.makeText(this, "Allocate at least 1 item before confirming.", Toast.LENGTH_SHORT).show();
            return;
        }
        submitAllocations();
    }

    // ---------------------------------------------------------------
    // Step 4 — POST allocations to submit_match.php
    // ---------------------------------------------------------------

    private void submitAllocations() {
        btnConfirmAllocations.setEnabled(false);

        try {
            JSONArray allocArray = new JSONArray();

            for (Map<String, Object> r : recipients) {
                int requestId = (int) r.get("request_id");
                int receiverId = (int) r.get("receiver_id");
                int qty = allocations.get(requestId);
                if (qty <= 0) continue;

                JSONObject entry = new JSONObject();
                entry.put("request_id", requestId);
                entry.put("receiver_id", receiverId);
                entry.put("quantity", qty);
                allocArray.put(entry);
            }

            JSONObject payload = new JSONObject();
            payload.put("donor_id", donorUserId);
            payload.put("donation_id", donationId);
            payload.put("item_id", itemId);
            payload.put("note", etDonationNote.getText().toString().trim());
            payload.put("allocations", allocArray);

            android.util.Log.d("MATCH_SUBMIT", payload.toString());

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(BASE_URL + "submit_match.php")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnConfirmAllocations.setEnabled(true);
                        Toast.makeText(MatchDonateActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() != null ? response.body().string() : "{}";
                    android.util.Log.d("MATCH_RESPONSE", respBody);
                    runOnUiThread(() -> {
                        btnConfirmAllocations.setEnabled(true);
                        try {
                            JSONObject json = new JSONObject(respBody);
                            if ("success".equals(json.optString("status"))) {
                                Toast.makeText(MatchDonateActivity.this,
                                        "Donation matched successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(MatchDonateActivity.this,
                                        json.optString("message", "Something went wrong."),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(MatchDonateActivity.this,
                                    "Unexpected server response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            btnConfirmAllocations.setEnabled(true);
            Toast.makeText(this, "Error preparing submission.", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}