package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.jagawarga.data.repository.AdminRepository;
import com.example.jagawarga.data.repository.SessionManager;

import java.util.List;

public class ListPermintaanActivity extends AppCompatActivity {

    private LinearLayout containerList;
    private ImageButton btnBack;
    private Button btnTabAbsensi, btnTabRegister;

    // MVVM
    private AdminRepository adminRepository;
    private String currentIdRt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_permintaan_register);

        adminRepository = new AdminRepository();
        SessionManager session = new SessionManager(this);
        currentIdRt = session.getIdRt();

        btnBack = findViewById(R.id.btnBack);
        containerList = findViewById(R.id.containerList);
        btnTabAbsensi = findViewById(R.id.btnTabAbsensi);
        btnTabRegister = findViewById(R.id.btnTabRegister);

        btnBack.setOnClickListener(v -> finish());

        // 1. Klik Tab Register
        btnTabRegister.setOnClickListener(v -> {
            updateTabUI(true);
            if (currentIdRt != null)
                loadPendingRegister();
        });

        // 2. Klik Tab Absensi
        btnTabAbsensi.setOnClickListener(v -> {
            updateTabUI(false);
            if (currentIdRt != null)
                loadPendingAbsen();
        });

        // Default Load
        updateTabUI(true);
        if (currentIdRt != null)
            loadPendingRegister();
    }

    private void updateTabUI(boolean isRegisterActive) {
        if (isRegisterActive) {
            btnTabRegister.setBackgroundResource(R.drawable.rounded_button_white);
            btnTabRegister.setTextColor(ContextCompat.getColor(this, R.color.black));
            btnTabAbsensi.setBackgroundResource(android.R.color.transparent);
            btnTabAbsensi.setTextColor(ContextCompat.getColor(this, R.color.gray));
        } else {
            btnTabAbsensi.setBackgroundResource(R.drawable.rounded_button_white);
            btnTabAbsensi.setTextColor(ContextCompat.getColor(this, R.color.black));
            btnTabRegister.setBackgroundResource(android.R.color.transparent);
            btnTabRegister.setTextColor(ContextCompat.getColor(this, R.color.gray));
        }
        containerList.removeAllViews();
    }

    // === REGISTER PENDING ===
    private void loadPendingRegister() {
        adminRepository.loadPendingRegister(currentIdRt, new AdminRepository.PendingListCallback() {
            @Override
            public void onSuccess(List<AdminRepository.PendingItem> items) {
                containerList.removeAllViews();
                for (AdminRepository.PendingItem item : items) {
                    addItemRegister(item);
                }
            }

            @Override
            public void onEmpty() {
                containerList.removeAllViews();
                Toast.makeText(ListPermintaanActivity.this,
                        getString(R.string.toast_no_pending_register), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(ListPermintaanActivity.this,
                        getString(R.string.toast_load_failed, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addItemRegister(AdminRepository.PendingItem item) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_request_register, containerList, false);

        TextView tvNama = itemView.findViewById(R.id.tvNamaWarga);
        TextView tvTelp = itemView.findViewById(R.id.tvTeleponWarga);
        Button btnAcc = itemView.findViewById(R.id.btnAcc);
        Button btnReject = itemView.findViewById(R.id.btnReject);

        tvNama.setText(item.nama);
        tvTelp.setText(item.telepon);

        btnAcc.setOnClickListener(v -> {
            btnAcc.setEnabled(false);
            btnReject.setEnabled(false);

            adminRepository.acceptRegister(item.docId, currentIdRt,
                    new AdminRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ListPermintaanActivity.this,
                                    getString(R.string.toast_warga_accepted, ""),
                                    Toast.LENGTH_SHORT).show();
                            containerList.removeView(itemView);
                        }

                        @Override
                        public void onError(String msg) {
                            btnAcc.setEnabled(true);
                            btnReject.setEnabled(true);
                            Toast.makeText(ListPermintaanActivity.this,
                                    getString(R.string.toast_error_generic, msg),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnReject.setOnClickListener(v -> {
            adminRepository.rejectRegister(item.docId, new AdminRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ListPermintaanActivity.this,
                            getString(R.string.toast_warga_rejected), Toast.LENGTH_SHORT).show();
                    containerList.removeView(itemView);
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(ListPermintaanActivity.this,
                            getString(R.string.toast_error_generic, msg), Toast.LENGTH_SHORT).show();
                }
            });
        });

        containerList.addView(itemView);
    }

    // === ABSEN PENDING ===
    private void loadPendingAbsen() {
        adminRepository.loadPendingAbsen(currentIdRt, new AdminRepository.AbsenPendingListCallback() {
            @Override
            public void onSuccess(List<AdminRepository.AbsenPendingItem> items) {
                containerList.removeAllViews();
                for (AdminRepository.AbsenPendingItem item : items) {
                    addItemAbsen(item);
                }
            }

            @Override
            public void onEmpty() {
                containerList.removeAllViews();
                Toast.makeText(ListPermintaanActivity.this,
                        getString(R.string.toast_no_pending_absen), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String msg) {
                Toast.makeText(ListPermintaanActivity.this,
                        getString(R.string.toast_load_failed, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addItemAbsen(AdminRepository.AbsenPendingItem item) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_request_absen, containerList, false);

        TextView tvNama = itemView.findViewById(R.id.tvNamaWargaAbsen);
        TextView tvWaktu = itemView.findViewById(R.id.tvWaktuAbsen);
        Button btnAcc = itemView.findViewById(R.id.btnAccAbsen);
        Button btnReject = itemView.findViewById(R.id.btnRejectAbsen);

        tvNama.setText(item.nama);
        tvWaktu.setText(getString(R.string.label_pukul_format, item.waktu));

        btnAcc.setOnClickListener(v -> {
            adminRepository.acceptAbsen(item.docId, new AdminRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ListPermintaanActivity.this,
                            getString(R.string.toast_absen_accepted), Toast.LENGTH_SHORT).show();
                    containerList.removeView(itemView);
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(ListPermintaanActivity.this,
                            getString(R.string.toast_error_generic, msg), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnReject.setOnClickListener(v -> {
            adminRepository.rejectAbsen(item.docId, new AdminRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ListPermintaanActivity.this,
                            getString(R.string.toast_absen_rejected), Toast.LENGTH_SHORT).show();
                    containerList.removeView(itemView);
                }

                @Override
                public void onError(String msg) {
                    Toast.makeText(ListPermintaanActivity.this,
                            getString(R.string.toast_error_generic, msg), Toast.LENGTH_SHORT).show();
                }
            });
        });

        containerList.addView(itemView);
    }
}
