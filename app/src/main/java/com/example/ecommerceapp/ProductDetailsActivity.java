package com.example.ecommerceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.Model.Products;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class ProductDetailsActivity extends AppCompatActivity {

    private Button addToCartButton, btnIncrement, btnDecrement;
    private ImageView productImage, loveButton;
    private TextView productPrice, productDescription, productName, tvQuantity;
    private String productID = "", state = "Normal";
    private int quantity = 1;
    private final int MAX_QUANTITY = 10;
    private final int MIN_QUANTITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        productID = getIntent().getStringExtra("pid");

        addToCartButton = findViewById(R.id.pd_add_to_cart_button);
        btnIncrement = findViewById(R.id.btn_increment);
        btnDecrement = findViewById(R.id.btn_decrement);
        tvQuantity = findViewById(R.id.tv_quantity);
        productImage = findViewById(R.id.product_image_details);
        productName = findViewById(R.id.product_name_details);
        productDescription = findViewById(R.id.product_description_details);
        productPrice = findViewById(R.id.product_price_details);
        loveButton = findViewById(R.id.love_button);

        getProductDetails(productID);

        loveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loveButton.setSelected(!loveButton.isSelected());

                if (loveButton.isSelected()) {
                    loveButton.setBackgroundResource(R.drawable.ic_heart_filled);
                    addToWishlist();
                } else {
                    loveButton.setBackgroundResource(R.drawable.ic_heart_outline);
                    removeFromWishlist();
                }
            }
        });

        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity < MAX_QUANTITY) {
                    quantity++;
                    tvQuantity.setText(String.valueOf(quantity));
                }
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > MIN_QUANTITY) {
                    quantity--;
                    tvQuantity.setText(String.valueOf(quantity));
                }
            }
        });

        addToCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (state.equals("Order Placed") || state.equals("Order Shipped")) {
                    Toast.makeText(ProductDetailsActivity.this, "You can add more products once your order is shipped or confirmed.", Toast.LENGTH_LONG).show();
                } else {
                    addingToCartList();
                }
            }
        });
    }

    private void addToWishlist() {
        final DatabaseReference wishlistRef = FirebaseDatabase.getInstance().getReference().child("Wishlist");

        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_");

        final HashMap<String, Object> wishlistMap = new HashMap<>();
        wishlistMap.put("pid", productID);
        wishlistMap.put("pname", productName.getText().toString());
        wishlistMap.put("price", productPrice.getText().toString());
        wishlistMap.put("image", getProductImageUrl());

        wishlistRef.child(userEmail).child(productID)
                .updateChildren(wishlistMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProductDetailsActivity.this, "Added to Wishlist.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProductDetailsActivity.this, "Failed to add to Wishlist.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Helper method to get the product image URL
    private String getProductImageUrl() {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference().child("Products").child(productID);
        final String[] imageUrl = {null};

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    imageUrl[0] = snapshot.child("image").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        return imageUrl[0];
    }

    private void removeFromWishlist() {
        final DatabaseReference wishlistRef = FirebaseDatabase.getInstance().getReference().child("Wishlist");

        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_");

        wishlistRef.child(userEmail).child(productID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ProductDetailsActivity.this, "Removed from Wishlist.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProductDetailsActivity.this, "Failed to remove from Wishlist.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addingToCartList() {
        String saveCurrentTime, saveCurrentDate;

        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calForDate.getTime());

        final DatabaseReference cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");

        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_");

        final HashMap<String, Object> cartMap = new HashMap<>();
        cartMap.put("pid", productID);
        cartMap.put("pname", productName.getText().toString());
        cartMap.put("price", productPrice.getText().toString());
        cartMap.put("date", saveCurrentDate);
        cartMap.put("time", saveCurrentTime);
        cartMap.put("quantity", String.valueOf(quantity));
        cartMap.put("discount", "");

        cartListRef.child("User View").child(userEmail).child("Products").child(productID)
                .updateChildren(cartMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProductDetailsActivity.this, "Added to Cart List.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(ProductDetailsActivity.this, HomeActivity.class));
                        } else {
                            Toast.makeText(ProductDetailsActivity.this, "Failed to add to cart.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getProductDetails(String productID) {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference().child("Products");
        DatabaseReference wishlistRef = FirebaseDatabase.getInstance().getReference().child("Wishlist")
                .child(FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_"));

        // Check if the product is in the wishlist
        wishlistRef.child(productID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    loveButton.setSelected(true);
                    loveButton.setBackgroundResource(R.drawable.ic_heart_filled);
                } else {
                    loveButton.setSelected(false);
                    loveButton.setBackgroundResource(R.drawable.ic_heart_outline);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProductDetailsActivity.this, "Failed to check wishlist.", Toast.LENGTH_SHORT).show();
            }
        });

        // Load product details
        productsRef.child(productID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Products products = dataSnapshot.getValue(Products.class);
                    productName.setText(products.getPname());
                    productPrice.setText(products.getPrice());
                    productDescription.setText(products.getDescription());
                    Picasso.get().load(products.getImage()).into(productImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

}
