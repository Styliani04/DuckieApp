package com.example.duckie;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddStoreActivity extends AppCompatActivity {

    private EditText editTextStoreName;
    private Button buttonAddStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_store);

        editTextStoreName = findViewById(R.id.editTextStoreName);
        buttonAddStore = findViewById(R.id.buttonAddStore);

        buttonAddStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String storeName = editTextStoreName.getText().toString().trim();

                if (storeName.isEmpty()) {
                    Toast.makeText(AddStoreActivity.this, "Παρακαλώ εισάγετε το όνομα του καταστήματος", Toast.LENGTH_SHORT).show();
                } else {
                    // Εδώ μπορείς να κάνεις ό,τι θες με το όνομα (π.χ. αποθήκευση στη βάση ή επιστροφή στο προηγούμενο activity)
                    Toast.makeText(AddStoreActivity.this, "Κατάστημα \"" + storeName + "\" προστέθηκε!", Toast.LENGTH_SHORT).show();

                    // Π.χ. κλείσιμο activity μετά την προσθήκη:
                    finish();
                }
            }
        });
    }
}
