package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.data.repository.AdminRepository;
import com.example.jagawarga.data.repository.SessionManager;

public class BuatPengumumanActivity extends AppCompatActivity {

    private EditText inputJudul, inputIsi;
    private Button btnSubmit;
    private ImageButton btnBack;

    // MVVM
    private AdminRepository adminRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buat_pengumuman);

        adminRepository = new AdminRepository();
        sessionManager = new SessionManager(this);

        inputJudul = findViewById(R.id.inputJudul);
        inputIsi = findViewById(R.id.inputIsi);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String judul = inputJudul.getText().toString().trim();
            String isi = inputIsi.getText().toString().trim();

            if (judul.isEmpty() || isi.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_judul_isi_required), Toast.LENGTH_SHORT).show();
                return;
            }

            String idRt = sessionManager.getIdRt();
            if (idRt == null) {
                Toast.makeText(this, getString(R.string.toast_session_expired), Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage(getString(R.string.progress_publishing));
            pd.setCancelable(false);
            pd.show();

            adminRepository.submitPengumuman(judul, isi, idRt, new AdminRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    pd.dismiss();
                    Toast.makeText(BuatPengumumanActivity.this,
                            getString(R.string.toast_publish_success), Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String msg) {
                    pd.dismiss();
                    Toast.makeText(BuatPengumumanActivity.this,
                            getString(R.string.toast_publish_failed, msg), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
