package com.example.ecommerceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.Model.Cart;
import com.example.ecommerceapp.Prevalent.Prevalent;
import com.example.ecommerceapp.ViewHolder.CartViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private TextView totalFeeTxt, taxTxt, totalTxt, emptyTxt;
    private Button nextBtn;
    private int subtotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.cart_list);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        totalFeeTxt = findViewById(R.id.totalFeeTxt);
        taxTxt = findViewById(R.id.taxTxt);
        totalTxt = findViewById(R.id.totalTxt);
        emptyTxt = findViewById(R.id.emptyTxt);
        nextBtn = findViewById(R.id.next_btn);

        nextBtn.setOnClickListener(v -> {
            String totalText = totalTxt.getText().toString();
            double totalAmount = Double.parseDouble(totalText.replace("Total: $", ""));
            Intent intent = new Intent(CartActivity.this, ConfirmFinalOrderActivity.class);
            intent.putExtra("Total Price", totalAmount);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkCartItems();
    }

    private void checkCartItems() {
        if (Prevalent.currentOnlineUser == null) {
            Toast.makeText(this, "Please log in to view your cart.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CartActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        String userEmail = Prevalent.currentOnlineUser.getEmail().replace(".", "_");
        DatabaseReference cartListRef = FirebaseDatabase.getInstance().getReference()
                .child("Cart List")
                .child("User View")
                .child(userEmail)
                .child("Products");

        FirebaseRecyclerOptions<Cart> options = new FirebaseRecyclerOptions.Builder<Cart>()
                .setQuery(cartListRef, Cart.class)
                .build();

        FirebaseRecyclerAdapter<Cart, CartViewHolder> adapter = new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull Cart model) {
                holder.txtProductName.setText(model.getPname() != null ? model.getPname() : "Unknown Product");

                String price = model.getPrice() != null ? model.getPrice() : "0";
                holder.txtProductPrice.setText("Price: $" + price);

                String quantity = model.getQuantity() != null ? model.getQuantity() : "1";
                holder.txtProductQuantity.setText(quantity);

                // Calculate subtotal safely
                try {
                    int itemTotal = Integer.parseInt(price) * Integer.parseInt(quantity);
                    subtotal += itemTotal;
                } catch (NumberFormatException e) {
                    Toast.makeText(CartActivity.this, "Error parsing price or quantity for " + model.getPname(), Toast.LENGTH_SHORT).show();
                }

                // Fetch product image
                DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference().child("Products");
                productsRef.child(model.getPid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String imageUrl = snapshot.child("image").getValue(String.class);
                            if (imageUrl != null) {
                                Picasso.get().load(imageUrl).placeholder(R.drawable.hats).into(holder.productImageView);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CartActivity.this, "Error fetching product details.", Toast.LENGTH_SHORT).show();
                    }
                });

                // Quantity Update Logic
                holder.btnIncrement.setOnClickListener(v -> {
                    int currentQuantity = Integer.parseInt(holder.txtProductQuantity.getText().toString());
                    currentQuantity++;
                    updateQuantity(model.getPid(), currentQuantity);
                });

                holder.btnDecrement.setOnClickListener(v -> {
                    int currentQuantity = Integer.parseInt(holder.txtProductQuantity.getText().toString());
                    if (currentQuantity > 1) {
                        currentQuantity--;
                        updateQuantity(model.getPid(), currentQuantity);
                    }
                });

                // Navigate to ProductDetailsActivity
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(CartActivity.this, ProductDetailsActivity.class);
                    intent.putExtra("pid", model.getPid());
                    startActivity(intent);
                });

                updateSummary();
            }

            @NonNull
            @Override
            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_items_layout, parent, false);
                return new CartViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) {
                    emptyTxt.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyTxt.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void updateQuantity(String productId, int newQuantity) {
        String userEmail = Prevalent.currentOnlineUser.getEmail().replace(".", "_");
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference()
                .child("Cart List")
                .child("User View")
                .child(userEmail)
                .child("Products")
                .child(productId);

        cartRef.child("quantity").setValue(String.valueOf(newQuantity)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(CartActivity.this, "Quantity updated.", Toast.LENGTH_SHORT).show();
                recreate(); // Refresh the activity to update the cart view
            } else {
                Toast.makeText(CartActivity.this, "Failed to update quantity.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary() {
        double tax = subtotal * 0.1;
        double total = subtotal + tax;

        totalFeeTxt.setText("Subtotal: $" + subtotal);
        taxTxt.setText("Total Tax: $" + String.format("%.2f", tax));
        totalTxt.setText("Total: $" + String.format("%.2f", total));
    }
}
