package com.example.jagawarga;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ListPermintaanActivity extends AppCompatActivity {

    private LinearLayout containerList;
    private ImageButton btnBack;
    private Button btnTabAbsensi, btnTabRegister;

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

    // === REGISTER PENDING ===
    private void loadPendingRegister(String idRt) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .whereEqualTo(Constants.FIELD_STATUS_WARGA, Constants.STATUS_PENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    containerList.removeAllViews();
                    if (snap.isEmpty()) {
                        Toast.makeText(this, getString(R.string.toast_no_pending_register),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snap) {
                        String idWarga = doc.getId();
                        String nama = doc.getString(Constants.FIELD_NAMA);
                        String telp = doc.getString(Constants.FIELD_TELEPON);
                        addItemRegister(idWarga, nama, telp, idRt);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_load_failed, e.getMessage()),
                        Toast.LENGTH_SHORT).show());
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
            btnAcc.setEnabled(false);
            btnReject.setEnabled(false);
            getBalancedDayForRt(idRt, balancedDay -> {
                String jadwalId = generateJadwalId(idRt);
                Map<String, Object> updates = new HashMap<>();
                updates.put(Constants.FIELD_STATUS_WARGA, Constants.STATUS_VERIFIED);
                updates.put(Constants.FIELD_JADWAL_HARI, balancedDay);
                updates.put(Constants.FIELD_JADWAL_ID, jadwalId);

                db.collection(Constants.COLLECTION_USERS).document(idWarga)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this,
                                    getString(R.string.toast_warga_accepted, balancedDay),
                                    Toast.LENGTH_SHORT).show();
                            containerList.removeView(itemView);
                        })
                        .addOnFailureListener(e -> {
                            btnAcc.setEnabled(true);
                            btnReject.setEnabled(true);
                            Toast.makeText(this, getString(R.string.toast_error_generic, e.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        });
            });
        });

        btnReject.setOnClickListener(v -> {
            db.collection(Constants.COLLECTION_USERS).document(idWarga)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.toast_warga_rejected),
                                Toast.LENGTH_SHORT).show();
                        containerList.removeView(itemView);
                    });
        });

        containerList.addView(itemView);
    }

    private String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }

    private interface OnBalancedDayCallback {
        void onResult(String day);
    }

    private void getBalancedDayForRt(String rt, OnBalancedDayCallback callback) {
        String[] days = { "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
        int[] counts = new int[7];
        AtomicInteger completed = new AtomicInteger(0);
        for (int i = 0; i < days.length; i++) {
            final int idx = i;
            db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo(Constants.FIELD_ID_RT, rt)
                    .whereEqualTo(Constants.FIELD_JADWAL_HARI, days[idx])
                    .get()
                    .addOnSuccessListener(s -> {
                        counts[idx] = s.size();
                        if (completed.incrementAndGet() == 7) {
                            int min = 0;
                            for (int j = 1; j < 7; j++)
                                if (counts[j] < counts[min])
                                    min = j;
                            callback.onResult(days[min]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completed.incrementAndGet() == 7)
                            callback.onResult(days[(int) (Math.random() * days.length)]);
                    });
        }
    }

    // === ABSEN PENDING ===
    private void loadPendingAbsen(String idRt) {
        db.collection(Constants.COLLECTION_ABSENSI)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .whereEqualTo(Constants.FIELD_STATUS, Constants.STATUS_PENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    containerList.removeAllViews();
                    if (snap.isEmpty()) {
                        Toast.makeText(this, getString(R.string.toast_no_pending_absen),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snap) {
                        String idAbsen = doc.getId();
                        String nama = doc.getString(Constants.FIELD_NAMA);
                        Object waktuObj = doc.get(Constants.FIELD_WAKTU);
                        String waktuStr = waktuObj != null ? waktuObj.toString() : "-";
                        addItemAbsen(idAbsen, nama, waktuStr);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_load_failed, e.getMessage()),
                        Toast.LENGTH_SHORT).show());
    }

    private void addItemAbsen(String idAbsen, String nama, String waktu) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_request_absen, containerList, false);

        TextView tvNama = itemView.findViewById(R.id.tvNamaWargaAbsen);
        TextView tvWaktu = itemView.findViewById(R.id.tvWaktuAbsen);
        Button btnAcc = itemView.findViewById(R.id.btnAccAbsen);
        Button btnReject = itemView.findViewById(R.id.btnRejectAbsen);

        tvNama.setText(nama);
        tvWaktu.setText(getString(R.string.label_pukul_format, waktu));

        btnAcc.setOnClickListener(v -> {
            db.collection(Constants.COLLECTION_ABSENSI).document(idAbsen)
                    .update(Constants.FIELD_STATUS, Constants.STATUS_VERIFIED)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.toast_absen_accepted),
                                Toast.LENGTH_SHORT).show();
                        containerList.removeView(itemView);
                    });
        });

        btnReject.setOnClickListener(v -> {
            db.collection(Constants.COLLECTION_ABSENSI).document(idAbsen)
                    .update(Constants.FIELD_STATUS, Constants.STATUS_REJECTED)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, getString(R.string.toast_absen_rejected),
                                Toast.LENGTH_SHORT).show();
                        containerList.removeView(itemView);
                    });
        });

        containerList.addView(itemView);
    }
}
