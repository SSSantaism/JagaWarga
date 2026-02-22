package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.PrefUtils;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_reset_password_info), Toast.LENGTH_LONG).show();
            btnReset.setEnabled(false);
        } else {
            inputPhone.setText(user.getEmail());
            inputPhone.setEnabled(false);
        }
    }

    private void initViews() {
        inputPhone = findViewById(R.id.inputPhoneWarga);
        inputPass = findViewById(R.id.inputPass);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);
        btnTogglePass = findViewById(R.id.btnTogglePass);

        btnReset.setText(getString(R.string.btn_ubah_password));
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
                Toast.makeText(this, getString(R.string.toast_fill_new_password), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, getString(R.string.toast_password_changed), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.toast_password_change_failed, task.getException().getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
