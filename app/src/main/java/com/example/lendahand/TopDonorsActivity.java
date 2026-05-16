package com.example.lendahand;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class TopDonorsActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> donorsList;

    private final String BASEURL =
            "http://13.135.14.204/api/auth/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_donors);

        listView = findViewById(R.id.topDonorsListView);

        donorsList = new ArrayList<>();

        loadTopDonors();
    }

    private void loadTopDonors() {

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASEURL + "top_donors.php",
                null,

                response -> {
                    try {

                        for (int i = 0; i < response.length(); i++) {

                            JSONObject obj = response.getJSONObject(i);

                            String fullname =
                                    obj.getString("fullname");

                            int total =
                                    obj.getInt("total_donated");

                            donorsList.add(
                                    fullname + " - " + total + " items donated"
                            );
                        }

                        TopDonorsAdapter adapter =
                                new TopDonorsAdapter(
                                        this,
                                        donorsList
                                );

                        listView.setAdapter(adapter);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> error.printStackTrace()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}