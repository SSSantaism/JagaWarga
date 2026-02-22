package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.PrefUtils;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;
import com.example.jagawarga.BootReceiver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    // UI Components
    private LinearLayout menuAbsen, menuTukar, menuLapor, menuJadwal;
    private TextView tvGreeting, tanggalCurrent;
    private FrameLayout profileContainer;

    // Contact card
    private TextView tvContactNumber;
    private TextView tvContactLocation;
    private ImageView imgWhatsapp;

    // --- TAMBAHAN VARIABEL RECYCLERVIEW ---
    private RecyclerView rvPengumuman;

    // User Data
    private String idWarga, idRt, namaUser;
    private String currentPosPhone = null;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();

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
        loadPosRondaForUser();
        loadPengumuman();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
        idWarga = prefs.getString(Constants.PREF_KEY_ID, null);
        idRt = prefs.getString(Constants.PREF_KEY_ID_RT, null);
        namaUser = prefs.getString(Constants.PREF_KEY_NAMA, getString(R.string.fallback_name_pengguna));
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
    }

    private void setGreeting() {
        tvGreeting.setText(getString(R.string.greeting_format, namaUser));
    }

    private void setTodayDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        tanggalCurrent.setText(sdf.format(cal.getTime()));
    }

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

    private void setupLogoutLogic() {
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dialog_logout_title))
                        .setMessage(getString(R.string.dialog_logout_message))
                        .setPositiveButton(getString(R.string.dialog_logout_positive), (dialog, which) -> {
                            SharedPreferences prefs = getSharedPreferences(Constants.PREF_USER_DATA, MODE_PRIVATE);
                            prefs.edit().clear().apply();

                            FirebaseAuth.getInstance().signOut();

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

    private void loadPosRondaForUser() {
        if (idRt == null || idRt.isEmpty())
            return;

        db.collection(Constants.COLLECTION_DATA_RT).document(idRt)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentPosPhone = documentSnapshot.getString(Constants.FIELD_POS_PHONE);
                        String lokasi = documentSnapshot.getString(Constants.FIELD_LOKASI);
                        if (currentPosPhone != null)
                            tvContactNumber.setText(currentPosPhone);
                        if (lokasi != null)
                            tvContactLocation.setText(lokasi);
                    } else {
                        // Default dummy data if not set yet
                        tvContactLocation.setText(getString(R.string.text_belum_diatur));
                        tvContactNumber.setText(getString(R.string.text_dash));
                    }
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE_RT", e.getMessage()));
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
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", new Locale("id", "ID"));
                    holder.tvTanggal.setText(sdf.format(date));
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
