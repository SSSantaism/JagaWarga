package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.viewmodel.TukarJadwalViewModel;

/**
 * Activity untuk mengajukan permintaan tukar jadwal ronda dengan warga lain.
 * Semua logika Firestore dipindahkan ke TukarJadwalRepository via
 * TukarJadwalViewModel.
 */
public class TukarJadwalActivity extends AppCompatActivity {

    private EditText inputIdJadwalSaya, inputIdJadwalTujuan;
    private TextView tvPrefixJadwalSaya, tvPrefixJadwalTujuan;
    private Button btnTukar;
    private ImageButton btnBack;

    private ProgressDialog progressDialog;

    // User data dari session
    private String myUserId, myNama, myJadwalId, myJadwalHari, myIdRt;
    private String jadwalPrefix; // e.g. "JDW-01-"

    // MVVM
    private TukarJadwalViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tukar_jadwal);

        // Inisialisasi MVVM
        viewModel = new ViewModelProvider(this).get(TukarJadwalViewModel.class);

        // Ambil data user dari SessionManager
        SessionManager session = new SessionManager(this);
        myUserId = session.getIdWarga();
        myNama = session.getNama();
        myJadwalId = session.getJadwalId();
        myJadwalHari = session.getJadwalHari();
        myIdRt = session.getIdRt();

        // Hitung prefix: JDW-{RT}-
        jadwalPrefix = "JDW-" + (myIdRt != null ? myIdRt : "00") + "-";

        initViews();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        inputIdJadwalSaya = findViewById(R.id.inputIdJadwalSaya);
        inputIdJadwalTujuan = findViewById(R.id.inputIdJadwalTujuan);
        tvPrefixJadwalSaya = findViewById(R.id.tvPrefixJadwalSaya);
        tvPrefixJadwalTujuan = findViewById(R.id.tvPrefixJadwalTujuan);
        btnTukar = findViewById(R.id.btnTukarAbsen);
        btnBack = findViewById(R.id.btnBackAbsen);

        // Set prefix sesuai RT user
        tvPrefixJadwalSaya.setText(jadwalPrefix);
        tvPrefixJadwalTujuan.setText(jadwalPrefix);

        // Pre-fill ID Jadwal Saya (hanya suffix, tanpa prefix)
        if (myJadwalId != null) {
            String suffix = myJadwalId;
            if (myJadwalId.startsWith(jadwalPrefix)) {
                suffix = myJadwalId.substring(jadwalPrefix.length());
            }
            inputIdJadwalSaya.setText(suffix);
            inputIdJadwalSaya.setEnabled(false);
        }

        progressDialog = new ProgressDialog(this);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnTukar.setOnClickListener(v -> {
            String inputSuffix = inputIdJadwalTujuan.getText().toString().trim().toUpperCase();

            if (inputSuffix.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_enter_target_jadwal),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Gabungkan prefix + suffix → full jadwal ID
            String idJadwalTujuan = jadwalPrefix + inputSuffix;

            if (idJadwalTujuan.equals(myJadwalId)) {
                Toast.makeText(this, getString(R.string.toast_cannot_swap_self),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.sendSwapRequest(myUserId, myNama, myJadwalId,
                    myJadwalHari, myIdRt, idJadwalTujuan);
        });
    }

    // ========================================================================
    // ViewModel Observers
    // ========================================================================

    private void observeViewModel() {
        // Loading state
        viewModel.getIsLoading().observe(this, loading -> {
            if (loading) {
                progressDialog.setMessage(getString(R.string.progress_searching_jadwal));
                progressDialog.show();
                btnTukar.setEnabled(false);
            } else {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
                btnTukar.setEnabled(true);
            }
        });

        // Swap result
        viewModel.getSwapResult().observe(this, result -> {
            if (result == null)
                return;

            switch (result.getStatus()) {
                case SUCCESS:
                    Toast.makeText(this,
                            getString(R.string.toast_swap_request_sent, result.getTargetNama()),
                            Toast.LENGTH_LONG).show();
                    finish();
                    break;

                case TARGET_NOT_FOUND:
                    Toast.makeText(this, getString(R.string.toast_jadwal_not_found_rt),
                            Toast.LENGTH_SHORT).show();
                    break;

                case ALREADY_EXISTS:
                    Toast.makeText(this, getString(R.string.toast_swap_request_exists),
                            Toast.LENGTH_SHORT).show();
                    break;

                case ERROR:
                    Toast.makeText(this,
                            getString(R.string.toast_swap_request_failed, result.getErrorMessage()),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}
