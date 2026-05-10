package com.example.lendahand;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityHistoryActivity extends AppCompatActivity {

    private LinearLayout btnBack;
    private TextView tabAll;
    private TextView tabDonated;
    private TextView tabReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        bindViews();
        setupBackButton();
        setupTabs();
    }

    private void bindViews() {
        btnBack      = findViewById(R.id.btnBack);
        tabAll       = findViewById(R.id.tabAll);
        tabDonated   = findViewById(R.id.tabDonated);
        tabReceived  = findViewById(R.id.tabReceived);
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {
            setTabActive(tabAll);
            setTabInactive(tabDonated);
            setTabInactive(tabReceived);
        });

        tabDonated.setOnClickListener(v -> {
            setTabActive(tabDonated);
            setTabInactive(tabAll);
            setTabInactive(tabReceived);
        });

        tabReceived.setOnClickListener(v -> {
            setTabActive(tabReceived);
            setTabInactive(tabAll);
            setTabInactive(tabDonated);
        });
    }

    private void setTabActive(TextView tab) {
        tab.setBackgroundColor(0xFF2F3E46);
        tab.setTextColor(0xFFFFFFFF);
    }

    private void setTabInactive(TextView tab) {
        tab.setBackgroundColor(0xFFF4F1EA);
        tab.setTextColor(0xFF5A524A);
    }
}