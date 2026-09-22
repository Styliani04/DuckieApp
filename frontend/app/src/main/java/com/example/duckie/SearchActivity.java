package com.example.duckie;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {

    EditText searchBox;
    Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search); // Θα το φτιάξουμε παρακάτω

        searchBox = findViewById(R.id.searchBox);
        searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String query = searchBox.getText().toString();
            // Προς το παρόν απλώς εμφανίζει μήνυμα
            Toast.makeText(this, "Αναζήτηση για: " + query, Toast.LENGTH_SHORT).show();
        });
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
