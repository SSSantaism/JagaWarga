package com.example.jagawarga;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TukarJadwalActivity extends AppCompatActivity {

    private ImageButton btnBack;
    // UI Changed: Instead of IDs, we ask for "New Day Request" or "Target User"?
    // Plan: "Create a doc in request_tukar: from_uid, from_name, from_day, id_rt, reason."
    // Let's repurpose the UI. "Jadwal Saya" -> just show current day (readonly).
    // "Jadwal Tujuan" -> Dropdown of Days? Or just "Alasan"?
    // If we want to request a NEW DAY, a spinner is best.

    // However, I need to keep the layout IDs or update XML. I'll reuse IDs for now but change logic.
    // etIdJadwalSaya -> (Hidden/Unused or Readonly)
    // etIdJadwalTujuan -> Use this for "Alasan" or "Hari Baru" text?
    // Let's assume the user wants to move to a specific day.

    private EditText etIdJadwalSaya; // Reused as "Alasan"
    private Spinner spinnerHari;     // Need to add this to layout or reuse existing EditText?
    // Let's reuse etIdJadwalTujuan as "Hari yang diinginkan (e.g. Senin)"
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

        // Cosmetic: Change hints
        etIdJadwalSaya.setHint("Alasan Tukar");
        etIdJadwalTujuan.setHint("Hari yang diinginkan (Senin - Minggu)");
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackAbsen);
        etIdJadwalSaya = findViewById(R.id.inputIdJadwalSaya);
        etIdJadwalTujuan = findViewById(R.id.inputIdJadwalTujuan);
        btnTukarJadwal = findViewById(R.id.btnTukarAbsen);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnTukarJadwal.setOnClickListener(v -> handleRequestTukar());
    }

    private void handleRequestTukar() {
        String alasan = etIdJadwalSaya.getText().toString().trim();
        String targetHari = etIdJadwalTujuan.getText().toString().trim();

        if (targetHari.isEmpty()) {
            Toast.makeText(this, "Hari tujuan wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Day
        if (!isValidDay(targetHari)) {
            Toast.makeText(this, "Hari tidak valid. Gunakan Senin, Selasa, dst.", Toast.LENGTH_SHORT).show();
            return;
        }

        String idWarga = PrefUtils.getIdWarga(this);
        String idRt = PrefUtils.getIdRt(this);
        String nama = getSharedPreferences("user_data", MODE_PRIVATE).getString("nama", "Warga");

        Map<String, Object> req = new HashMap<>();
        req.put("id_warga", idWarga);
        req.put("nama", nama);
        req.put("id_rt", idRt);
        req.put("hari_tujuan", targetHari); // e.g., "Senin"
        req.put("alasan", alasan);
        req.put("status", "pending");
        req.put("timestamp", Timestamp.now());

        db.collection("request_tukar").add(req)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Permintaan tukar jadwal dikirim!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean isValidDay(String day) {
        String[] days = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        for (String d : days) {
            if (d.equalsIgnoreCase(day)) return true;
        }
        return false;
    }
}
