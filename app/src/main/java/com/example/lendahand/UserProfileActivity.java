package com.example.lendahand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

    // Header
    private LinearLayout btnBack;
    private LinearLayout btnEdit;
    private TextView     tvAvatarInitials;
    private TextView     tvProfileName;

    // Stats
    private TextView tvStatDonated;
    private TextView tvStatReceived;
    private TextView tvStatMatches;

    // Personal info rows
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvBio;

    // Footer
    private LinearLayout btnLogout;

    private int userId;

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        initSession();
        bindViews();
        setupButtons();
        fetchUserProfile();
    }

    // ---------------------------------------------------------------
    // Session
    // ---------------------------------------------------------------

    private void initSession() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) finish();
    }

    // ---------------------------------------------------------------
    // View binding
    // ---------------------------------------------------------------

    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        btnEdit          = findViewById(R.id.btnEdit);
        btnLogout        = findViewById(R.id.btnLogout);

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvProfileName    = findViewById(R.id.tvProfileName);

        tvStatDonated    = findViewById(R.id.tvStatDonated);
        tvStatReceived   = findViewById(R.id.tvStatReceived);
        tvStatMatches    = findViewById(R.id.tvStatMatches);

        tvName           = findViewById(R.id.tvName);
        tvEmail          = findViewById(R.id.tvEmail);
        tvPhone          = findViewById(R.id.tvPhone);
        tvBio            = findViewById(R.id.tvBio);
    }

    // ---------------------------------------------------------------
    // Buttons
    // ---------------------------------------------------------------

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            // Uncomment when EditProfileActivity is ready:
            // startActivity(new Intent(this, EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                    .edit().clear().apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ---------------------------------------------------------------
    // Fetch profile from server
    //   GET get_profile.php?user_id=X
    // ---------------------------------------------------------------

    private void fetchUserProfile() {
        Request request = new Request.Builder()
                .url(BASE_URL + "get_profile.php?user_id=" + userId)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(UserProfileActivity.this,
                                "Network error. Could not load profile.",
                                Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status"))) {
                            populateUI(json.getJSONObject("user"));
                        } else {
                            Toast.makeText(UserProfileActivity.this,
                                    json.optString("message", "Failed to load profile."),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(UserProfileActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------
    // Populate UI from the user JSON object
    // ---------------------------------------------------------------

    private void populateUI(JSONObject user) throws JSONException {
        String fullname = user.getString("fullname");
        String email    = user.getString("email");
        String phone    = user.optString("phone", "");
        String bio      = user.optString("bio", "");

        int itemsDonated  = user.optInt("items_donated",  0);
        int itemsReceived = user.optInt("items_received", 0);
        int matchesMade   = user.optInt("matches_made",   0);

        // Avatar initials — safely handles single-word names
        tvAvatarInitials.setText(buildInitials(fullname));
        tvProfileName.setText(fullname);

        // Personal info
        tvName.setText(fullname);
        tvEmail.setText(email);
        tvPhone.setText(phone.isEmpty() ? "Not provided" : phone);
        tvBio.setText(bio.isEmpty() ? "No bio yet" : bio);

        // Stats
        tvStatDonated.setText(String.valueOf(itemsDonated));
        tvStatReceived.setText(String.valueOf(itemsReceived));
        tvStatMatches.setText(String.valueOf(matchesMade));
    }

    private String buildInitials(String fullname) {
        if (fullname == null || fullname.isEmpty()) return "?";
        String[] parts = fullname.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}