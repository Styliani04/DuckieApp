package com.example.duckie;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UpdateStockActivity extends AppCompatActivity {

    private EditText editTextProductId;
    private EditText editTextNewStock;
    private Button btnUpdateStockConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_stock);

        editTextProductId = findViewById(R.id.editTextProductId);
        editTextNewStock = findViewById(R.id.editTextNewStock);
        btnUpdateStockConfirm = findViewById(R.id.btnUpdateStockConfirm);

        btnUpdateStockConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productId = editTextProductId.getText().toString().trim();
                String newStockStr = editTextNewStock.getText().toString().trim();

                if (productId.isEmpty() || newStockStr.isEmpty()) {
                    Toast.makeText(UpdateStockActivity.this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Εδώ θα μπει ο κώδικας για την αποθήκευση του αποθέματος στη βάση
                int newStock = Integer.parseInt(newStockStr);

                // Προς το παρόν, εμφανίζουμε απλώς ένα μήνυμα
                Toast.makeText(UpdateStockActivity.this, "Το απόθεμα ενημερώθηκε!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
