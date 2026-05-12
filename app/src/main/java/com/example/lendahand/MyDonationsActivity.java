package com.example.lendahand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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

public class MyDonationsActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://13.135.14.204/api/auth/";

    private LinearLayout donationListContainer;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private ScrollView   scrollView;
    private LinearLayout btnBack;

    private TextView addDonation;
    private int userId;
    private final OkHttpClient httpClient = new OkHttpClient();

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_donations);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);


        bindViews();
        btnBack.setOnClickListener(v -> finish());
        addDonation.setOnClickListener(v ->
                startActivity(new Intent(this, DonateActivity.class))
        );
        fetchDonations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list whenever we return from the Match screen
        // (a match may have changed statuses)
        fetchDonations();
    }

    // ---------------------------------------------------------------
    // View binding
    // ---------------------------------------------------------------

    private void bindViews() {
        donationListContainer = findViewById(R.id.donationListContainer);
        progressBar           = findViewById(R.id.progressBar);
        tvEmpty               = findViewById(R.id.tvEmpty);
        scrollView            = findViewById(R.id.scrollView);
        btnBack               = findViewById(R.id.btnBack);
        addDonation           = findViewById(R.id.btnAddDonations);
    }

    // ---------------------------------------------------------------
    // Fetch
    //   GET get_my_donations.php?user_id=X
    // ---------------------------------------------------------------

    private void fetchDonations() {
        setLoading(true);

        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BASE_URL + "get_my_donations.php?user_id=" + userId;
        Request request = new Request.Builder().url(url).get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(MyDonationsActivity.this,
                            "Could not load donations. Check your connection.",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                runOnUiThread(() -> {
                    setLoading(false);
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status"))) {
                            buildDonationCards(json.getJSONArray("donations"));
                        } else {
                            Toast.makeText(MyDonationsActivity.this,
                                    json.optString("message", "Failed to load donations."),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MyDonationsActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------
    // Build one card per donation
    // ---------------------------------------------------------------

    private void buildDonationCards(JSONArray donations) throws JSONException {
        donationListContainer.removeAllViews();

        if (donations.length() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);

        for (int i = 0; i < donations.length(); i++) {
            JSONObject d = donations.getJSONObject(i);

            int    donationId   = d.getInt("donation_id");
            int    itemId       = d.getInt("item_id");
            String itemName     = d.getString("item_name");
            String unit         = d.getString("unit");
            String category     = d.getString("category");
            int    qtyAvail     = d.getInt("quantity_available");
            String status       = d.getString("status");

            donationListContainer.addView(
                    makeDonationCard(donationId, itemId, itemName, unit, category, qtyAvail, status));
        }
    }

    private LinearLayout makeDonationCard(
            int donationId, int itemId, String itemName,
            String unit, String category, int qtyAvail, String status) {

        boolean isMatchable = status.equals("available") || status.equals("partial");

        // ── Outer card ──────────────────────────────────────────────
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(12);
        card.setLayoutParams(cardLp);

        // ── Top row: item name + status badge ───────────────────────
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setBaselineAligned(false);
        LinearLayout.LayoutParams topRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        topRowLp.bottomMargin = dp(4);
        topRow.setLayoutParams(topRowLp);

        // Item name
        TextView tvName = new TextView(this);
        tvName.setText(itemName);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);
        topRow.addView(tvName);

        // Status badge
        TextView tvStatus = new TextView(this);
        tvStatus.setText(status.toUpperCase());
        tvStatus.setTextSize(10f);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(dp(8), dp(4), dp(8), dp(4));
        tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        switch (status) {
            case "available":
                tvStatus.setBackgroundColor(Color.parseColor("#EAF3DE"));
                tvStatus.setTextColor(Color.parseColor("#1E5631"));
                break;
            case "partial":
                tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
                tvStatus.setTextColor(Color.parseColor("#B45309"));
                break;
            case "matched":
                tvStatus.setBackgroundColor(Color.parseColor("#E5E7EB"));
                tvStatus.setTextColor(Color.parseColor("#6B7280"));
                break;
        }
        topRow.addView(tvStatus);
        card.addView(topRow);

        // ── Sub-label: category · quantity unit ─────────────────────
        TextView tvSub = new TextView(this);
        tvSub.setText(category + "  ·  " + qtyAvail + " " + unit);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(Color.parseColor("#8A7F78"));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.bottomMargin = isMatchable ? dp(12) : 0;
        tvSub.setLayoutParams(subLp);
        card.addView(tvSub);

        // ── "Find a Match" button — only for available / partial ────
        if (isMatchable) {
            LinearLayout btnMatch = new LinearLayout(this);
            btnMatch.setOrientation(LinearLayout.HORIZONTAL);
            btnMatch.setGravity(android.view.Gravity.CENTER);
            btnMatch.setBackgroundColor(Color.parseColor("#2F3E46"));
            btnMatch.setPadding(dp(0), dp(10), dp(0), dp(10));
            btnMatch.setClickable(true);
            btnMatch.setFocusable(true);
            btnMatch.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvBtn = new TextView(this);
            tvBtn.setText("Find a Match");
            tvBtn.setTextSize(13f);
            tvBtn.setTypeface(null, Typeface.BOLD);
            tvBtn.setTextColor(Color.WHITE);
            btnMatch.addView(tvBtn);

            btnMatch.setOnClickListener(v -> openMatchScreen(
                    donationId, itemId, itemName, unit, qtyAvail));

            card.addView(btnMatch);
        }

        return card;
    }

    // ---------------------------------------------------------------
    // Open MatchDonateActivity for a specific donation
    // ---------------------------------------------------------------

    private void openMatchScreen(int donationId, int itemId,
                                 String itemName, String unit, int qtyAvail) {
        Intent intent = new Intent(this, MatchDonateActivity.class);
        intent.putExtra(MatchDonateActivity.EXTRA_DONATION_ID, donationId);
        intent.putExtra(MatchDonateActivity.EXTRA_ITEM_ID,     itemId);
        intent.putExtra(MatchDonateActivity.EXTRA_ITEM_NAME,   itemName);
        intent.putExtra(MatchDonateActivity.EXTRA_UNIT,        unit);
        intent.putExtra(MatchDonateActivity.EXTRA_QUANTITY,    qtyAvail);
        startActivity(intent);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            scrollView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}