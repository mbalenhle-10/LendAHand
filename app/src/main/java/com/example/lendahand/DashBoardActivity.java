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

    private LinearLayout btnAddDonation, btnAddNeed, btnUserProfile, btnMyActivity, btnTopDonors;

    // Session
    private SharedPreferences prefs;
    private int userId;
    private String username;

    // API
    private final String statsUrl = "https://wmc.ms.wits.ac.za/students/sgroup2685/public/api/auth/stats.php";;

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
        btnUserProfile = findViewById(R.id.btnViewMatches);
        btnMyActivity  = findViewById(R.id.btnMyActivity);
        btnTopDonors   = findViewById(R.id.btnTopDonors);
    }

    private void setupUI() {
        welcomeText.setText("Welcome back, " + username + " 👋");
        loadDashboardStats();
    }

    private void setupListeners() {

        btnAddDonation.setOnClickListener(v ->
                startActivity(new Intent(this, MyDonationsActivity.class)));

        btnAddNeed.setOnClickListener(v ->
                startActivity(new Intent(this, MyNeeds.class)));

        btnUserProfile.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfileActivity.class)));

        btnMyActivity.setOnClickListener(v ->
                startActivity(new Intent(this, ActivityHistoryActivity.class)));

        btnTopDonors.setOnClickListener(v ->
                startActivity(new Intent(this, TopDonorsActivity.class)));
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