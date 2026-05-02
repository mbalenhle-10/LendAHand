package com.example.lendahand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DashBoardActivity extends AppCompatActivity {

    // UI
    private TextView welcomeText;
    private TextView donationCount, needCount;

    private LinearLayout btnAddDonation, btnAddNeed, btnViewMatches, btnMyActivity;

    // Session
    private SharedPreferences prefs;
    private int userId;
    private String username;

    // API
    private final String statsUrl = "http://13.135.14.204/api/dashboard/stats.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);

        initSession();
        bindViews();
        setupUI();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    // ─────────────────────────────────────────────
    // Setup Methods
    // ─────────────────────────────────────────────

    private void initSession() {
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        username = prefs.getString("username", "");

        if (userId == -1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void bindViews() {
        welcomeText = findViewById(R.id.welcomeText);

        donationCount = findViewById(R.id.txtDonationCount);
        needCount     = findViewById(R.id.txtNeedCount);

        btnAddDonation = findViewById(R.id.btnAddDonation);
        btnAddNeed     = findViewById(R.id.btnAddNeed);
        btnViewMatches = findViewById(R.id.btnViewMatches);
        btnMyActivity  = findViewById(R.id.btnMyActivity);
    }

    private void setupUI() {
        welcomeText.setText("Welcome back, " + username + " 👋");
        loadDashboardStats();
    }

  private void setupListeners() {

      btnAddDonation.setOnClickListener(v ->
              startActivity(new Intent(this, DonateActivity.class)));

 //     btnAddNeed.setOnClickListener(v ->
 //             startActivity(new Intent(this, RequestActivity.class)));

 //     btnViewMatches.setOnClickListener(v ->
 //             startActivity(new Intent(this, MatchesActivity.class)));

 //     btnMyActivity.setOnClickListener(v ->
 //             startActivity(new Intent(this, MyActivity.class)));
 }

    // ─────────────────────────────────────────────
    // API
    // ─────────────────────────────────────────────

    private void loadDashboardStats() {

        StringRequest request = new StringRequest(
                Request.Method.POST,
                statsUrl,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);

                        if (json.getString("status").equals("success")) {

                            int donations = json.getInt("donations");
                            int requests  = json.getInt("requests");

                            donationCount.setText(String.valueOf(donations));
                            needCount.setText(String.valueOf(requests));
                        }

                    } catch (Exception e) {
                        setDefaultStats();
                    }
                },
                error -> setDefaultStats()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void setDefaultStats() {
        donationCount.setText("—");
        needCount.setText("—");
    }
}