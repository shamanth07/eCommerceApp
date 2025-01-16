package com.example.ecommerceapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private String check = "";
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        check = getIntent().getStringExtra("check");
        firebaseAuth = FirebaseAuth.getInstance();

        // Show the email input dialog when in "login" mode
        if ("login".equals(check)) {
            showEmailResetDialog();
        }
    }

    private void showEmailResetDialog() {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_reset_password, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final EditText emailInput = dialogView.findViewById(R.id.reset_email_input);
        Button sendLinkButton = dialogView.findViewById(R.id.send_reset_link_btn);

        AlertDialog dialog = builder.create();
        dialog.show();

        sendLinkButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(ResetPasswordActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(ResetPasswordActivity.this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
