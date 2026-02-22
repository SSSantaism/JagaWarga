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
import com.example.jagawarga.viewmodel.AbsenViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AbsenRondaActivity extends AppCompatActivity {

    private ImageButton btnBackAbsen;
    private Button btnKirimAbsen;
    private EditText insert_absenID;
    private TextView textTanggalAbsen;

    private ProgressDialog loading;

    // MVVM
    private AbsenViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absen_ronda);

        viewModel = new ViewModelProvider(this).get(AbsenViewModel.class);
        sessionManager = new SessionManager(this);

        initViews();
        setupDate();
        observeViewModel();

        btnBackAbsen.setOnClickListener(v -> finish());

        btnKirimAbsen.setOnClickListener(v -> {
            String idWarga = sessionManager.getIdWarga();
            String idRt = sessionManager.getIdRt();
            String nama = sessionManager.getNama();
            if (nama == null)
                nama = getString(R.string.fallback_name_warga);

            if (idWarga != null && idRt != null) {
                String catatan = insert_absenID.getText().toString().trim();
                viewModel.submitAbsen(idWarga, idRt, nama, catatan);
            }
        });
    }

    private void initViews() {
        btnBackAbsen = findViewById(R.id.btnBackAbsen);
        btnKirimAbsen = findViewById(R.id.btnUploadLaporan);
        insert_absenID = findViewById(R.id.insert_absenID);
        textTanggalAbsen = findViewById(R.id.Tanggal_absen);

        // Auto-fill ID Jadwal dari session
        String jadwalId = sessionManager.getJadwalId();
        if (jadwalId != null) {
            insert_absenID.setText(jadwalId);
            insert_absenID.setEnabled(false);
            insert_absenID.setFocusable(false);
        } else {
            insert_absenID.setHint(getString(R.string.hint_jadwal_not_found));
            insert_absenID.setEnabled(false);
        }

        loading = new ProgressDialog(this);
        loading.setCancelable(false);
    }

    private void setupDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        textTanggalAbsen.setText(sdf.format(calendar.getTime()));
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                loading.setMessage(getString(R.string.toast_absen_verifying));
                loading.show();
                btnKirimAbsen.setEnabled(false);
            } else {
                if (loading.isShowing())
                    loading.dismiss();
                btnKirimAbsen.setEnabled(true);
            }
        });

        viewModel.getAbsenResult().observe(this, result -> {
            if (result == null)
                return;

            switch (result.getStatus()) {
                case SUCCESS:
                    Toast.makeText(this, getString(R.string.toast_absen_success),
                            Toast.LENGTH_LONG).show();
                    finish();
                    break;

                case WRONG_DAY:
                    Toast.makeText(this,
                            getString(R.string.toast_absen_wrong_day,
                                    result.getHariIni(), result.getJadwalHari()),
                            Toast.LENGTH_LONG).show();
                    break;

                case ERROR:
                    Toast.makeText(this,
                            getString(R.string.toast_absen_failed, result.getErrorMessage()),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}
