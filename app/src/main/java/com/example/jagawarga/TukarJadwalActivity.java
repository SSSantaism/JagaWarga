package com.example.jagawarga;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TukarJadwalActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etIdJadwalSaya;
    private EditText etIdJadwalTujuan;
    private Button btnTukarJadwal;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tukar_jadwal);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();

        // Set proper hints
        etIdJadwalSaya.setHint("Masukkan ID Jadwal Anda (contoh: JDW-01-ABC123)");
        etIdJadwalTujuan.setHint("Masukkan ID Jadwal Target Tukar");
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackAbsen);
        etIdJadwalSaya = findViewById(R.id.inputIdJadwalSaya);
        etIdJadwalTujuan = findViewById(R.id.inputIdJadwalTujuan);
        btnTukarJadwal = findViewById(R.id.btnTukarAbsen);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnTukarJadwal.setOnClickListener(v -> handleSwapJadwal());
    }

    private void handleSwapJadwal() {
        String myJadwalId = etIdJadwalSaya.getText().toString().trim().toUpperCase();
        String targetJadwalId = etIdJadwalTujuan.getText().toString().trim().toUpperCase();

        if (myJadwalId.isEmpty() || targetJadwalId.isEmpty()) {
            Toast.makeText(this, "Kedua ID Jadwal wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (myJadwalId.equals(targetJadwalId)) {
            Toast.makeText(this, "Tidak bisa tukar dengan diri sendiri!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Memproses tukar jadwal...", Toast.LENGTH_SHORT).show();
        btnTukarJadwal.setEnabled(false);

        // Step 1: Find user with MY jadwal_id
        db.collection("users")
                .whereEqualTo("jadwal_id", myJadwalId)
                .get()
                .addOnSuccessListener(myQuerySnapshot -> {
                    if (myQuerySnapshot.isEmpty()) {
                        Toast.makeText(this, "ID Jadwal Anda tidak ditemukan!", Toast.LENGTH_SHORT).show();
                        btnTukarJadwal.setEnabled(true);
                        return;
                    }

                    DocumentSnapshot myDoc = myQuerySnapshot.getDocuments().get(0);
                    String myUserId = myDoc.getId();
                    String myHari = myDoc.getString("jadwal_hari");

                    // Step 2: Find user with TARGET jadwal_id
                    db.collection("users")
                            .whereEqualTo("jadwal_id", targetJadwalId)
                            .get()
                            .addOnSuccessListener(targetQuerySnapshot -> {
                                if (targetQuerySnapshot.isEmpty()) {
                                    Toast.makeText(this, "ID Jadwal Target tidak ditemukan!", Toast.LENGTH_SHORT)
                                            .show();
                                    btnTukarJadwal.setEnabled(true);
                                    return;
                                }

                                DocumentSnapshot targetDoc = targetQuerySnapshot.getDocuments().get(0);
                                String targetUserId = targetDoc.getId();
                                String targetHari = targetDoc.getString("jadwal_hari");

                                // Step 3: Swap jadwal_hari between the two users
                                performSwap(myUserId, myHari, targetUserId, targetHari);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error mencari jadwal target: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                btnTukarJadwal.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error mencari jadwal Anda: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnTukarJadwal.setEnabled(true);
                });
    }

    private void performSwap(String myUserId, String myHari, String targetUserId, String targetHari) {
        // Update my jadwal_hari to target's day
        db.collection("users").document(myUserId)
                .update("jadwal_hari", targetHari)
                .addOnSuccessListener(v1 -> {
                    // Update target's jadwal_hari to my day
                    db.collection("users").document(targetUserId)
                            .update("jadwal_hari", myHari)
                            .addOnSuccessListener(v2 -> {
                                Toast.makeText(this,
                                        "Tukar jadwal berhasil!\nAnda: " + myHari + " → " + targetHari +
                                                "\nTarget: " + targetHari + " → " + myHari,
                                        Toast.LENGTH_LONG).show();
                                btnTukarJadwal.setEnabled(true);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Rollback - restore my jadwal_hari
                                db.collection("users").document(myUserId)
                                        .update("jadwal_hari", myHari);
                                Toast.makeText(this, "Gagal update jadwal target. Tukar dibatalkan.",
                                        Toast.LENGTH_SHORT).show();
                                btnTukarJadwal.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal update jadwal Anda: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnTukarJadwal.setEnabled(true);
                });
    }
}
