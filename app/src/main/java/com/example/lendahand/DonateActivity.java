package com.example.lendahand;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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

public class DonateActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://13.135.14.204/api/auth/";


    private final LinkedHashMap<String, List<Map<String, String>>> categoryItemMap = new LinkedHashMap<>();
    private final List<String>            categoryNames = new ArrayList<>();
    private       List<Map<String, String>> currentItems  = new ArrayList<>();

    // Views
    private Spinner     spinnerCategory;
    private Spinner     spinnerItem;
    private EditText    editDonateQuantity;
    private Button      btnSubmitDonation;
    private ProgressBar progressBar;

    private final OkHttpClient httpClient = new OkHttpClient();

    // Session
    private SharedPreferences prefs;
    private int userId;
    private String username;

    // Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.donate_activity);

        initSession();
        bindViews();
        setupListeners();
        fetchItems();
    }

    // Session

    private void initSession(){
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
    }

    // View binding

    private void bindViews() {
        spinnerCategory    = findViewById(R.id.spinnerCategory);
        spinnerItem        = findViewById(R.id.spinnerItem);
        editDonateQuantity = findViewById(R.id.editDonateQuantity);
        btnSubmitDonation  = findViewById(R.id.btnSubmitDonation);
        progressBar        = findViewById(R.id.progressBar);
    }


    // Listeners

    private void setupListeners() {
        // When category changes, repopulate the item spinner
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categoryNames.get(position);
                currentItems = categoryItemMap.get(selectedCategory);
                populateItemSpinner(currentItems);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnSubmitDonation.setOnClickListener(v -> submitDonation());
    }

    //  Fetch predefined items from the server

    private void fetchItems() {
        setLoading(true);

        Request request = new Request.Builder()
                .url(BASE_URL + "items.php")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(DonateActivity.this,
                            "Could not load items. Check your connection.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    setLoading(false);
                    try {
                        parseAndPopulateItems(new JSONArray(body));
                    } catch (JSONException e) {
                        Toast.makeText(DonateActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Parse the flat item array → grouped by category

    private void parseAndPopulateItems(JSONArray jsonArray) throws JSONException {
        categoryItemMap.clear();
        categoryNames.clear();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj      = jsonArray.getJSONObject(i);
            String category     = obj.getString("category");
            String itemId       = obj.getString("item_id");
            String itemName     = obj.getString("item_name");
            String unit         = obj.optString("unit", "");

            // Group by category
            if (!categoryItemMap.containsKey(category)) {
                categoryItemMap.put(category, new ArrayList<>());
                categoryNames.add(category);
            }

            Map<String, String> item = new LinkedHashMap<>();
            item.put("item_id",   itemId);
            item.put("item_name", itemName);
            item.put("unit",      unit);
            categoryItemMap.get(category).add(item);
        }

        // Populate the category spinner
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Trigger the item spinner for the first category
        if (!categoryNames.isEmpty()) {
            currentItems = categoryItemMap.get(categoryNames.get(0));
            populateItemSpinner(currentItems);
        }
    }

    // Populate item spinner from the current category's item list

    private void populateItemSpinner(List<Map<String, String>> items) {
        List<String> displayNames = new ArrayList<>();
        for (Map<String, String> item : items) {
            String unit = item.get("unit");
            String label = item.get("item_name");
            if (unit != null && !unit.isEmpty()) {
                label += " (" + unit + ")";
            }
            displayNames.add(label);
        }

        ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                displayNames
        );
        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerItem.setAdapter(itemAdapter);
    }

    // ---------------------------------------------------------------
    // Step 2: Submit the donation
    //
    //  POST /api/donate.php
    //  Body (form): user_id, item_id, quantity
    // ---------------------------------------------------------------
    private void submitDonation() {
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String quantityStr = editDonateQuantity.getText().toString().trim();

        if (quantityStr.isEmpty()) {
            editDonateQuantity.setError("Enter a quantity");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            editDonateQuantity.setError("Enter a valid quantity (1 or more)");
            return;
        }

        if (currentItems == null || currentItems.isEmpty()) {
            Toast.makeText(this, "No item selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Grab the selected item_id
        int selectedItemPosition = spinnerItem.getSelectedItemPosition();
        String itemId = currentItems.get(selectedItemPosition).get("item_id");

        String userId = Integer.toString(this.userId);

        android.util.Log.d("DONATE_SEND", "user_id: " + userId);
        android.util.Log.d("DONATE_SEND", "item_id: " + itemId);
        android.util.Log.d("DONATE_SEND", "quantity: " + quantityStr);

        setLoading(true);
        btnSubmitDonation.setEnabled(false);

        RequestBody formBody = new FormBody.Builder()
                .add("user_id",  userId)
                .add("item_id",  itemId)
                .add("quantity", String.valueOf(quantity))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "donation.php")
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    btnSubmitDonation.setEnabled(true);
                    Toast.makeText(DonateActivity.this,
                            "Submission failed. Try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                android.util.Log.d("DONATE_RESPONSE", body); // debug line
                runOnUiThread(() -> {
                    setLoading(false);
                    btnSubmitDonation.setEnabled(true);
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.optString("status", ""))) {
                            Toast.makeText(DonateActivity.this,
                                    "Donation submitted!", Toast.LENGTH_SHORT).show();
                            editDonateQuantity.setText("");
                            spinnerCategory.setSelection(0);
                        } else {
                            String msg = json.optString("message", "Something went wrong.");
                            Toast.makeText(DonateActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(DonateActivity.this,
                                "Unexpected server response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------
    // Helper: toggle loading state
    // ---------------------------------------------------------------
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmitDonation.setEnabled(!loading);
    }
}