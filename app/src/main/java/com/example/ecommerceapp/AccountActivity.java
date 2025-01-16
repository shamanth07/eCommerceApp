package com.example.ecommerceapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class AccountActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText editFullName, editEmail, editPhone;
    private Button saveButton, resetPasswordButton, uploadImageButton;
    private ImageView profileImage;

    private DatabaseReference userRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private StorageReference storageRef;
    private ProgressDialog loadingBar;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        if (currentUser == null) {
            finish();
            return;
        }

        // Initialize Views
        profileImage = findViewById(R.id.profile_image);
        editFullName = findViewById(R.id.edit_full_name);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        saveButton = findViewById(R.id.save_button);
        resetPasswordButton = findViewById(R.id.reset_password_button);
        uploadImageButton = findViewById(R.id.upload_image_button);
        loadingBar = new ProgressDialog(this);

        // Load user data
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        loadUserData();

        // Save changes
        saveButton.setOnClickListener(v -> saveUserData());

        // Reset Password
        resetPasswordButton.setOnClickListener(v -> resetPassword());

        // Upload Image
        uploadImageButton.setOnClickListener(v -> openImagePicker());

        profileImage.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri != null) {
            loadingBar.setTitle("Uploading Image");
            loadingBar.setMessage("Please wait while we are uploading your profile image...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            final StorageReference fileRef = storageRef.child(currentUser.getUid() + ".jpg");
            fileRef.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        fileRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String imageUrl = task.getResult().toString();
                                    saveProfileImageUrl(imageUrl);
                                }
                            }
                        });
                    } else {
                        loadingBar.dismiss();
                        Toast.makeText(AccountActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void saveProfileImageUrl(String imageUrl) {
        userRef.child("profileImage").setValue(imageUrl).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                loadingBar.dismiss();
                Toast.makeText(AccountActivity.this, "Profile Image updated successfully.", Toast.LENGTH_SHORT).show();
            } else {
                loadingBar.dismiss();
                Toast.makeText(AccountActivity.this, "Failed to update profile image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String imageUrl = snapshot.child("profileImage").getValue(String.class);

                    editFullName.setText(fullName);
                    editEmail.setText(email);
                    editPhone.setText(phone);

                    if (imageUrl != null) {
                        Picasso.get().load(imageUrl).placeholder(R.drawable.profile).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AccountActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData() {
        String fullName = editFullName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Full Name is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Phone Number is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", fullName);
        userMap.put("phone", phone);

        userRef.updateChildren(userMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(AccountActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AccountActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

        private void resetPassword() {
        auth.sendPasswordResetEmail(currentUser.getEmail())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AccountActivity.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to send password reset email.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
