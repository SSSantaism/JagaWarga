package com.example.jagawarga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.utils.Constants;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cek apakah user sudah login sebelumnya
        // Jika sudah, langsung arahkan ke Dashboard yang sesuai
        if (checkSession()) {
            return; // Hentikan eksekusi agar layout landing tidak perlu dirender
        }

        setContentView(R.layout.activity_landing);

        Button btnMulai = findViewById(R.id.btnMulai);
        btnMulai.setOnClickListener(v -> {
            // Pindah ke halaman Login
            Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Tutup LandingActivity agar tidak bisa kembali dengan tombol Back
        });
    }

    private boolean checkSession() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
        String savedId = prefs.getString(Constants.PREF_KEY_ID, null);
        String savedRole = prefs.getString(Constants.PREF_KEY_ROLE, null);
        String savedNama = prefs.getString(Constants.PREF_KEY_NAMA, getString(R.string.fallback_name_user));

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