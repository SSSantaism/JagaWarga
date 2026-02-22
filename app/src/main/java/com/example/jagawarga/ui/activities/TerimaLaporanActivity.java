package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.data.repository.AdminRepository;
import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.ui.adapter.LaporanAdapter;

import java.util.List;
import java.util.Map;

public class TerimaLaporanActivity extends AppCompatActivity {

    private RecyclerView rvLaporan;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    // MVVM
    private AdminRepository adminRepository;
    private String idRt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terima_laporan);

        adminRepository = new AdminRepository();
        SessionManager session = new SessionManager(this);
        idRt = session.getIdRt();

        rvLaporan = findViewById(R.id.rvLaporanMasuk);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        rvLaporan.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        if (idRt != null) {
            loadLaporan();
        }
    }

    private void loadLaporan() {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        adminRepository.loadLaporan(idRt, new AdminRepository.LaporanListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> dataList) {
                if (progressBar != null)
                    progressBar.setVisibility(View.GONE);
                rvLaporan.setAdapter(new LaporanAdapter(dataList));
            }

            @Override
            public void onEmpty() {
                if (progressBar != null)
                    progressBar.setVisibility(View.GONE);
                Toast.makeText(TerimaLaporanActivity.this,
                        getString(R.string.toast_no_reports), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String msg) {
                if (progressBar != null)
                    progressBar.setVisibility(View.GONE);
                Toast.makeText(TerimaLaporanActivity.this,
                        getString(R.string.toast_load_laporan_failed, msg),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
