package com.example.lendahand;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText nameInput, emailInput, passwordInput, phoneInput, contactInput, bioInput;
    Button registerButton;
    TextView loginText;

    String registerUrl = "http://13.135.14.204/api/auth/register.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        nameInput     = findViewById(R.id.nameInput);
        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        phoneInput    = findViewById(R.id.phoneInput);
        contactInput  = findViewById(R.id.contactInput);
        bioInput      = findViewById(R.id.bioInput);
        registerButton = findViewById(R.id.registerButton);
        loginText     = findViewById(R.id.loginText);

        registerButton.setOnClickListener(v -> {
            String name     = nameInput.getText().toString().trim();
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String phone    = phoneInput.getText().toString().trim();
            String contact  = contactInput.getText().toString().trim();
            String bio      = bioInput.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Name, email and password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(name, email, password, phone, contact, bio);
        });

        loginText.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void registerUser(String name, String email, String password,
                              String phone, String contact, String bio) {

        StringRequest request = new StringRequest(
                Request.Method.POST,
                registerUrl,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String status   = json.getString("status");

                        if (status.equals("success")) {
                            Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            String message = json.getString("message");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Response error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("username", email); // using email as username
                params.put("email", email);
                params.put("password", password);
                params.put("phone", phone);
                params.put("contact", contact);
                params.put("bio", bio);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
