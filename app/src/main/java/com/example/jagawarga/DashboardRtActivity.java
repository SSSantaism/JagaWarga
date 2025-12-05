package com.example.jagawarga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardRtActivity extends AppCompatActivity {

    // Menu utama
    private LinearLayout menuTerimaLaporan;
    private LinearLayout menuListPermintaan;

    private LinearLayout menuBuatPengumuman;

    // Tombol generate (sesuai layout XML, ini adalah TextView yang dibungkus CardView/Layout)
    private TextView btnGenerateJadwal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_rt);

        // ====== 1. Greeting nama RT ======
        String namaRt = getIntent().getStringExtra("nama_user");
        if (namaRt == null || namaRt.trim().isEmpty()) {
            namaRt = "Pak RT";
        }

        TextView tvGreetingRt = findViewById(R.id.tvGreetingRt);
        tvGreetingRt.setText("Hai, " + namaRt + " !");

        // ====== 2. Set tanggal hari ini ======
        TextView tvTanggalRt = findViewById(R.id.tvTanggalRt);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        String tanggal = sdf.format(calendar.getTime());
        tvTanggalRt.setText(tanggal);

        // ====== 3. Inisialisasi menu ======
        menuTerimaLaporan   = findViewById(R.id.menuTerimaLaporan);
        menuListPermintaan  = findViewById(R.id.menuListPermintaan);
        menuBuatPengumuman  = findViewById(R.id.menuBuatPengumuman);
        btnGenerateJadwal   = findViewById(R.id.btnGenerateJadwal);

        // ====== 4. Setup Listener (Navigasi) ======

        menuTerimaLaporan.setOnClickListener(v -> {
            startActivity(new Intent(DashboardRtActivity.this, TerimaLaporanActivity.class));
        });

        menuBuatPengumuman.setOnClickListener(v -> {
            // Arahkan ke halaman Buat Pengumuman
            Intent intent = new Intent(DashboardRtActivity.this, BuatPengumumanActivity.class);
            startActivity(intent);
        });

        menuListPermintaan.setOnClickListener(v -> {
            // Mengarahkan ke ListPermintaan (activity_list_permintaan_register.xml)
            startActivity(new Intent(this, ListPermintaanActivity.class));
        });

        // ====== 5. LOGIC GENERATE JADWAL (REMOVED) ======
        if (btnGenerateJadwal != null) {
            btnGenerateJadwal.setOnClickListener(v -> {
                 Toast.makeText(this, "Fitur generate jadwal sudah otomatis.", Toast.LENGTH_SHORT).show();
            });
            // Optional: Hide the button
            // ((View)btnGenerateJadwal.getParent()).setVisibility(View.GONE);
        }
    }
}
