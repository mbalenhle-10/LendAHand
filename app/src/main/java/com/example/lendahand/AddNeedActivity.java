package com.example.lendahand;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddNeedActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://13.135.14.204/api/auth/";

    // Views
    private View        btnSubmitNeed;
    private TextView    tvQtyDisplay;
    private View        btnQtyPlus;
    private View        btnQtyMinus;
    private Spinner     spinnerUnit;
    private EditText    etNote;
    private View        btnBack;

    private int currentQty = 2;
    private String selectedItemId = "1"; // Default to Rice
    private final List<View> resourceButtons = new ArrayList<>();

    private final OkHttpClient httpClient = new OkHttpClient();

    // Session
    private SharedPreferences prefs;
    private int userId;

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_a_need);

        initSession();
        bindViews();
        setupListeners();
    }

    // ---------------------------------------------------------------
    // Session
    // ---------------------------------------------------------------

    private void initSession() {
        prefs  = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
    }

    // ---------------------------------------------------------------
    // View binding
    // ---------------------------------------------------------------

    private void bindViews() {
        btnSubmitNeed    = findViewById(R.id.btnSubmitNeed);
        tvQtyDisplay     = findViewById(R.id.tvQtyDisplay);
        btnQtyPlus       = findViewById(R.id.btnQtyPlus);
        btnQtyMinus      = findViewById(R.id.btnQtyMinus);
        spinnerUnit      = findViewById(R.id.spinnerUnit);
        etNote           = findViewById(R.id.etNote);
        btnBack          = findViewById(R.id.btnBack);
    }

    private void setupListeners() {
        btnQtyPlus.setOnClickListener(v -> {
            currentQty++;
            updateQtyDisplay();
        });

        btnQtyMinus.setOnClickListener(v -> {
            if (currentQty > 1) {
                currentQty--;
                updateQtyDisplay();
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnSubmitNeed.setOnClickListener(v -> submitNeed());

        // Setup individual resource buttons (Grid in XML)
        int[] resIds = {
                R.id.btnRice, R.id.btnMaize, R.id.btnCookingOil, R.id.btnBlankets,
                R.id.btnClothingAdults, R.id.btnClothingChildren, R.id.btnStationery,
                R.id.btnSanitary, R.id.btnCanned, R.id.btnBabyFormula
        };
        String[] names = {
                "Rice", "Maize Meal", "Cooking Oil", "Blankets",
                "Clothing (Adults)", "Clothing (Children)", "School Stationery",
                "Sanitary Products", "Canned Goods", "Baby Formula"
        };
        String[] ids = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

        for (int i = 0; i < resIds.length; i++) {
            final View btn = findViewById(resIds[i]);
            if (btn == null) continue;
            final String name = names[i];
            final String id = ids[i];
            resourceButtons.add(btn);
            btn.setOnClickListener(v -> selectResource(btn, name, id));
        }

        // Set initial selection
        if (!resourceButtons.isEmpty()) {
            selectResource(resourceButtons.get(0), names[0], ids[0]);
        }
    }

    private void selectResource(View v, String name, String id) {
        selectedItemId = id;

        // Reset all buttons
        for (View btn : resourceButtons) {
            btn.setBackgroundResource(android.R.drawable.btn_default); // Placeholder
            if (btn instanceof TextView) {
                ((TextView) btn).setTextColor(android.graphics.Color.BLACK);
            }
        }

        // Highlight selected (Use a distinguishable color)
        v.setBackgroundColor(android.graphics.Color.LTGRAY);
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(android.graphics.Color.BLUE);
        }

        Toast.makeText(this, "Selected: " + name, Toast.LENGTH_SHORT).show();
    }

    private void updateQtyDisplay() {
        tvQtyDisplay.setText(String.valueOf(currentQty));
    }



    // ---------------------------------------------------------------
    // Step 2: Submit the need
    //
    //  POST /api/auth/need.php
    //  Body (form): user_id, item_id, quantity
    // ---------------------------------------------------------------

    private void submitNeed() {
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String quantityStr = String.valueOf(currentQty);
        String note = etNote.getText().toString().trim();
        String unit = spinnerUnit.getSelectedItem().toString();

        setLoading(true);
        btnSubmitNeed.setEnabled(false);

        RequestBody formBody = new FormBody.Builder()
                .add("user_id",  String.valueOf(userId))
                .add("item_id",  selectedItemId)
                .add("quantity", quantityStr)
                .add("unit",     unit)
                .add("note",     note)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "need.php")
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    btnSubmitNeed.setEnabled(true);
                    Toast.makeText(AddNeedActivity.this,
                            "Submission failed. Try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                android.util.Log.d("NEED_RESPONSE", body);
                runOnUiThread(() -> {
                    setLoading(false);
                    btnSubmitNeed.setEnabled(true);
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status", ""))) {
                            Toast.makeText(AddNeedActivity.this,
                                    "Need submitted!", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity on success
                        } else {
                            String msg = json.optString("message", "Something went wrong.");
                            Toast.makeText(AddNeedActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(AddNeedActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        // No separate progress bar in this layout, could add one or use a dialog
        // For now just toggle button state
        btnSubmitNeed.setEnabled(!loading);
    }
}

