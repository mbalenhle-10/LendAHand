package com.example.lendahand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserProfileActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://13.135.14.204/api/auth/";
    private final OkHttpClient httpClient = new OkHttpClient();

    private LinearLayout btnBack;
    private LinearLayout btnEdit;
    private LinearLayout btnLogout;
    
    private TextView tvName, tvEmail, tvPhone, tvLocation;
    private TextView tvProfileName, tvAvatarInitials;

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        initSession();
        bindViews();
        setupBackButton();
        setupEditButton();
        setupLogoutButton();
        
        fetchUserProfile();
    }

    private void initSession() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            finish();
        }
    }

    private void bindViews() {
        btnBack   = findViewById(R.id.btnBack);
        btnEdit   = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);
        
        tvName     = findViewById(R.id.tvName);
        tvEmail    = findViewById(R.id.tvEmail);
        tvPhone    = findViewById(R.id.tvPhone);
        tvLocation = findViewById(R.id.tvLocation);
        
        tvProfileName   = findViewById(R.id.tvProfileName);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
    }

    private void fetchUserProfile() {
        Request request = new Request.Builder()
                .url(BASE_URL + "get_profile.php?user_id=" + userId)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(UserProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.getString("status"))) {
                            populateData(json.getJSONObject("user"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void populateData(JSONObject user) throws JSONException {
        String name = user.getString("fullname");
        String email = user.getString("email");
        String phone = user.optString("phone", "N/A");
        String location = user.optString("location", "N/A");

        tvName.setText(name);
        tvEmail.setText(email);
        tvPhone.setText(phone);
        tvLocation.setText(location);
        
        tvProfileName.setText(name);
        
        if (name.length() >= 2) {
            String initials = name.substring(0, 1).toUpperCase() + name.substring(name.indexOf(" ") + 1, name.indexOf(" ") + 2).toUpperCase();
            tvAvatarInitials.setText(initials);
        }
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupEditButton() {
        btnEdit.setOnClickListener(v -> {
            // Intent intent = new Intent(this, EditProfileActivity.class);
            // startActivity(intent);
        });
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
