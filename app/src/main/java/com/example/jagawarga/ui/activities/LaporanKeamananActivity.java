package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.PrefUtils;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Scroller;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.databinding.ActivityLaporanKeamananBinding;
import com.example.jagawarga.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LaporanKeamananActivity extends AppCompatActivity {

    private Spinner spinnerJenisLaporan;
    private ActivityLaporanKeamananBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLaporanKeamananBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // inisialisasi view
        spinnerJenisLaporan = findViewById(R.id.spinnerJenisLaporan);
        ImageButton btnBack = findViewById(R.id.btnBackLaporan);
        EditText inputDetailLaporan = findViewById(R.id.inputDetailLaporan);

        setupJenisLaporanSpinner();
        setupBackButton(btnBack);

        inputDetailLaporan.setScroller(new Scroller(this));
        inputDetailLaporan.setVerticalScrollBarEnabled(true);
        inputDetailLaporan.setMovementMethod(new ScrollingMovementMethod());

        binding.btnUploadLaporanKeamanan.setOnClickListener(v -> {
            String isi = binding.inputDetailLaporan.getText().toString();

            if (isi.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_all_data), Toast.LENGTH_SHORT).show();
            } else {
                kirimLaporan();
            }
        });

        // Set tanggal + waktu sekarang
        TextView inputTanggalLaporan = findViewById(R.id.inputTanggalLaporan);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM, HH:mm 'WIB'", new Locale("id", "ID"));

        String tanggalWaktu = sdf.format(calendar.getTime());
        inputTanggalLaporan.setText(tanggalWaktu);
        inputTanggalLaporan.setFocusable(false);
        inputTanggalLaporan.setClickable(false);
    }

    // --- Logic dropdown Jenis Laporan ---
    private void setupJenisLaporanSpinner() {
        String[] jenisLaporan = new String[] {
                "Keributan",
                "Perusakan",
                "Pencurian",
                "Penculikan",
                "Lainnya"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                jenisLaporan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerJenisLaporan.setAdapter(adapter);
    }

    // --- Logic tombol back ---
    private void setupBackButton(ImageButton btnBack) {
        if (btnBack == null)
            return;

        btnBack.setOnClickListener(v -> finish());
    }

    private void kirimLaporan() {

        String isi = binding.inputDetailLaporan.getText().toString();
        String jenis = binding.spinnerJenisLaporan.getSelectedItem().toString();

        SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
        String idWarga = prefs.getString(Constants.PREF_KEY_ID, null);
        String idRt = prefs.getString(Constants.PREF_KEY_ID_RT, null);
        String nama = prefs.getString(Constants.PREF_KEY_NAMA, getString(R.string.fallback_name_warga));

        if (idWarga == null || idRt == null) {
            Toast.makeText(this, getString(R.string.toast_id_not_found_login), Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> laporan = new HashMap<>();
        laporan.put(Constants.FIELD_ID_WARGA, idWarga);
        laporan.put(Constants.FIELD_NAMA_PELAPOR, nama);
        laporan.put(Constants.FIELD_ID_RT, idRt);
        laporan.put(Constants.FIELD_ISI_LAPORAN, isi);
        laporan.put(Constants.FIELD_JENIS_LAPORAN, jenis);
        laporan.put(Constants.FIELD_TANGGAL, Timestamp.now());

        db.collection(Constants.COLLECTION_LAPORAN).add(laporan)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, getString(R.string.toast_laporan_sent), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_LAPORAN", "Error: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.toast_laporan_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }
}
