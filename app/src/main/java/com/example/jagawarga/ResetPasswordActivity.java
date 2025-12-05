package com.example.jagawarga;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText inputPhone, inputPass;
    private Button btnReset;
    private ImageButton btnBack;
    private ImageView btnTogglePass;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initViews();
        setupListeners();

        // Since we can only reset current user's password easily with Firebase Client SDK,
        // we check if user is logged in. If not, we can't do much with "Fake Email" auth.
        // We will assume this is "Change Password" feature for logged in user.
        // If meant for "Forgot Password", it's limited.

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Fitur ini hanya untuk mengubah password saat login. Jika lupa password, hubungi Admin/RT.", Toast.LENGTH_LONG).show();
            // finish(); // Don't close immediately so they can read toast, but maybe disable button
            btnReset.setEnabled(false);
        } else {
             inputPhone.setText(user.getEmail()); // Just show email/ID
             inputPhone.setEnabled(false); // Can't change target user
        }
    }

    private void initViews() {
        inputPhone = findViewById(R.id.inputPhoneWarga);
        inputPass = findViewById(R.id.inputPass);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);
        btnTogglePass = findViewById(R.id.btnTogglePass);

        btnReset.setText("Ubah Password");
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnTogglePass.setOnClickListener(v -> {
            if (isPasswordVisible) {
                inputPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePass.setImageResource(R.drawable.icon_mata2);
            } else {
                inputPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePass.setImageResource(R.drawable.icon_mata1);
            }
            inputPass.setSelection(inputPass.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        btnReset.setOnClickListener(v -> {
            String newPass = inputPass.getText().toString().trim();

            if (newPass.isEmpty()) {
                Toast.makeText(this, "Isi password baru!", Toast.LENGTH_SHORT).show();
            } else {
                changePassword(newPass);
            }
        });
    }

    private void changePassword(String newPass) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updatePassword(newPass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
