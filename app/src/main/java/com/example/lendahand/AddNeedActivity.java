package com.example.lendahand;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private static final String BASE_URL = "https://wmc.ms.wits.ac.za/students/sgroup2685/public/api/auth/";

    // ---------------------------------------------------------------
    // Data — loaded from items.php
    //   categoryMap : category_name → list of items
    //   Each item map has keys: item_id, item_name, unit
    // ---------------------------------------------------------------
    private final LinkedHashMap<String, List<Map<String, String>>> categoryMap = new LinkedHashMap<>();

    // Views
    private LinearLayout categoryTabsContainer;
    private LinearLayout itemGridContainer;
    private TextView     tvQtyDisplay;
    private View         btnQtyPlus;
    private View         btnQtyMinus;
    private EditText     etNote;
    private View         btnSubmitNeed;
    private View         btnBack;

    // Selection state
    private int    currentQty      = 1;
    private String selectedItemId  = null;
    private View   selectedTab     = null;
    private View   selectedPanel   = null;

    private final OkHttpClient httpClient = new OkHttpClient();
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
        fetchItems();
    }

    private void initSession() {
        prefs  = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
    }

    private void bindViews() {
        categoryTabsContainer = findViewById(R.id.categoryTabsContainer);
        itemGridContainer     = findViewById(R.id.itemGridContainer);
        tvQtyDisplay          = findViewById(R.id.tvQtyDisplay);
        btnQtyPlus            = findViewById(R.id.btnQtyPlus);
        btnQtyMinus           = findViewById(R.id.btnQtyMinus);
        etNote                = findViewById(R.id.etNote);
        btnSubmitNeed         = findViewById(R.id.btnSubmitNeed);
        btnBack               = findViewById(R.id.btnBack);

        btnSubmitNeed.setEnabled(false);
        btnBack.setOnClickListener(v -> finish());

        btnQtyPlus.setOnClickListener(v  -> { currentQty++; refreshQtyDisplay(); });
        btnQtyMinus.setOnClickListener(v -> {
            if (currentQty > 1) { currentQty--; refreshQtyDisplay(); }
        });

        btnSubmitNeed.setOnClickListener(v -> submitNeed());
    }

    // ---------------------------------------------------------------
    // Step 1 — Fetch items from server
    // ---------------------------------------------------------------

    private void fetchItems() {
        Request request = new Request.Builder()
                .url(BASE_URL + "items.php")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(AddNeedActivity.this,
                                "Could not load items. Check your connection.",
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    try {
                        parseCategoryMap(new JSONArray(body));
                        buildCategoryTabs();
                        btnSubmitNeed.setEnabled(true);
                    } catch (JSONException e) {
                        Toast.makeText(AddNeedActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Parse the flat item list from items.php into:
     *   categoryMap { "Food" → [ {item_id, item_name, unit}, … ], "Clothing" → […] }
     *
     * Expects each JSON object to have: item_id, item_name, unit, category
     * (same structure the original DonateActivity used)
     */
    private void parseCategoryMap(JSONArray array) throws JSONException {
        categoryMap.clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj   = array.getJSONObject(i);
            String category  = obj.getString("category");
            String itemId    = obj.getString("item_id");
            String itemName  = obj.getString("item_name");
            String unit      = obj.optString("unit", "");

            if (!categoryMap.containsKey(category)) {
                categoryMap.put(category, new ArrayList<>());
            }

            Map<String, String> item = new LinkedHashMap<>();
            item.put("item_id",   itemId);
            item.put("item_name", itemName);
            item.put("unit",      unit);
            categoryMap.get(category).add(item);
        }
    }

    // ---------------------------------------------------------------
    // Step 2 — Build category tabs and show first category's items
    // ---------------------------------------------------------------

    private void buildCategoryTabs() {
        categoryTabsContainer.removeAllViews();

        boolean isFirst = true;
        for (String category : categoryMap.keySet()) {
            TextView tab = makeCategoryTab(category);
            categoryTabsContainer.addView(tab);

            if (isFirst) {
                selectCategory(tab, category);
                isFirst = false;
            }
        }
    }

    private TextView makeCategoryTab(String category) {
        TextView tab = new TextView(this);
        tab.setText(category);
        tab.setTextSize(13f);
        tab.setTypeface(null, Typeface.BOLD);
        tab.setTextColor(Color.parseColor("#AACDD8"));

        int hPad = dp(16);
        int vPad = dp(8);
        tab.setPadding(hPad, vPad, hPad, vPad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        tab.setLayoutParams(lp);

        tab.setOnClickListener(v -> selectCategory(tab, category));
        return tab;
    }

    // ---------------------------------------------------------------
    // Step 3 — On category tap: highlight tab, repopulate item grid
    // ---------------------------------------------------------------

    private void selectCategory(TextView selected, String category) {
        // Reset all tabs
        for (int i = 0; i < categoryTabsContainer.getChildCount(); i++) {
            View child = categoryTabsContainer.getChildAt(i);
            if (child instanceof TextView) {
                child.setBackgroundColor(Color.TRANSPARENT);
                ((TextView) child).setTextColor(Color.parseColor("#AACDD8"));
            }
        }

        // Highlight selected tab
        selected.setBackgroundColor(Color.parseColor("#8A6F5A"));
        selected.setTextColor(Color.WHITE);
        selectedTab = selected;

        // Clear previous item selection when switching categories
        selectedItemId = null;
        selectedPanel  = null;

        // Repopulate items
        buildItemGrid(categoryMap.get(category));
    }

    // ---------------------------------------------------------------
    // Step 4 — Build item panels in a 2-column grid
    // ---------------------------------------------------------------

    private void buildItemGrid(List<Map<String, String>> items) {
        itemGridContainer.removeAllViews();

        // Build rows of 2
        for (int i = 0; i < items.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);

            // Left panel
            row.addView(makeItemPanel(items.get(i), true));

            // Right panel (may not exist for the last odd item)
            if (i + 1 < items.size()) {
                row.addView(makeItemPanel(items.get(i + 1), false));
            } else {
                // Empty spacer so left panel stays half-width
                View spacer = new View(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                sp.setMarginStart(dp(8));
                spacer.setLayoutParams(sp);
                row.addView(spacer);
            }

            itemGridContainer.addView(row);
        }
    }

    private LinearLayout makeItemPanel(Map<String, String> item, boolean isLeft) {
        String itemId   = item.get("item_id");
        String itemName = item.get("item_name");
        String unit     = item.get("unit");

        // Outer panel (LinearLayout so it holds two TextViews)
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Color.WHITE);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setClickable(true);
        panel.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (isLeft) lp.setMarginEnd(dp(8));
        panel.setLayoutParams(lp);

        // Item name
        TextView tvName = new TextView(this);
        tvName.setText(itemName);
        tvName.setTextSize(13f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        panel.addView(tvName);

        // Unit label (smaller, grey)
        if (unit != null && !unit.isEmpty()) {
            TextView tvUnit = new TextView(this);
            tvUnit.setText(unit);
            tvUnit.setTextSize(11f);
            tvUnit.setTextColor(Color.parseColor("#8A8A8A"));
            LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            unitLp.topMargin = dp(2);
            tvUnit.setLayoutParams(unitLp);
            panel.addView(tvUnit);
        }

        panel.setOnClickListener(v -> selectItem(panel, itemId, itemName));
        return panel;
    }

    // ---------------------------------------------------------------
    // Step 5 — On item tap: highlight panel, store item_id
    // ---------------------------------------------------------------

    private void selectItem(LinearLayout selected, String itemId, String itemName) {
        // Reset previously selected panel
        if (selectedPanel instanceof LinearLayout) {
            selectedPanel.setBackgroundColor(Color.WHITE);
            tintPanelText((LinearLayout) selectedPanel,
                    Color.parseColor("#1A1A1A"), Color.parseColor("#8A8A8A"));
        }

        // Apply selected style
        selected.setBackgroundColor(Color.parseColor("#2F3E46"));
        tintPanelText(selected, Color.WHITE, Color.parseColor("#AACDD8"));

        selectedPanel  = selected;
        selectedItemId = itemId;

        android.util.Log.d("NEED_SELECT", "item=" + itemName + " id=" + itemId);
    }

    /** Tints the name (first child) and unit (second child) of a panel. */
    private void tintPanelText(LinearLayout panel, int nameColor, int unitColor) {
        for (int i = 0; i < panel.getChildCount(); i++) {
            View child = panel.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(i == 0 ? nameColor : unitColor);
            }
        }
    }

    // ---------------------------------------------------------------
    // Quantity display
    // ---------------------------------------------------------------

    private void refreshQtyDisplay() {
        tvQtyDisplay.setText(String.valueOf(currentQty));
    }

    // ---------------------------------------------------------------
    // Submit
    //   POST /api/auth/need.php — user_id, item_id, quantity, note
    // ---------------------------------------------------------------

    private void submitNeed() {
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedItemId == null) {
            Toast.makeText(this, "Please select an item first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();

        android.util.Log.d("NEED_SEND",
                "user_id=" + userId + " item_id=" + selectedItemId + " qty=" + currentQty);

        setLoading(true);

        RequestBody formBody = new FormBody.Builder()
                .add("user_id",  String.valueOf(userId))
                .add("item_id",  selectedItemId)
                .add("quantity", String.valueOf(currentQty))
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
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status", ""))) {
                            Toast.makeText(AddNeedActivity.this,
                                    "Need submitted!", Toast.LENGTH_SHORT).show();
                            finish();
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

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void setLoading(boolean loading) {
        btnSubmitNeed.setEnabled(!loading);
    }

    /** Converts dp to pixels for the current display. */
    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
}