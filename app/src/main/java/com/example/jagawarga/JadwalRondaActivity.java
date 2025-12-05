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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class JadwalRondaActivity extends AppCompatActivity {

    // === UI Jadwal ===
    TextView[] tvNama = new TextView[10];
    TextView[] tvIdJadwal = new TextView[10];
    TextView[] tvJam = new TextView[10];

    // === UI Navigasi Tanggal ===
    ImageButton btnBackJadwal, btnPrevDate, btnNextDate;
    Button btnKembaliJadwal;
    TextView textTanggalPilihan;

    // === Date Management ===
    Calendar calendar;
    SimpleDateFormat dateFormatDay;      // Format: "Senin", "Selasa"
    SimpleDateFormat dateFormatDisplay;  // Format: "Senin, 25 November"

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
        btnPrevDate   = findViewById(R.id.btnPrevDate);
        btnNextDate   = findViewById(R.id.btnNextDate);
        btnKembaliJadwal = findViewById(R.id.btnKembaliJadwal);
        textTanggalPilihan = findViewById(R.id.textTanggalPilihan);

        tvNama[0] = findViewById(R.id.textNama1);
        tvNama[1] = findViewById(R.id.textNama2);
        tvNama[2] = findViewById(R.id.textNama3);
        tvNama[3] = findViewById(R.id.textNama4);
        tvNama[4] = findViewById(R.id.textNama5);
        tvNama[5] = findViewById(R.id.textNama6);
        tvNama[6] = findViewById(R.id.textNama7);
        tvNama[7] = findViewById(R.id.textNama8);
        tvNama[8] = findViewById(R.id.textNama9);
        tvNama[9] = findViewById(R.id.textNama10);

        tvIdJadwal[0] = findViewById(R.id.textIdJadwal1);
        tvIdJadwal[1] = findViewById(R.id.textIdJadwal2);
        tvIdJadwal[2] = findViewById(R.id.textIdJadwal3);
        tvIdJadwal[3] = findViewById(R.id.textIdJadwal4);
        tvIdJadwal[4] = findViewById(R.id.textIdJadwal5);
        tvIdJadwal[5] = findViewById(R.id.textIdJadwal6);
        tvIdJadwal[6] = findViewById(R.id.textIdJadwal7);
        tvIdJadwal[7] = findViewById(R.id.textIdJadwal8);
        tvIdJadwal[8] = findViewById(R.id.textIdJadwal9);
        tvIdJadwal[9] = findViewById(R.id.textIdJadwal10);

        tvJam[0] = findViewById(R.id.textJam1);
        tvJam[1] = findViewById(R.id.textJam2);
        tvJam[2] = findViewById(R.id.textJam3);
        tvJam[3] = findViewById(R.id.textJam4);
        tvJam[4] = findViewById(R.id.textJam5);
        tvJam[5] = findViewById(R.id.textJam6);
        tvJam[6] = findViewById(R.id.textJam7);
        tvJam[7] = findViewById(R.id.textJam8);
        tvJam[8] = findViewById(R.id.textJam9);
        tvJam[9] = findViewById(R.id.textJam10);
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
        formatted = formatted.substring(0,1).toUpperCase() + formatted.substring(1);
        textTanggalPilihan.setText(formatted);
    }

    private void loadJadwalFromFirestore() {
        if (currentRt == null) return;

        // Ambil nama hari (Senin, Selasa, dll)
        String hariIni = dateFormatDay.format(calendar.getTime());

        // Reset UI
        for (int i = 0; i < 10; i++) {
            tvNama[i].setText("-");
            tvIdJadwal[i].setText("ID Jadwal: -");
            tvJam[i].setText("-");
        }

        db.collection("users")
                .whereEqualTo("id_rt", currentRt)
                .whereEqualTo("jadwal_hari", hariIni)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int index = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (index >= 10) break;

                        // Check if verified (optional based on your flow)
                        String status = doc.getString("status_warga");
                        if ("verified".equals(status) || status == null) { // Handle legacy users without status
                             tvNama[index].setText(doc.getString("nama"));
                             // ID Jadwal is technically just their UID/Name in this simplified flow
                             // Or we can display the Day
                             tvIdJadwal[index].setText("ID: " + hariIni);
                             // Shift/Waktu can be default 20:00 - 02:00
                             tvJam[index].setText("20:00 - 02:00");
                             index++;
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal muat jadwal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
