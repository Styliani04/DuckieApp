package com.example.duckie;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddProductActivity extends AppCompatActivity {

    private EditText editTextProductName, editTextProductPrice, editTextProductStock;
    private Button btnAddProductConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        editTextProductName = findViewById(R.id.editTextProductName);
        editTextProductPrice = findViewById(R.id.editTextProductPrice);
        editTextProductStock = findViewById(R.id.editTextProductStock);
        btnAddProductConfirm = findViewById(R.id.btnAddProductConfirm);

        btnAddProductConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextProductName.getText().toString().trim();
                String priceStr = editTextProductPrice.getText().toString().trim();
                String stockStr = editTextProductStock.getText().toString().trim();

                if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
                    Toast.makeText(AddProductActivity.this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
                    return;
                }

                double price = Double.parseDouble(priceStr);
                int stock = Integer.parseInt(stockStr);

                // Εδώ θα μπει ο κώδικας για αποθήκευση του προϊόντος σε βάση ή λίστα

                Toast.makeText(AddProductActivity.this, "Το προϊόν προστέθηκε!", Toast.LENGTH_SHORT).show();

                // Προαιρετικά: Καθάρισμα των πεδίων
                editTextProductName.setText("");
                editTextProductPrice.setText("");
                editTextProductStock.setText("");
            }
        });
    }
}
