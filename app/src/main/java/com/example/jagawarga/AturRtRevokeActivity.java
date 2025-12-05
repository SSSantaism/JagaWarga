package com.example.jagawarga;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AturRtRevokeActivity extends AppCompatActivity {

    private EditText etIdWarga, etTelepon;
    private ImageButton btnBack;
    private Button btnAction;
    private TextView tabPromosikan, tabTurunkan;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revoke_rt);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupTabs();
        setupButton();
        setupBackButton();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        etIdWarga   = findViewById(R.id.inputIDWarga);
        etTelepon   = findViewById(R.id.inputPhoneWarga);
        btnAction   = findViewById(R.id.btnActionRevoke);

        tabPromosikan = findViewById(R.id.btnTabPromote);
        tabTurunkan   = findViewById(R.id.btnTabRevoke);

        btnAction.setText("Turunkan");
        etIdWarga.setHint("Nama (Opsional)");
    }

    private void setupTabs() {
        tabPromosikan.setOnClickListener(v -> {
            startActivity(new Intent(AturRtRevokeActivity.this, AturRtPromoteActivity.class));
            finish();
        });

        tabTurunkan.setOnClickListener(v -> {});
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupButton() {
        btnAction.setOnClickListener(v -> {
            String telepon = etTelepon.getText().toString().trim();

            if (telepon.isEmpty()) {
                etTelepon.setError("No. telepon wajib diisi");
                etTelepon.requestFocus();
                return;
            }

            if (telepon.startsWith("0")) telepon = "+62" + telepon.substring(1);
            else if (!telepon.startsWith("+")) telepon = "+62" + telepon;

            revokeUser(telepon);
        });
    }

    private void revokeUser(String phone) {
        db.collection("users")
                .whereEqualTo("telepon", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String uid = doc.getId();
                        db.collection("users").document(uid)
                                .update("role", "Warga")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Berhasil diturunkan jadi Warga", Toast.LENGTH_LONG).show();
                                    etTelepon.setText("");
                                    etIdWarga.setText("");
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error cari user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
