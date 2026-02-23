package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.ui.adapter.JadwalAdapter;
import com.example.jagawarga.viewmodel.JadwalViewModel;

public class JadwalRondaActivity extends AppCompatActivity {

    // UI Navigasi Tanggal
    private ImageButton btnBackJadwal, btnPrevDate, btnNextDate;
    private Button btnKembaliJadwal;
    private TextView textTanggalPilihan;

    // RecyclerView Jadwal
    private RecyclerView rvJadwal;
    private JadwalAdapter jadwalAdapter;

    // MVVM
    private JadwalViewModel viewModel;
    private String currentRt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jadwal_ronda);

        viewModel = new ViewModelProvider(this).get(JadwalViewModel.class);
        SessionManager session = new SessionManager(this);
        currentRt = session.getIdRt();
        String currentUserId = session.getIdWarga();

        initUI(currentUserId);
        setupListeners();
        observeViewModel();

        viewModel.loadJadwal(currentRt);
    }

    private void initUI(String currentUserId) {
        btnBackJadwal = findViewById(R.id.btnBackJadwal);
        btnPrevDate = findViewById(R.id.btnPrevDate);
        btnNextDate = findViewById(R.id.btnNextDate);
        btnKembaliJadwal = findViewById(R.id.btnKembaliJadwal);
        textTanggalPilihan = findViewById(R.id.textTanggalPilihan);

        rvJadwal = findViewById(R.id.rvJadwal);
        rvJadwal.setLayoutManager(new LinearLayoutManager(this));
        jadwalAdapter = new JadwalAdapter(currentUserId);
        rvJadwal.setAdapter(jadwalAdapter);
    }

    private void setupListeners() {
        View.OnClickListener back = v -> finish();
        btnBackJadwal.setOnClickListener(back);
        btnKembaliJadwal.setOnClickListener(back);

        btnPrevDate.setOnClickListener(v -> {
            viewModel.prevDay();
            viewModel.loadJadwal(currentRt);
        });

        btnNextDate.setOnClickListener(v -> {
            viewModel.nextDay();
            viewModel.loadJadwal(currentRt);
        });
    }

    private void observeViewModel() {
        // Tanggal display
        viewModel.getDisplayDate().observe(this, date -> textTanggalPilihan.setText(date));

        // Jadwal list — langsung push ke adapter
        viewModel.getJadwalList().observe(this, items -> jadwalAdapter.updateData(items));

        // Error
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, getString(R.string.toast_load_jadwal_failed, msg),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
