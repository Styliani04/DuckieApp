package com.example.duckie;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DeleteProductActivity extends AppCompatActivity {

    private EditText editTextProductId;
    private Button btnDeleteProductConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_product);

        editTextProductId = findViewById(R.id.editTextProductId);
        btnDeleteProductConfirm = findViewById(R.id.btnDeleteProductConfirm);

        btnDeleteProductConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productId = editTextProductId.getText().toString().trim();

                if (productId.isEmpty()) {
                    Toast.makeText(DeleteProductActivity.this, "Συμπλήρωσε το όνομα ή ID του προϊόντος", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Εδώ θα γίνει ο έλεγχος και η διαγραφή από τη βάση ή λίστα

                Toast.makeText(DeleteProductActivity.this, "Το προϊόν διαγράφηκε (προσωρινά)", Toast.LENGTH_SHORT).show();

                editTextProductId.setText("");
            }
        });
    }
}
