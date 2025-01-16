package com.example.ecommerceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.Model.Products;
import com.example.ecommerceapp.ViewHolder.ProductViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class WishlistActivity extends AppCompatActivity {

    private DatabaseReference WishlistRef;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        // Firebase Wishlist reference
        WishlistRef = FirebaseDatabase.getInstance().getReference()
                .child("Wishlist")
                .child(FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_"));

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recycler_wishlist);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Products> options =
                new FirebaseRecyclerOptions.Builder<Products>()
                        .setQuery(WishlistRef, Products.class)
                        .build();

        FirebaseRecyclerAdapter<Products, ProductViewHolder> adapter =
                new FirebaseRecyclerAdapter<Products, ProductViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull final Products model) {
                        holder.txtProductName.setText(model.getPname());
                        holder.txtProductPrice.setText("Price = $" + model.getPrice());

                        // Fetch image URL from Products node using pid
                        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference().child("Products");
                        productsRef.child(model.getPid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String imageUrl = snapshot.child("image").getValue(String.class);
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        Picasso.get().load(imageUrl).placeholder(R.drawable.hats).into(holder.imageView);
                                    } else {
                                        holder.imageView.setImageResource(R.drawable.hats);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(WishlistActivity.this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Navigate to ProductDetailsActivity on item click
                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(WishlistActivity.this, ProductDetailsActivity.class);
                            intent.putExtra("pid", model.getPid());
                            startActivity(intent);
                        });

                        // Delete button logic
                        Button deleteButton = holder.itemView.findViewById(R.id.delete_btn);
                        deleteButton.setVisibility(View.VISIBLE);

                        deleteButton.setOnClickListener(v -> {
                            WishlistRef.child(model.getPid()).removeValue()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(WishlistActivity.this, "Removed from Wishlist.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(WishlistActivity.this, "Failed to remove item.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        });

                        // Add to Cart button logic
                        Button addToCartButton = holder.itemView.findViewById(R.id.add_to_cart_btn);
                        addToCartButton.setVisibility(View.VISIBLE);

                        addToCartButton.setOnClickListener(v -> addToCart(model));
                    }

                    @NonNull
                    @Override
                    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wishlist_item_layout, parent, false);
                        return new ProductViewHolder(view);
                    }
                };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void addToCart(Products product) {
        DatabaseReference cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "_");

        cartListRef.child("User View").child(userEmail).child("Products").child(product.getPid())
                .setValue(product)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(WishlistActivity.this, "Added to Cart.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(WishlistActivity.this, "Failed to add to Cart.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
