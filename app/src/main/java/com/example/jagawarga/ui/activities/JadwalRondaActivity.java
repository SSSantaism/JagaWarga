package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.jagawarga.data.repository.DataRepository;
import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.viewmodel.JadwalViewModel;

import java.util.List;

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

    // MVVM
    private JadwalViewModel viewModel;
    private String currentRt;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jadwal_ronda);

        viewModel = new ViewModelProvider(this).get(JadwalViewModel.class);
        SessionManager session = new SessionManager(this);
        currentRt = session.getIdRt();
        currentUserId = session.getIdWarga();

        initUI();
        setupListeners();
        observeViewModel();

        viewModel.loadJadwal(currentRt);
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

        itemJadwal[0] = findViewById(R.id.itemJadwal1);
        itemJadwal[1] = findViewById(R.id.itemJadwal2);
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
        viewModel.getDisplayDate().observe(this, date -> {
            textTanggalPilihan.setText(date);
        });

        // Jadwal list
        viewModel.getJadwalList().observe(this, items -> {
            // Reset UI - hide all items
            for (int i = 0; i < 10; i++) {
                if (itemJadwal[i] != null) {
                    itemJadwal[i].setVisibility(View.GONE);
                }
                if (tvNama[i] != null) {
                    tvNama[i].setText(getString(R.string.text_dash));
                }
                if (tvIdJadwal[i] != null) {
                    tvIdJadwal[i].setText(getString(R.string.label_id_jadwal_dash));
                }
                if (tvJam[i] != null) {
                    tvJam[i].setText(getString(R.string.text_dash));
                }
            }

            // Populate data
            if (items != null) {
                int index = 0;
                for (DataRepository.JadwalItem item : items) {
                    if (index >= 10)
                        break;

                    if (itemJadwal[index] != null) {
                        itemJadwal[index].setVisibility(View.VISIBLE);

                        // Highlight current user
                        if (currentUserId != null && item.docId.equals(currentUserId)) {
                            itemJadwal[index].setBackgroundResource(R.drawable.bg_schedule_item_selected);
                        } else {
                            itemJadwal[index].setBackgroundResource(R.drawable.bg_schedule_item_normal);
                        }
                    }
                    if (tvNama[index] != null) {
                        tvNama[index].setText(item.nama != null ? item.nama
                                : getString(R.string.text_dash));
                    }
                    if (tvIdJadwal[index] != null) {
                        tvIdJadwal[index].setText(getString(R.string.label_id_jadwal_format,
                                item.jadwalId != null ? item.jadwalId
                                        : getString(R.string.text_dash)));
                    }
                    if (tvJam[index] != null) {
                        tvJam[index].setText(getString(R.string.text_jadwal_time));
                    }
                    index++;
                }
            }
        });

        // Error
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, getString(R.string.toast_load_jadwal_failed, msg),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
