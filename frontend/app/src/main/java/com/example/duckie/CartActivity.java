package com.example.duckie;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    ListView cartListView;
    Button checkoutBtn;

    // Για παράδειγμα, λίστα προϊόντων στο καλάθι
    ArrayList<String> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartListView = findViewById(R.id.cartListView);
        checkoutBtn = findViewById(R.id.checkoutBtn);

        // Προσθέτουμε προσωρινά μερικά δείγματα στο καλάθι
        cartItems.add("Pizza Margherita");
        cartItems.add("Burger με Πατάτες");
        cartItems.add("Χυμός Πορτοκάλι");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                cartItems
        );

        cartListView.setAdapter(adapter);

        checkoutBtn.setOnClickListener(v -> {
            // Εδώ μπορεί να υλοποιήσεις την παραγγελία
            Toast.makeText(this, "Παραγγελία ολοκληρώθηκε!", Toast.LENGTH_SHORT).show();
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
