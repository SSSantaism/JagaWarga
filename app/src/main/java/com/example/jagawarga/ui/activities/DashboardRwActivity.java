package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class DashboardRwActivity extends AppCompatActivity {

    // HEADER
    private TextView tvGreeting, tvSubGreeting, tvTanggalCurrent;
    private FrameLayout profileContainer;
    private ImageView imgProfile;

    // MENU CARD (di dalam cardToday)
    private LinearLayout menuKelolaKetuaRT; // id: menuTerimaLaporan
    private LinearLayout menuBuatPengumuman; // id: menuBuatPengumuman

    // Pengumuman
    private RecyclerView rvPengumuman;

    // MVVM
    private DashboardViewModel viewModel;
    private SessionManager sessionManager;
    private PengumumanAdapter pengumumanAdapter;

    // DATA USER RW
    private String idRw;
    private String namaRw;
    private String idRtRw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_rw);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        sessionManager = new SessionManager(this);

        loadUserData();
        initViews();
        setGreeting();
        setTodayDate();
        setupMenuClick();
        setupLogout();

        observeViewModel();
        viewModel.loadPengumuman();
    }

    // =======================================
    // LOAD DATA USER RW DARI SESSION MANAGER
    // =======================================
    private void loadUserData() {
        idRw = sessionManager.getIdWarga();
        namaRw = sessionManager.getNama();
        if (namaRw == null)
            namaRw = getString(R.string.fallback_name_pak_rw);
        idRtRw = sessionManager.getIdRt();

        if (idRw == null) {
            Toast.makeText(this, getString(R.string.toast_rw_data_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    // =======================================
    // INIT VIEW
    // =======================================
    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvSubGreeting = findViewById(R.id.tvSubGreeting);
        tvTanggalCurrent = findViewById(R.id.tanggal_current);

        profileContainer = findViewById(R.id.profileContainer);
        imgProfile = findViewById(R.id.imgProfile);

        // menu di dalam cardToday
        menuKelolaKetuaRT = findViewById(R.id.menuTerimaLaporan); // teks: "Kelola Ketua RT"
        menuBuatPengumuman = findViewById(R.id.menuBuatPengumuman); // teks: "Buat Pengumuman"

        // Pengumuman RecyclerView
        rvPengumuman = findViewById(R.id.rvPengumuman);
        rvPengumuman.setLayoutManager(new LinearLayoutManager(this));
        pengumumanAdapter = new PengumumanAdapter(new ArrayList<>());
        rvPengumuman.setAdapter(pengumumanAdapter);
    }

    private void setGreeting() {
        tvGreeting.setText(getString(R.string.greeting_format, namaRw));
        tvSubGreeting.setText(getString(R.string.sub_greeting_ronda));
    }

    private void setTodayDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        String today = sdf.format(cal.getTime());
        tvTanggalCurrent.setText(today);
    }

    // =======================================
    // CLICK MENU
    // =======================================
    private void setupMenuClick() {
        if (menuKelolaKetuaRT != null) {
            menuKelolaKetuaRT.setOnClickListener(v -> {
                Intent intent = new Intent(this, AturRtPromoteActivity.class);
                intent.putExtra(Constants.EXTRA_ID_RW, idRw);
                intent.putExtra(Constants.EXTRA_NAMA_RW, namaRw);
                intent.putExtra(Constants.EXTRA_ID_RT_RW, idRtRw);
                startActivity(intent);
            });
        }

        if (menuBuatPengumuman != null) {
            menuBuatPengumuman.setOnClickListener(v -> {
                Intent intent = new Intent(this, BuatPengumumanActivity.class);
                intent.putExtra(Constants.EXTRA_ID_RW, idRw);
                startActivity(intent);
            });
        }
    }

    // =======================================
    // LOGOUT
    // =======================================
    private void setupLogout() {
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

    // =======================================
    // OBSERVE VIEWMODEL
    // =======================================
    private void observeViewModel() {
        viewModel.getPengumumanList().observe(this, dataList -> {
            if (dataList != null) {
                pengumumanAdapter.updateData(dataList);
            }
        });
    }
}