package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Scroller;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.databinding.ActivityLaporanKeamananBinding;
import com.example.jagawarga.viewmodel.LaporanViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LaporanKeamananActivity extends AppCompatActivity {

    private Spinner spinnerJenisLaporan;
    private ActivityLaporanKeamananBinding binding;

    // MVVM
    private LaporanViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLaporanKeamananBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LaporanViewModel.class);
        sessionManager = new SessionManager(this);

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
            String jenis = binding.spinnerJenisLaporan.getSelectedItem().toString();

            String idWarga = sessionManager.getIdWarga();
            String idRt = sessionManager.getIdRt();
            String nama = sessionManager.getNama();
            if (nama == null)
                nama = getString(R.string.fallback_name_warga);

            viewModel.submitLaporan(idWarga, idRt, nama, isi, jenis);
        });

        // Set tanggal + waktu sekarang
        TextView inputTanggalLaporan = findViewById(R.id.inputTanggalLaporan);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM, HH:mm 'WIB'", new Locale("id", "ID"));
        String tanggalWaktu = sdf.format(calendar.getTime());
        inputTanggalLaporan.setText(tanggalWaktu);
        inputTanggalLaporan.setFocusable(false);
        inputTanggalLaporan.setClickable(false);

        observeViewModel();
    }

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

    private void setupBackButton(ImageButton btnBack) {
        if (btnBack == null)
            return;
        btnBack.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getLaporanResult().observe(this, result -> {
            if (result == null)
                return;

            switch (result.getStatus()) {
                case SUCCESS:
                    Toast.makeText(this, getString(R.string.toast_laporan_sent),
                            Toast.LENGTH_SHORT).show();
                    finish();
                    break;

                case VALIDATION_ERROR:
                    Toast.makeText(this, getString(R.string.toast_fill_all_data),
                            Toast.LENGTH_SHORT).show();
                    break;

                case SESSION_ERROR:
                    Toast.makeText(this, getString(R.string.toast_id_not_found_login),
                            Toast.LENGTH_LONG).show();
                    break;

                case ERROR:
                    Toast.makeText(this,
                            getString(R.string.toast_laporan_failed, result.getErrorMessage()),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}
