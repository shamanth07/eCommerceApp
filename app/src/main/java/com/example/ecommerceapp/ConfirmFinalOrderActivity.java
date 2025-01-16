package com.example.ecommerceapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Date;
import java.util.HashMap;

public class ConfirmFinalOrderActivity extends AppCompatActivity {

    // UI components
    private EditText nameEditText, phoneEditText;
    private AutoCompleteTextView addressEditText;
    private Button confirmOrderBtn;

    // Firebase
    private DatabaseReference userRef, cartRef;

    // Stripe Payment variables
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;
    private final String stripePublishableKey = "pk_test_51QeVY6LgoAKhLV6i5KxwluTp0aElL2hQ4KpXDzZSY5fXa2efrUrX0WNT98o3cMFkhf9az1r8lwVCWOGS4KkUJkE800ba24Pq43";

    // Your backend URL exposed via ngrok
    private final String backendUrl = "https://3e8e-70-26-192-21.ngrok-free.app";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_final_order);






        nameEditText = findViewById(R.id.nameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        confirmOrderBtn = findViewById(R.id.confirmOrderBtn);

        String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        cartRef = FirebaseDatabase.getInstance().getReference().child("Cart List").child("User View").child(currentUserID).child("Products");
        double totalAmount = getIntent().getDoubleExtra("Total Price", 0.0);


        // Initialize Stripe Payment
        PaymentConfiguration.init(getApplicationContext(), stripePublishableKey);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDhZS9OGON-I9ZTpKXBeAhVmXwIcXDEfc8");
        }
        PlacesClient placesClient = Places.createClient(this);

        // Setup address autocomplete
        setupAutoComplete(placesClient);

        confirmOrderBtn.setOnClickListener(v -> validateAndProcessPayment(totalAmount));
    }

    private void setupAutoComplete(PlacesClient placesClient) {
        addressEditText.setAdapter(new PlaceAutoSuggestAdapter(this, placesClient));
        addressEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedAddress = (String) parent.getItemAtPosition(position);
            addressEditText.setText(selectedAddress);
        });
    }

    private void validateAndProcessPayment(double totalAmount) {
        String name = nameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        String address = addressEditText.getText().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please enter your phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please enter your address.", Toast.LENGTH_SHORT).show();
            return;
        }

        createPaymentIntent(totalAmount);
    }


        private void createPaymentIntent(double amount) {
        // Convert amount to cents
        int amountInCents = (int) (amount * 100);

        // Create a request to your backend
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("amount", amountInCents);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = backendUrl + "/create-payment-intent";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        paymentIntentClientSecret = response.getString("clientSecret");

                        // Present the PaymentSheet
                        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration(
                                "E-Commerce App"
                        );

                        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Log the error response
                    error.printStackTrace();
                    Toast.makeText(ConfirmFinalOrderActivity.this, "Failed to create PaymentIntent", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();

            // Save order details to Firebase
            saveOrderDetailsToFirebase();

            // Redirect to HomeActivity after saving the order
            Intent intent = new Intent(ConfirmFinalOrderActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrderDetailsToFirebase() {
        String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(currentUserID);
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference().child("Cart List").child("User View").child(currentUserID);

        String orderID = ordersRef.push().getKey();

        HashMap<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderID", orderID);
        orderMap.put("name", nameEditText.getText().toString());
        orderMap.put("phone", phoneEditText.getText().toString());
        orderMap.put("address", addressEditText.getText().toString());
        orderMap.put("totalAmount", getIntent().getDoubleExtra("Total Price", 0.0));
        orderMap.put("date", java.text.DateFormat.getDateInstance().format(new Date()));
        orderMap.put("time", java.text.DateFormat.getTimeInstance().format(new Date()));

        // Save order details to Firebase
        ordersRef.child(orderID).updateChildren(orderMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Clear the cart after saving the order
                clearCart(cartRef);

                Toast.makeText(ConfirmFinalOrderActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ConfirmFinalOrderActivity.this, "Failed to save order.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearCart(DatabaseReference cartRef) {
        cartRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ConfirmFinalOrderActivity.this, "Cart emptied successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ConfirmFinalOrderActivity.this, "Failed to empty the cart.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
