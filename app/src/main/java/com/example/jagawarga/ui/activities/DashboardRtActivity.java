package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.PrefUtils;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardRtActivity extends AppCompatActivity {

    // Menu utama
    private LinearLayout menuTerimaLaporan;
    private LinearLayout menuListPermintaan;
    private LinearLayout menuBuatPengumuman;
    private FrameLayout profileContainer;
    private RecyclerView rvPengumuman;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_rt);

        db = FirebaseFirestore.getInstance();

        // ====== 1. Greeting nama RT ======
        String namaRt = getIntent().getStringExtra(Constants.EXTRA_NAMA_USER);
        if (namaRt == null || namaRt.trim().isEmpty()) {
            namaRt = getString(R.string.fallback_name_pak_rt);
        }

        TextView tvGreetingRt = findViewById(R.id.tvGreetingRt);
        tvGreetingRt.setText(getString(R.string.greeting_format, namaRt));

        // ====== 2. Set tanggal hari ini ======
        TextView tvTanggalRt = findViewById(R.id.tvTanggalRt);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        String tanggal = sdf.format(calendar.getTime());
        tvTanggalRt.setText(tanggal);

        // ====== 3. Inisialisasi menu ======
        menuTerimaLaporan = findViewById(R.id.menuTerimaLaporan);
        menuListPermintaan = findViewById(R.id.menuListPermintaan);
        menuBuatPengumuman = findViewById(R.id.menuBuatPengumuman);

        // ====== 4. Setup RecyclerView Pengumuman ======
        rvPengumuman = findViewById(R.id.rvPengumuman);
        rvPengumuman.setLayoutManager(new LinearLayoutManager(this));
        loadPengumuman();

        // ====== 5. Setup Listener (Navigasi) ======

        menuTerimaLaporan.setOnClickListener(v -> {
            startActivity(new Intent(DashboardRtActivity.this, TerimaLaporanActivity.class));
        });

        menuBuatPengumuman.setOnClickListener(v -> {
            // Arahkan ke halaman Buat Pengumuman
            Intent intent = new Intent(DashboardRtActivity.this, BuatPengumumanActivity.class);
            startActivity(intent);
        });

        menuListPermintaan.setOnClickListener(v -> {
            // Mengarahkan ke ListPermintaan (activity_list_permintaan_register.xml)
            startActivity(new Intent(this, ListPermintaanActivity.class));
        });

        // ====== 6. Setup Logout ======
        profileContainer = findViewById(R.id.profileContainer);
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dialog_logout_title))
                        .setMessage(getString(R.string.dialog_logout_message))
                        .setPositiveButton(getString(R.string.dialog_logout_positive), (dialog, which) -> {
                            SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
                            prefs.edit().clear().apply();

                            FirebaseAuth.getInstance().signOut();

                            Intent intent = new Intent(DashboardRtActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(getString(R.string.dialog_logout_negative), null)
                        .show();
            });
        }
    }

    // --- FUNGSI LOAD PENGUMUMAN (FIRESTORE) - UNIVERSAL UNTUK SEMUA USER ---
    private void loadPengumuman() {
        db.collection(Constants.COLLECTION_PENGUMUMAN)
                .orderBy(Constants.FIELD_TANGGAL, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("PENGUMUMAN", "Loaded " + queryDocumentSnapshots.size() + " pengumuman");
                    List<Map<String, Object>> dataList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        dataList.add(doc.getData());
                    }
                    PengumumanAdapter adapter = new PengumumanAdapter(dataList);
                    rvPengumuman.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e("PENGUMUMAN", "Error loading pengumuman: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    // --- INNER CLASS ADAPTER ---
    class PengumumanAdapter extends RecyclerView.Adapter<PengumumanAdapter.Holder> {
        List<Map<String, Object>> data;

        public PengumumanAdapter(List<Map<String, Object>> data) {
            this.data = data;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pengumuman, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            try {
                Map<String, Object> item = data.get(position);

                // Format judul: "RT [id_rt] - [judul]"
                String idRtPengumuman = (String) item.get(Constants.FIELD_ID_RT);
                String judul = (String) item.get(Constants.FIELD_JUDUL);
                String formattedJudul = getString(R.string.pengumuman_judul_format,
                        idRtPengumuman != null ? idRtPengumuman : "-",
                        judul != null ? judul : "-");
                holder.tvJudul.setText(formattedJudul);

                // Isi pengumuman
                String isi = (String) item.get(Constants.FIELD_ISI);
                holder.tvIsi.setText(isi != null ? isi : getString(R.string.text_dash));

                // Format tanggal: "DD MMM" (contoh: "12 Des")
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) item
                        .get(Constants.FIELD_TANGGAL);
                if (timestamp != null) {
                    Date date = timestamp.toDate();
                    SimpleDateFormat sdfTanggal = new SimpleDateFormat("dd MMM", new Locale("id", "ID"));
                    holder.tvTanggal.setText(sdfTanggal.format(date));
                } else {
                    holder.tvTanggal.setText(getString(R.string.text_dash));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvJudul, tvIsi, tvTanggal;

            public Holder(View v) {
                super(v);
                tvJudul = v.findViewById(R.id.tvJudulPengumuman);
                tvIsi = v.findViewById(R.id.tvIsiPengumuman);
                tvTanggal = v.findViewById(R.id.tvTanggalPengumuman);
            }
        }
    }
}
