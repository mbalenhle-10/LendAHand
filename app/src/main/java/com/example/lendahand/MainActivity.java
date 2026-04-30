package com.example.lendahand;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button signInButton;
    TextView createAccountText;

    String loginUrl = "http://13.135.14.204/api/auth/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailInput        = findViewById(R.id.emailInput);
        passwordInput     = findViewById(R.id.passwordInput);
        signInButton      = findViewById(R.id.signInButton);
        createAccountText = findViewById(R.id.createAccountText);

        signInButton.setOnClickListener(v -> {
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        createAccountText.setOnClickListener(v -> {
            // navigate to register screen when ready
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void loginUser(String email, String password) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                loginUrl,
                response -> {
                    try {
                        JSONObject json   = new JSONObject(response);
                        String status     = json.getString("status");

                        if (status.equals("success")) {
                            String username = json.getString("username");
                            int userId      = json.getInt("user_id");

                            Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();

                            // navigate to next screen after login
                            Intent intent = new Intent(this, DashBoardActivity.class);
                            startActivity(intent);
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
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}

