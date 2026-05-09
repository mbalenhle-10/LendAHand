package com.example.lendahand;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MatchDonateActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://13.135.14.204/api/auth/";
    private final OkHttpClient httpClient = new OkHttpClient();
    private int userId;

    // Resource buttons
    private TextView btnRice;
    private TextView btnMaize;
    private TextView btnCookingOil;
    private TextView btnBlankets;
    private TextView btnClothingAdults;
    private TextView btnClothingChildren;
    private TextView btnStationery;
    private TextView btnSanitary;
    private TextView btnCanned;
    private TextView btnBabyFormula;
    private TextView currentSelectedResource;

    // Allocation progress
    private ProgressBar progressAllocation;
    private TextView tvAllocProgress;
    private static final int TOTAL_KG = 10;

    // Recipient 1 stepper
    private TextView tvRecipient1Val;
    private TextView btnRecipient1Minus;
    private TextView btnRecipient1Plus;
    private int recipient1Val = 3;
    private static final int RECIPIENT1_MAX = 5;

    // Recipient 2 stepper
    private TextView tvRecipient2Val;
    private TextView btnRecipient2Minus;
    private TextView btnRecipient2Plus;
    private int recipient2Val = 2;
    private static final int RECIPIENT2_MAX = 3;

    // Recipient 3 stepper
    private TextView tvRecipient3Val;
    private TextView btnRecipient3Minus;
    private TextView btnRecipient3Plus;
    private int recipient3Val = 0;
    private static final int RECIPIENT3_MAX = 8;

    // Confirm and Back
    private LinearLayout btnConfirmAllocations;
    private LinearLayout btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.match_donate);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        bindViews();
        setupResourceSelection();
        setupSteppers();
        setupConfirmButton();
        setupBackButton();
        updateProgress();
        
        fetchRecipients("1"); // Default to Rice (ID 1)
    }

    private void fetchRecipients(String itemId) {
        Request request = new Request.Builder()
                .url(BASE_URL + "get_recipients.php?item_id=" + itemId)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MatchDonateActivity.this, "Failed to load recipients", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.getString("status"))) {
                            updateRecipientUI(json.getJSONArray("recipients"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void updateRecipientUI(JSONArray recipients) throws JSONException {
        // This is a placeholder for dynamic UI injection. 
        // For now, we'll map the first 3 results to your static cards if they exist.
        if (recipients.length() > 0) {
            JSONObject r1 = recipients.getJSONObject(0);
            // Update TextViews for Recipient 1 name/location
        }
    }

    private void bindViews() {
        btnRice             = findViewById(R.id.btnRice);
        btnMaize            = findViewById(R.id.btnMaize);
        btnCookingOil       = findViewById(R.id.btnCookingOil);
        btnBlankets         = findViewById(R.id.btnBlankets);
        btnClothingAdults   = findViewById(R.id.btnClothingAdults);
        btnClothingChildren = findViewById(R.id.btnClothingChildren);
        btnStationery       = findViewById(R.id.btnStationery);
        btnSanitary         = findViewById(R.id.btnSanitary);
        btnCanned           = findViewById(R.id.btnCanned);
        btnBabyFormula      = findViewById(R.id.btnBabyFormula);

        progressAllocation  = findViewById(R.id.progressAllocation);
        tvAllocProgress     = findViewById(R.id.tvAllocProgress);

        tvRecipient1Val     = findViewById(R.id.tvRecipient1Val);
        btnRecipient1Minus  = findViewById(R.id.btnRecipient1Minus);
        btnRecipient1Plus   = findViewById(R.id.btnRecipient1Plus);

        tvRecipient2Val     = findViewById(R.id.tvRecipient2Val);
        btnRecipient2Minus  = findViewById(R.id.btnRecipient2Minus);
        btnRecipient2Plus   = findViewById(R.id.btnRecipient2Plus);

        tvRecipient3Val     = findViewById(R.id.tvRecipient3Val);
        btnRecipient3Minus  = findViewById(R.id.btnRecipient3Minus);
        btnRecipient3Plus   = findViewById(R.id.btnRecipient3Plus);

        btnConfirmAllocations = findViewById(R.id.btnConfirmAllocations);
        btnBack               = findViewById(R.id.btnBack);
    }

    private void setupResourceSelection() {
        currentSelectedResource = btnRice;
        highlightResource(btnRice);

        View.OnClickListener resourceClick = view -> {
            clearResourceHighlight(currentSelectedResource);
            currentSelectedResource = (TextView) view;
            highlightResource(currentSelectedResource);
            
            // Map view ID to Item ID for backend
            String itemId = "1"; // Default
            if (view.getId() == R.id.btnMaize) itemId = "2";
            else if (view.getId() == R.id.btnCookingOil) itemId = "3";
            // ... add others
            
            fetchRecipients(itemId);
        };

        btnRice.setOnClickListener(resourceClick);
        btnMaize.setOnClickListener(resourceClick);
        btnCookingOil.setOnClickListener(resourceClick);
        btnBlankets.setOnClickListener(resourceClick);
        btnClothingAdults.setOnClickListener(resourceClick);
        btnClothingChildren.setOnClickListener(resourceClick);
        btnStationery.setOnClickListener(resourceClick);
        btnSanitary.setOnClickListener(resourceClick);
        btnCanned.setOnClickListener(resourceClick);
        btnBabyFormula.setOnClickListener(resourceClick);
    }

    private void highlightResource(TextView btn) {
        btn.setBackgroundColor(0xFFEEF3F5);
        btn.setTextColor(0xFF2F3E46);
    }

    private void clearResourceHighlight(TextView btn) {
        btn.setBackgroundColor(0xFFFFFFFF);
        btn.setTextColor(0xFF3A3A3A);
    }

    private void setupSteppers() {

        btnRecipient1Minus.setOnClickListener(v -> {
            if (recipient1Val > 0) {
                recipient1Val--;
                tvRecipient1Val.setText(getString(R.string.kg_value, recipient1Val));
                updateProgress();
            }
        });
        btnRecipient1Plus.setOnClickListener(v -> {
            if (recipient1Val < RECIPIENT1_MAX && getAllocated() < TOTAL_KG) {
                recipient1Val++;
                tvRecipient1Val.setText(getString(R.string.kg_value, recipient1Val));
                updateProgress();
            } else if (getAllocated() >= TOTAL_KG) {
                Toast.makeText(this, getString(R.string.total_limit_reached, TOTAL_KG), Toast.LENGTH_SHORT).show();
            }
        });

        btnRecipient2Minus.setOnClickListener(v -> {
            if (recipient2Val > 0) {
                recipient2Val--;
                tvRecipient2Val.setText(getString(R.string.kg_value, recipient2Val));
                updateProgress();
            }
        });
        btnRecipient2Plus.setOnClickListener(v -> {
            if (recipient2Val < RECIPIENT2_MAX && getAllocated() < TOTAL_KG) {
                recipient2Val++;
                tvRecipient2Val.setText(getString(R.string.kg_value, recipient2Val));
                updateProgress();
            } else if (getAllocated() >= TOTAL_KG) {
                Toast.makeText(this, getString(R.string.total_limit_reached, TOTAL_KG), Toast.LENGTH_SHORT).show();
            }
        });

        btnRecipient3Minus.setOnClickListener(v -> {
            if (recipient3Val > 0) {
                recipient3Val--;
                tvRecipient3Val.setText(getString(R.string.kg_value, recipient3Val));
                updateProgress();
            }
        });
        btnRecipient3Plus.setOnClickListener(v -> {
            if (recipient3Val < RECIPIENT3_MAX && getAllocated() < TOTAL_KG) {
                recipient3Val++;
                tvRecipient3Val.setText(getString(R.string.kg_value, recipient3Val));
                updateProgress();
            } else if (getAllocated() >= TOTAL_KG) {
                Toast.makeText(this, getString(R.string.total_limit_reached, TOTAL_KG), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getAllocated() {
        return recipient1Val + recipient2Val + recipient3Val;
    }

    private void updateProgress() {
        int allocated = getAllocated();
        int percent   = (allocated * 100) / TOTAL_KG;
        progressAllocation.setProgress(percent);
        tvAllocProgress.setText(getString(R.string.alloc_progress, allocated, TOTAL_KG));
    }

    private void setupConfirmButton() {
        btnConfirmAllocations.setOnClickListener(v -> {
            if (getAllocated() == 0) {
                Toast.makeText(this, getString(R.string.alloc_empty_warning), Toast.LENGTH_SHORT).show();
                return;
            }
            
            submitAllocations();
        });
    }

    private void submitAllocations() {
        // Prepare JSON payload of allocations
        JSONArray allocations = new JSONArray();
        try {
            if (recipient1Val > 0) {
                JSONObject a = new JSONObject();
                a.put("recipient_id", 101); // Example ID
                a.put("quantity", recipient1Val);
                allocations.put(a);
            }
            // Add other recipients...
        } catch (JSONException e) {}

        RequestBody body = new FormBody.Builder()
                .add("user_id", String.valueOf(userId))
                .add("item_id", "1") // Dynamic based on selection
                .add("allocations", allocations.toString())
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "submit_match.php")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MatchDonateActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    Toast.makeText(MatchDonateActivity.this, "Allocations saved!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }
}