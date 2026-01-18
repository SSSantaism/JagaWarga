package com.example.jagawarga;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class JadwalRondaActivity extends AppCompatActivity {

    // === UI Jadwal ===
    TextView[] tvNama = new TextView[10];
    TextView[] tvIdJadwal = new TextView[10];
    TextView[] tvJam = new TextView[10];
    LinearLayout[] itemJadwal = new LinearLayout[10];

    // === UI Navigasi Tanggal ===
    ImageButton btnBackJadwal, btnPrevDate, btnNextDate;
    Button btnKembaliJadwal;
    TextView textTanggalPilihan;

    // === Date Management ===
    Calendar calendar;
    SimpleDateFormat dateFormatDay; // Format: "Senin", "Selasa"
    SimpleDateFormat dateFormatDisplay; // Format: "Senin, 25 November"

    private FirebaseFirestore db;
    private String currentRt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jadwal_ronda);

        db = FirebaseFirestore.getInstance();
        currentRt = PrefUtils.getIdRt(this);

        initUI();
        initTanggal();
        setupListeners();

        loadJadwalFromFirestore();
    }

    private void initUI() {
        btnBackJadwal = findViewById(R.id.btnBackJadwal);
        btnPrevDate = findViewById(R.id.btnPrevDate);
        btnNextDate = findViewById(R.id.btnNextDate);
        btnKembaliJadwal = findViewById(R.id.btnKembaliJadwal);
        textTanggalPilihan = findViewById(R.id.textTanggalPilihan);

        tvNama[0] = findViewById(R.id.textNama1);
        tvNama[1] = findViewById(R.id.textNama2);

        tvIdJadwal[0] = findViewById(R.id.textIdJadwal1);
        tvIdJadwal[1] = findViewById(R.id.textIdJadwal2);

        tvJam[0] = findViewById(R.id.textJam1);
        tvJam[1] = findViewById(R.id.textJam2);

        // Item containers for visibility control
        itemJadwal[0] = findViewById(R.id.itemJadwal1);
        itemJadwal[1] = findViewById(R.id.itemJadwal2);
    }

    private void initTanggal() {
        calendar = Calendar.getInstance();
        dateFormatDay = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
        dateFormatDisplay = new SimpleDateFormat("EEEE, dd MMMM", new Locale("id", "ID"));
        updateTanggalUI();
    }

    private void setupListeners() {
        View.OnClickListener back = v -> finish();
        btnBackJadwal.setOnClickListener(back);
        btnKembaliJadwal.setOnClickListener(back);

        btnPrevDate.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateTanggalUI();
            loadJadwalFromFirestore();
        });

        btnNextDate.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateTanggalUI();
            loadJadwalFromFirestore();
        });
    }

    private void updateTanggalUI() {
        String formatted = dateFormatDisplay.format(calendar.getTime());
        formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        textTanggalPilihan.setText(formatted);
    }

    private void loadJadwalFromFirestore() {
        if (currentRt == null) {
            Log.d("JadwalRonda", "currentRt is null");
            return;
        }

        // Get current logged-in user ID for comparison
        String currentUserId = PrefUtils.getIdWarga(this);

        // Ambil nama hari (Senin, Selasa, dll)
        String hariIni = dateFormatDay.format(calendar.getTime());
        // Capitalize first letter
        hariIni = hariIni.substring(0, 1).toUpperCase() + hariIni.substring(1).toLowerCase();

        Log.d("JadwalRonda", "Loading jadwal for RT: " + currentRt + ", Hari: " + hariIni);

        // Reset UI - hide all items first
        for (int i = 0; i < 10; i++) {
            if (itemJadwal[i] != null) {
                itemJadwal[i].setVisibility(View.GONE);
            }
            if (tvNama[i] != null) {
                tvNama[i].setText("-");
            }
            if (tvIdJadwal[i] != null) {
                tvIdJadwal[i].setText("ID Jadwal: -");
            }
            if (tvJam[i] != null) {
                tvJam[i].setText("-");
            }
        }

        final String finalHari = hariIni;
        final String finalUserId = currentUserId;
        db.collection("users")
                .whereEqualTo("id_rt", currentRt)
                .whereEqualTo("jadwal_hari", hariIni)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("JadwalRonda", "Query returned " + queryDocumentSnapshots.size() + " documents");

                    int index = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (index >= 10)
                            break;

                        String docId = doc.getId();
                        String nama = doc.getString("nama");
                        String jadwalId = doc.getString("jadwal_id");
                        Log.d("JadwalRonda", "Found user: " + nama + ", jadwal_id: " + jadwalId);

                        // Show item and populate data
                        if (itemJadwal[index] != null) {
                            itemJadwal[index].setVisibility(View.VISIBLE);

                            // Set background based on whether this is the logged-in user's schedule
                            if (finalUserId != null && docId.equals(finalUserId)) {
                                // Green highlight for current user's schedule
                                itemJadwal[index].setBackgroundResource(R.drawable.bg_schedule_item_selected);
                            } else {
                                // Gray background for other users' schedules
                                itemJadwal[index].setBackgroundResource(R.drawable.bg_schedule_item_normal);
                            }
                        }
                        if (tvNama[index] != null) {
                            tvNama[index].setText(nama != null ? nama : "-");
                        }
                        if (tvIdJadwal[index] != null) {
                            tvIdJadwal[index].setText("ID: " + (jadwalId != null ? jadwalId : "-"));
                        }
                        if (tvJam[index] != null) {
                            tvJam[index].setText("20:00 - 02:00");
                        }
                        index++;
                    }

                    if (index == 0) {
                        Log.d("JadwalRonda", "No users found for this day");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("JadwalRonda", "Error loading jadwal: " + e.getMessage());
                    Toast.makeText(this, "Gagal muat jadwal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
