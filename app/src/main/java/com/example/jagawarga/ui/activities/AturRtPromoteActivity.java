package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.data.repository.AdminRepository;
import com.example.jagawarga.utils.Constants;

public class AturRtPromoteActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etIdWarga, etTelepon;
    private Button btnAction;
    private Button tabPromosikan, tabTurunkan;

    // MVVM
    private AdminRepository adminRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_rt);

        adminRepository = new AdminRepository();

        btnBack = findViewById(R.id.btnBack);
        etIdWarga = findViewById(R.id.inputIDWarga);
        etTelepon = findViewById(R.id.inputPhoneWarga);
        btnAction = findViewById(R.id.btnActionPromote);
        tabPromosikan = findViewById(R.id.btnTabPromote);
        tabTurunkan = findViewById(R.id.btnTabRevoke);

        btnBack.setOnClickListener(v -> finish());

        // Tab navigation
        tabTurunkan.setOnClickListener(v -> {
            startActivity(new Intent(this, AturRtRevokeActivity.class));
            finish();
        });

        btnAction.setOnClickListener(v -> {
            String rawPhone = etTelepon.getText().toString().trim();

            if (rawPhone.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_phone_required), Toast.LENGTH_SHORT).show();
                return;
            }

            String formatted = AdminRepository.formatPhoneNumber(rawPhone);

            adminRepository.changeUserRole(formatted, Constants.ROLE_KETUA_RT,
                    new AdminRepository.FindUserCallback() {
                        @Override
                        public void onFound(String uid) {
                            /* user found, update in progress */ }

                        @Override
                        public void onNotFound() {
                            Toast.makeText(AturRtPromoteActivity.this,
                                    getString(R.string.toast_user_not_found), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String msg) {
                            Toast.makeText(AturRtPromoteActivity.this,
                                    getString(R.string.toast_search_user_failed, msg),
                                    Toast.LENGTH_SHORT).show();
                        }
                    },
                    new AdminRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AturRtPromoteActivity.this,
                                    getString(R.string.toast_promote_success), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String msg) {
                            Toast.makeText(AturRtPromoteActivity.this,
                                    getString(R.string.toast_update_failed, msg),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
