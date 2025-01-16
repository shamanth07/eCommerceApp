package com.example.ecommerceapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private Button CreateAccountButton;
    private EditText InputName, InputEmail, InputPassword;
    private CheckBox AdminCheckBox;
    private ProgressDialog loadingBar;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        CreateAccountButton = findViewById(R.id.register_btn);
        InputName = findViewById(R.id.register_username_input);
        InputEmail = findViewById(R.id.register_email_input);
        InputPassword = findViewById(R.id.register_password_input);
        AdminCheckBox = findViewById(R.id.admin_checkbox);
        loadingBar = new ProgressDialog(this);

        CreateAccountButton.setOnClickListener(view -> CreateAccount());
    }

    private void CreateAccount() {
        String name = InputName.getText().toString().trim();
        String email = InputEmail.getText().toString().trim();
        String password = InputPassword.getText().toString().trim();
        boolean isAdmin = AdminCheckBox.isChecked();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
        } else if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle("Creating Account");
            loadingBar.setMessage("Please wait, we are registering your account...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            registerUserWithEmail(name, email, password, isAdmin);
        }
    }

    private void registerUserWithEmail(String name, String email, String password, boolean isAdmin) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            if (isAdmin) {
                                saveAdminToDatabase(userId, name, email);
                            } else {
                                saveUserToDatabase(userId, name, email);
                            }
                        }
                    } else {
                        loadingBar.dismiss();

                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegisterActivity.this, "This email is already registered. Please use a different email.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("userType", "User");

        databaseReference.child("Users").child(userId).setValue(userMap)
                .addOnCompleteListener(task -> {
                    loadingBar.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "User account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Database error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveAdminToDatabase(String userId, String name, String email) {
        HashMap<String, Object> adminMap = new HashMap<>();
        adminMap.put("uid", userId);
        adminMap.put("name", name);
        adminMap.put("email", email);
        adminMap.put("userType", "Admin");

        databaseReference.child("Admins").child(userId).setValue(adminMap)
                .addOnCompleteListener(task -> {
                    loadingBar.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Admin account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Database error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
