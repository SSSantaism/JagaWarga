package com.example.jagawarga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private FirebaseFirestore db;

    // DATA USER RW (ambil dari SharedPreferences)
    private String idRw;
    private String namaRw;
    private String idRtRw; // kalau RW punya RT khusus atau bisa kosong

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_rw);

        db = FirebaseFirestore.getInstance();

        loadUserData();
        initViews();
        setGreeting();
        setTodayDate();
        setupMenuClick();
        setupLogout();
        loadPengumuman();
    }

    // =======================================
    // LOAD DATA USER RW DARI SHAREDPREF
    // =======================================
    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);

        idRw = prefs.getString(Constants.PREF_KEY_ID, null);
        namaRw = prefs.getString(Constants.PREF_KEY_NAMA, getString(R.string.fallback_name_pak_rw));
        idRtRw = prefs.getString(Constants.PREF_KEY_ID_RT, null); // optional, kalau mau dipakai

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
    }

    private void setGreeting() {
        // contoh: "Hai, Pak RW Budi !"
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
        // MENU: Kelola Ketua RT -> AturRtActivity
        if (menuKelolaKetuaRT != null) {
            menuKelolaKetuaRT.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardRwActivity.this, AturRtPromoteActivity.class);

                // kalau mau kirim data RW ke halaman Atur RT:
                intent.putExtra(Constants.EXTRA_ID_RW, idRw);
                intent.putExtra(Constants.EXTRA_NAMA_RW, namaRw);
                intent.putExtra(Constants.EXTRA_ID_RT_RW, idRtRw);

                startActivity(intent);
            });
        }

        // MENU: Buat Pengumuman -> BuatPengumumanActivity
        if (menuBuatPengumuman != null) {
            menuBuatPengumuman.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardRwActivity.this, BuatPengumumanActivity.class);
                // bisa juga kirim nama RW / id RW jika perlu
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
                            SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
                            prefs.edit().clear().apply();

                            FirebaseAuth.getInstance().signOut();

                            Intent intent = new Intent(DashboardRwActivity.this, LoginActivity.class);
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
    // LOAD PENGUMUMAN (UNIVERSAL)
    // =======================================
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