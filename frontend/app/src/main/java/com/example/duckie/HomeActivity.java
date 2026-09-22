package com.example.duckie;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;


public class HomeActivity extends AppCompatActivity {

    Button profileBtn, searchBtn, cartBtn, logoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Βρες το TextView
        TextView welcomeTextView = findViewById(R.id.welcomeTextView);

        // Πάρε το username από το Intent
        String username = getIntent().getStringExtra("username");

        // Αν το username δεν είναι null, άλλαξε το κείμενο του TextView
        if (username != null && !username.isEmpty()) {
            welcomeTextView.setText("Καλώς ήρθες, " + username + "!");
        }

        // Κουμπιά
        profileBtn = findViewById(R.id.profileBtn);
        searchBtn = findViewById(R.id.searchBtn);
        cartBtn = findViewById(R.id.cartBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, com.example.duckie.ProfileActivity.class);
            intent.putExtra("username", username); // απλώς χρησιμοποίησε την υπάρχουσα μεταβλητή
            startActivity(intent);
        });

        searchBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, com.example.duckie.SearchActivity.class);
            startActivity(intent);
        });

        cartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, com.example.duckie.CartActivity.class);
            startActivity(intent);
        });
        logoutBtn.setOnClickListener(v -> {
            Intent intent = new Intent(com.example.duckie.HomeActivity.this, com.example.duckie.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

}
