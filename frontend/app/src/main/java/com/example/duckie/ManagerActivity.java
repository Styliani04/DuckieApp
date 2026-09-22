package com.example.duckie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ManagerActivity extends AppCompatActivity {

    private Button btnAddStore;
    private Button btnUpdateStock;
    private Button btnAddProduct;
    private Button btnDeleteProduct;
    private Button btnShowSales;
    private Button btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        btnAddStore = findViewById(R.id.btnAddStore);
        btnUpdateStock = findViewById(R.id.btnUpdateStock);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnDeleteProduct = findViewById(R.id.btnDeleteProduct);
        btnShowSales = findViewById(R.id.btnShowSales);
        btnExit = findViewById(R.id.btnExit);

        btnAddStore.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, AddStoreActivity.class);
            startActivity(intent);
        });
        btnUpdateStock.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, UpdateStockActivity.class);
            startActivity(intent);
        });
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, AddProductActivity.class);
            startActivity(intent);
        });
        btnDeleteProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, DeleteProductActivity.class);
            startActivity(intent);
        });
        btnExit.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}