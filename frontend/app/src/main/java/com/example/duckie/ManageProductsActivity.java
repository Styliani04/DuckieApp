package com.example.duckie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ManageProductsActivity extends AppCompatActivity {

    private ListView listViewProducts;
    private Button buttonAddProduct;
    private ArrayList<String> products;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);

        listViewProducts = findViewById(R.id.listViewProducts);
        buttonAddProduct = findViewById(R.id.buttonAddProduct);

        // Αρχική λίστα προϊόντων (μπορεί να την φορτώσεις από βάση ή API)
        products = new ArrayList<>();
        products.add("Παράδειγμα Προϊόντος 1");
        products.add("Παράδειγμα Προϊόντος 2");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, products);
        listViewProducts.setAdapter(adapter);

        listViewProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String product = products.get(position);
                Toast.makeText(ManageProductsActivity.this, "Επέλεξες: " + product, Toast.LENGTH_SHORT).show();
                // Εδώ μπορείς να ανοίξεις activity για επεξεργασία ή να προσθέσεις επιλογές διαγραφής κλπ.
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            String newProduct = data.getStringExtra("productName");
            if (newProduct != null && !newProduct.isEmpty()) {
                products.add(newProduct);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Προστέθηκε το προϊόν: " + newProduct, Toast.LENGTH_SHORT).show();
            }
        }
    }
}