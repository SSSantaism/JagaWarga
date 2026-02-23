package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.data.model.PosRonda;
import com.example.jagawarga.data.model.User;
import com.example.jagawarga.data.repository.AuthRepository;
import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.ui.adapter.PengumumanAdapter;
import com.example.jagawarga.utils.Constants;
import com.example.jagawarga.viewmodel.DashboardViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    // UI Components
    private LinearLayout menuAbsen, menuTukar, menuLapor, menuJadwal;
    private TextView tvGreeting, tanggalCurrent;
    private FrameLayout profileContainer;

    // Contact card
    private TextView tvContactNumber;
    private TextView tvContactLocation;
    private ImageView imgWhatsapp;

    // RecyclerView
    private RecyclerView rvPengumuman;
    private PengumumanAdapter pengumumanAdapter;

    // User Data (from session)
    private String idWarga, idRt, namaUser;
    private String currentPosPhone = null;

    // MVVM
    private DashboardViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Minta izin notifikasi saat pertama kali masuk dashboard (Android 13+)
        requestNotificationPermission();

        // Inisialisasi MVVM
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        sessionManager = new SessionManager(this);

        loadUserData();
        initViews();
        setTodayDate();
        setGreeting();
        setupLogoutLogic();

        setupMenuNavigation(menuAbsen, AbsenRondaActivity.class);
        setupMenuNavigation(menuTukar, TukarJadwalActivity.class);
        setupMenuNavigation(menuLapor, LaporanKeamananActivity.class);
        setupMenuNavigation(menuJadwal, JadwalRondaActivity.class);

        setupContactCard();
        observeViewModel();

        // Trigger data loading
        viewModel.loadPosRonda(idRt);
        viewModel.listenPengumuman();
    }

    // ========================================================================
    // ViewModel Observers
    // ========================================================================

    private void observeViewModel() {
        // Pos Ronda data
        viewModel.getPosRondaData().observe(this, posRonda -> {
            if (posRonda != null) {
                currentPosPhone = posRonda.getTelepon();
                if (currentPosPhone != null) {
                    tvContactNumber.setText(currentPosPhone);
                } else {
                    tvContactNumber.setText(getString(R.string.text_dash));
                }
                if (posRonda.getLokasi() != null) {
                    tvContactLocation.setText(posRonda.getLokasi());
                } else {
                    tvContactLocation.setText(getString(R.string.text_belum_diatur));
                }
            }
        });

        // Pengumuman data
        viewModel.getPengumumanList().observe(this, dataList -> {
            if (dataList != null) {
                pengumumanAdapter.updateData(dataList);
            }
        });

        // Error
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Log.e("Dashboard", msg);
            }
        });
    }

    // ========================================================================
    // UI Initialization
    // ========================================================================

    private void loadUserData() {
        idWarga = sessionManager.getIdWarga();
        idRt = sessionManager.getIdRt();
        namaUser = sessionManager.getNama();
        if (namaUser == null) {
            namaUser = getString(R.string.fallback_name_pengguna);
        }
    }

    private void initViews() {
        menuAbsen = findViewById(R.id.menuAbsen);
        menuTukar = findViewById(R.id.menuTukar);
        menuLapor = findViewById(R.id.menuLapor);
        menuJadwal = findViewById(R.id.menuJadwal);

        tvGreeting = findViewById(R.id.tvGreeting);
        tanggalCurrent = findViewById(R.id.tanggal_current);
        profileContainer = findViewById(R.id.profileContainer);

        tvContactNumber = findViewById(R.id.tvContactNumberText);
        tvContactLocation = findViewById(R.id.tvContactLocation);
        imgWhatsapp = findViewById(R.id.imgWhatsapp);

        rvPengumuman = findViewById(R.id.rvPengumuman);
        rvPengumuman.setLayoutManager(new LinearLayoutManager(this));
        pengumumanAdapter = new PengumumanAdapter(new ArrayList<>());
        rvPengumuman.setAdapter(pengumumanAdapter);
    }

    private void setGreeting() {
        tvGreeting.setText(getString(R.string.greeting_format, namaUser));
    }

    private void setTodayDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        tanggalCurrent.setText(sdf.format(cal.getTime()));
    }

    // ========================================================================
    // Navigation (tetap di Activity)
    // ========================================================================

    private void setupMenuNavigation(LinearLayout menu, Class<?> targetActivity) {
        if (menu == null)
            return;
        menu.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, targetActivity);
            intent.putExtra(Constants.EXTRA_ID_RT, idRt);
            intent.putExtra(Constants.EXTRA_ID_WARGA, idWarga);
            intent.putExtra(Constants.EXTRA_NAMA, namaUser);
            startActivity(intent);
        });
    }

    // ========================================================================
    // Logout (tetap di Activity)
    // ========================================================================

    private void setupLogoutLogic() {
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dialog_logout_title))
                        .setMessage(getString(R.string.dialog_logout_message))
                        .setPositiveButton(getString(R.string.dialog_logout_positive), (dialog, which) -> {
                            sessionManager.clearSession();
                            new AuthRepository().logout();

                            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(getString(R.string.dialog_logout_negative), null)
                        .show();
            });
        }
    }

    // ========================================================================
    // WhatsApp Contact
    // ========================================================================

    private void setupContactCard() {
        if (imgWhatsapp != null) {
            imgWhatsapp.setOnClickListener(v -> openWhatsapp());
        }
    }

    private void openWhatsapp() {
        if (currentPosPhone == null || currentPosPhone.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_whatsapp_number), Toast.LENGTH_SHORT).show();
            return;
        }
        String raw = currentPosPhone.replaceAll("[^0-9]", "");
        String international = raw.startsWith("0") ? "62" + raw.substring(1) : raw;
        String url = "https://wa.me/" + international;

        try {
            Intent waIntent = new Intent(Intent.ACTION_VIEW);
            waIntent.setData(Uri.parse(url));
            waIntent.setPackage("com.whatsapp");
            startActivity(waIntent);
        } catch (Exception e) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(this, getString(R.string.toast_no_whatsapp_app), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ========================================================================
    // Notification Permission (Android 13+ / API 33+)
    // ========================================================================

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
