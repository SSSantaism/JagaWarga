package com.example.jagawarga;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AturRtPromoteActivity extends AppCompatActivity {

    private EditText etIdWarga, etTelepon; // Note: inputIDWarga might not be needed if we search by phone
    private Button btnAction;
    private ImageButton btnBack;
    private TextView tabPromosikan, tabTurunkan;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_rt);

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
        btnAction   = findViewById(R.id.btnActionPromote);

        tabPromosikan = findViewById(R.id.btnTabPromote);
        tabTurunkan   = findViewById(R.id.btnTabRevoke);

        btnAction.setText("Promosikan");

        // Hide ID Warga input if we just want to use Phone
        // etIdWarga.setVisibility(android.view.View.GONE); // Or keep it but it won't match anything easily in Firestore if it's numeric ID vs UID.
        // Let's assume user inputs Phone to find the user.
        etIdWarga.setHint("Nama (Opsional)"); // Repurpose as Name check? Or ignore.
    }

    private void setupTabs() {
        tabPromosikan.setOnClickListener(v -> {});

        tabTurunkan.setOnClickListener(v -> {
            startActivity(new Intent(AturRtPromoteActivity.this, AturRtRevokeActivity.class));
            finish();
        });
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

            // Standardize phone format if needed (e.g. +62)
            if (telepon.startsWith("0")) telepon = "+62" + telepon.substring(1);
            else if (!telepon.startsWith("+")) telepon = "+62" + telepon;

            promoteUser(telepon);
        });
    }

    private void promoteUser(String phone) {
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
                                .update("role", "KetuaRT")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Berhasil dipromosikan jadi Ketua RT", Toast.LENGTH_LONG).show();
                                    etTelepon.setText("");
                                    etIdWarga.setText("");
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error cari user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
