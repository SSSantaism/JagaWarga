package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.utils.Constants;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cek apakah user sudah login sebelumnya
        if (checkSession()) {
            return;
        }

        setContentView(R.layout.activity_landing);

        Button btnMulai = findViewById(R.id.btnMulai);
        btnMulai.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean checkSession() {
        SessionManager session = new SessionManager(this);
        String savedId = session.getIdWarga();
        String savedRole = session.getRole();
        String savedNama = session.getNama();
        if (savedNama == null)
            savedNama = getString(R.string.fallback_name_user);

        if (savedId != null && savedRole != null) {
            redirectDashboard(savedRole, savedNama);
            return true;
        }
        return false;
    }

    private void redirectDashboard(String role, String namaUser) {
        Intent intent;

        if (role.equalsIgnoreCase(Constants.ROLE_KETUA_RT)) {
            intent = new Intent(this, DashboardRtActivity.class);
        } else if (role.equalsIgnoreCase(Constants.ROLE_KETUA_RW)) {
            intent = new Intent(this, DashboardRwActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }

        intent.putExtra(Constants.EXTRA_NAMA_USER, namaUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}