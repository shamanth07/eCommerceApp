package com.example.ecommerceapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecommerceapp.Admin.AdminCategoryActivity;
import com.example.ecommerceapp.Model.Users;
import com.example.ecommerceapp.Prevalent.Prevalent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.paperdb.Paper;

public class LoginActivity extends AppCompatActivity {

    private EditText InputEmail, InputPassword;
    private Button LoginButton;
    private TextView AdminLink, NotAdminLink, ForgetPasswordLink;
    private ProgressDialog loadingBar;
    private CheckBox chkBoxRememberMe;
    private FirebaseAuth firebaseAuth;

    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        LoginButton = findViewById(R.id.login_btn);
        InputEmail = findViewById(R.id.login_email_input);
        InputPassword = findViewById(R.id.login_password_input);
        AdminLink = findViewById(R.id.admin_panel_link);
        NotAdminLink = findViewById(R.id.not_admin_panel_link);
        ForgetPasswordLink = findViewById(R.id.forget_password_link);
        loadingBar = new ProgressDialog(this);

        Paper.init(this);

        LoginButton.setOnClickListener(view -> loginUser());

        ForgetPasswordLink.setOnClickListener(view -> showResetPasswordDialog());

        AdminLink.setOnClickListener(view -> {
            isAdminMode = true;
            LoginButton.setText("Login Admin");
            AdminLink.setVisibility(View.INVISIBLE);
            NotAdminLink.setVisibility(View.VISIBLE);
        });

        NotAdminLink.setOnClickListener(view -> {
            isAdminMode = false;
            LoginButton.setText("Login");
            AdminLink.setVisibility(View.VISIBLE);
            NotAdminLink.setVisibility(View.INVISIBLE);
        });
    }

    private void loginUser() {
        String email = InputEmail.getText().toString().trim();
        String password = InputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle("Login Account");
            loadingBar.setMessage("Please wait while we check your credentials.");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        loadingBar.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                if (isAdminMode) {
                                    checkIfAdmin(user.getEmail());
                                } else {
                                    checkIfUser(user.getEmail());
                                }
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void checkIfAdmin(String email) {
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference().child("Admins");

        adminRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(LoginActivity.this, "Welcome Admin, you are logged in successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, AdminCategoryActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "No admin account found with this email.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfUser(String email) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        userRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        Users user = userSnapshot.getValue(Users.class);
                        Prevalent.currentOnlineUser = user;
                        Prevalent.currentOnlineUserEmail = user.getEmail();
                    }

                    Toast.makeText(LoginActivity.this, "Logged in successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "No user account found with this email.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

private void showResetPasswordDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_reset_password, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final EditText emailInput = dialogView.findViewById(R.id.reset_email_input);
        Button sendResetLinkButton = dialogView.findViewById(R.id.send_reset_link_btn);

        AlertDialog dialog = builder.create();
        dialog.show();

        sendResetLinkButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            } else {
                sendPasswordResetEmail(email);
                dialog.dismiss();
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
