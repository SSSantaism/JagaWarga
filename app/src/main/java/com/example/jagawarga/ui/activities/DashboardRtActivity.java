package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.data.repository.AuthRepository;
import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.ui.adapter.PengumumanAdapter;
import com.example.jagawarga.utils.Constants;
import com.example.jagawarga.viewmodel.DashboardViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class DashboardRtActivity extends AppCompatActivity {

    // Menu utama
    private LinearLayout menuTerimaLaporan;
    private LinearLayout menuListPermintaan;
    private LinearLayout menuBuatPengumuman;
    private FrameLayout profileContainer;
    private RecyclerView rvPengumuman;

    // MVVM
    private DashboardViewModel viewModel;
    private SessionManager sessionManager;
    private PengumumanAdapter pengumumanAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_rt);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        sessionManager = new SessionManager(this);

        // ====== 1. Greeting nama RT ======
        String namaRt = getIntent().getStringExtra(Constants.EXTRA_NAMA_USER);
        if (namaRt == null || namaRt.trim().isEmpty()) {
            namaRt = getString(R.string.fallback_name_pak_rt);
        }

        TextView tvGreetingRt = findViewById(R.id.tvGreetingRt);
        tvGreetingRt.setText(getString(R.string.greeting_format, namaRt));

        // ====== 2. Set tanggal hari ini ======
        TextView tvTanggalRt = findViewById(R.id.tvTanggalRt);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        String tanggal = sdf.format(calendar.getTime());
        tvTanggalRt.setText(tanggal);

        // ====== 3. Inisialisasi menu ======
        menuTerimaLaporan = findViewById(R.id.menuTerimaLaporan);
        menuListPermintaan = findViewById(R.id.menuListPermintaan);
        menuBuatPengumuman = findViewById(R.id.menuBuatPengumuman);

        // ====== 4. Setup RecyclerView Pengumuman ======
        rvPengumuman = findViewById(R.id.rvPengumuman);
        rvPengumuman.setLayoutManager(new LinearLayoutManager(this));
        pengumumanAdapter = new PengumumanAdapter(new ArrayList<>());
        rvPengumuman.setAdapter(pengumumanAdapter);

        // ====== 5. Observe ViewModel ======
        observeViewModel();
        viewModel.loadPengumuman();

        // ====== 6. Setup Listener (Navigasi) ======
        menuTerimaLaporan.setOnClickListener(v -> startActivity(new Intent(this, TerimaLaporanActivity.class)));

        menuBuatPengumuman.setOnClickListener(v -> startActivity(new Intent(this, BuatPengumumanActivity.class)));

        menuListPermintaan.setOnClickListener(v -> startActivity(new Intent(this, ListPermintaanActivity.class)));

        // ====== 7. Setup Logout ======
        profileContainer = findViewById(R.id.profileContainer);
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dialog_logout_title))
                        .setMessage(getString(R.string.dialog_logout_message))
                        .setPositiveButton(getString(R.string.dialog_logout_positive), (dialog, which) -> {
                            sessionManager.clearSession();
                            new AuthRepository().logout();

                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(getString(R.string.dialog_logout_negative), null)
                        .show();
            });
        }
    }

    private void observeViewModel() {
        viewModel.getPengumumanList().observe(this, dataList -> {
            if (dataList != null) {
                pengumumanAdapter.updateData(dataList);
            }
        });
    }
}
