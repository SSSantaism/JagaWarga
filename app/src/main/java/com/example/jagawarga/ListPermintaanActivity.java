package com.example.jagawarga;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ListPermintaanActivity extends AppCompatActivity {

    private LinearLayout containerList;
    private ImageButton btnBack;
    private Button btnTabAbsensi, btnTabRegister;
    private Button btnTabTukar; // New Tab

    private String currentIdRt = "";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_permintaan_register);

        // Init Views
        btnBack = findViewById(R.id.btnBack);
        containerList = findViewById(R.id.containerList);
        btnTabAbsensi = findViewById(R.id.btnTabAbsensi);
        btnTabRegister = findViewById(R.id.btnTabRegister);
        // Assuming layout XML might not have this button yet, I will add it if I edit
        // XML,
        // but for now let's just use the existing 2 tabs or repurpose one if needed.
        // Wait, the plan mentioned "Swap Request" in list permintaan.
        // I will stick to 2 tabs for now and maybe add swap requests in "Absensi" tab
        // or a new logic.
        // Let's keep it simple: 2 tabs. Swap requests can appear in "Register" tab or
        // "Absensi"?
        // Actually, let's just implement Register & Absen first as per plan step.

        db = FirebaseFirestore.getInstance();
        currentIdRt = PrefUtils.getIdRt(this);

        btnBack.setOnClickListener(v -> finish());

        // 1. Klik Tab Register
        btnTabRegister.setOnClickListener(v -> {
            updateTabUI(true);
            if (currentIdRt != null)
                loadPendingRegister(currentIdRt);
        });

        // 2. Klik Tab Absensi
        btnTabAbsensi.setOnClickListener(v -> {
            updateTabUI(false);
            if (currentIdRt != null)
                loadPendingAbsen(currentIdRt);
        });

        // Default Load
        updateTabUI(true);
        if (currentIdRt != null)
            loadPendingRegister(currentIdRt);
    }

    private void updateTabUI(boolean isRegisterActive) {
        if (isRegisterActive) {
            btnTabRegister.setBackgroundResource(R.drawable.rounded_button_white);
            btnTabRegister.setTextColor(ContextCompat.getColor(this, R.color.black));
            btnTabAbsensi.setBackgroundResource(android.R.color.transparent);
            btnTabAbsensi.setTextColor(ContextCompat.getColor(this, R.color.gray));
        } else {
            btnTabAbsensi.setBackgroundResource(R.drawable.rounded_button_white);
            btnTabAbsensi.setTextColor(ContextCompat.getColor(this, R.color.black));
            btnTabRegister.setBackgroundResource(android.R.color.transparent);
            btnTabRegister.setTextColor(ContextCompat.getColor(this, R.color.gray));
        }
        containerList.removeAllViews();
    }

    // =================================================================
    // LOGIC PERMINTAAN REGISTER (Firestore)
    // =================================================================
    private void loadPendingRegister(String idRt) {
        db.collection("users")
                .whereEqualTo("id_rt", idRt)
                .whereEqualTo("status_warga", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    containerList.removeAllViews();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Tidak ada register baru", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String idWarga = doc.getId();
                        String nama = doc.getString("nama");
                        String telp = doc.getString("telepon");

                        addItemRegister(idWarga, nama, telp, idRt);
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Gagal memuat: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addItemRegister(String idWarga, String nama, String telp, String idRt) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_request_register, containerList, false);

        TextView tvNama = itemView.findViewById(R.id.tvNamaWarga);
        TextView tvTelp = itemView.findViewById(R.id.tvTeleponWarga);
        Button btnAcc = itemView.findViewById(R.id.btnAcc);
        Button btnReject = itemView.findViewById(R.id.btnReject);

        tvNama.setText(nama);
        tvTelp.setText(telp);

        btnAcc.setOnClickListener(v -> {
            // When approving, assign jadwal_hari and jadwal_id
            btnAcc.setEnabled(false);
            btnReject.setEnabled(false);

            getBalancedDayForRt(idRt, balancedDay -> {
                String jadwalId = generateJadwalId(idRt);

                Map<String, Object> updates = new HashMap<>();
                updates.put("status_warga", "verified");
                updates.put("jadwal_hari", balancedDay);
                updates.put("jadwal_id", jadwalId);

                db.collection("users").document(idWarga)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Warga diterima dan jadwal telah diassign (" + balancedDay + ")",
                                    Toast.LENGTH_SHORT).show();
                            containerList.removeView(itemView);
                        })
                        .addOnFailureListener(e -> {
                            btnAcc.setEnabled(true);
                            btnReject.setEnabled(true);
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        });

        btnReject.setOnClickListener(v -> {
            // Delete user or set status rejected
            db.collection("users").document(idWarga)
                    .delete() // Simple rejection: delete the doc (User must register again)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Warga ditolak", Toast.LENGTH_SHORT).show();
                        containerList.removeView(itemView);
                    });
        });

        containerList.addView(itemView);
    }

    /**
     * Generate unique jadwal ID in format: JDW-{RT}-{6 random alphanumeric chars}
     */
    private String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }

    /**
     * Callback interface for balanced day assignment
     */
    private interface OnBalancedDayCallback {
        void onResult(String day);
    }

    /**
     * Get day with least users for a specific RT (balanced assignment)
     */
    private void getBalancedDayForRt(String rt, OnBalancedDayCallback callback) {
        String[] days = { "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
        int[] counts = new int[7];
        java.util.concurrent.atomic.AtomicInteger completedQueries = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < days.length; i++) {
            final int index = i;
            db.collection("users")
                    .whereEqualTo("id_rt", rt)
                    .whereEqualTo("jadwal_hari", days[index])
                    .get()
                    .addOnSuccessListener(snap -> {
                        counts[index] = snap.size();
                        if (completedQueries.incrementAndGet() == 7) {
                            // Find day with minimum count
                            int minIndex = 0;
                            for (int j = 1; j < 7; j++) {
                                if (counts[j] < counts[minIndex])
                                    minIndex = j;
                            }
                            callback.onResult(days[minIndex]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to random if query fails
                        if (completedQueries.incrementAndGet() == 7) {
                            callback.onResult(days[(int) (Math.random() * days.length)]);
                        }
                    });
        }
    }

    // =================================================================
    // LOGIC PERMINTAAN ABSENSI (Firestore)
    // =================================================================
    private void loadPendingAbsen(String idRt) {
        db.collection("absensi")
                .whereEqualTo("id_rt", idRt)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    containerList.removeAllViews();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Tidak ada absen pending", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String idAbsen = doc.getId();
                        String nama = doc.getString("nama");
                        // Convert Timestamp to readable time if needed, or string
                        Object waktuObj = doc.get("waktu");
                        String waktuStr = waktuObj != null ? waktuObj.toString() : "-";

                        // If it's a Timestamp, formatting would be better, but for now toString()

                        addItemAbsen(idAbsen, nama, waktuStr);
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Gagal memuat: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addItemAbsen(String idAbsen, String nama, String waktu) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_request_absen, containerList, false);

        TextView tvNama = itemView.findViewById(R.id.tvNamaWargaAbsen);
        TextView tvWaktu = itemView.findViewById(R.id.tvWaktuAbsen);
        Button btnAcc = itemView.findViewById(R.id.btnAccAbsen);
        Button btnReject = itemView.findViewById(R.id.btnRejectAbsen);

        tvNama.setText(nama);
        tvWaktu.setText("Pukul: " + waktu);

        btnAcc.setOnClickListener(v -> {
            db.collection("absensi").document(idAbsen)
                    .update("status", "verified")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Absen diterima", Toast.LENGTH_SHORT).show();
                        containerList.removeView(itemView);
                    });
        });

        btnReject.setOnClickListener(v -> {
            db.collection("absensi").document(idAbsen)
                    .update("status", "rejected")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Absen ditolak", Toast.LENGTH_SHORT).show();
                        containerList.removeView(itemView);
                    });
        });

        containerList.addView(itemView);
    }
}
