package com.example.jagawarga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;

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

    private static final String BASE_URL = "https://newsletter-cod-jeff-cement.trycloudflare.com/jagawarga/";
    private static final String GET_POS_RONDA_URL = BASE_URL + "get_pos_ronda.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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
        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
        idWarga   = prefs.getString("id", null);
        idRt      = prefs.getString("id_rt", null);
        namaUser  = prefs.getString("nama", "Pengguna");
    }

    private void initViews() {
        menuAbsen   = findViewById(R.id.menuAbsen);
        menuTukar   = findViewById(R.id.menuTukar);
        menuLapor   = findViewById(R.id.menuLapor);
        menuJadwal  = findViewById(R.id.menuJadwal);

        tvGreeting       = findViewById(R.id.tvGreeting);
        tanggalCurrent   = findViewById(R.id.tanggal_current);
        profileContainer = findViewById(R.id.profileContainer);

        tvContactNumber   = findViewById(R.id.tvContactNumberText);
        tvContactLocation = findViewById(R.id.tvContactLocation);
        imgWhatsapp   = findViewById(R.id.imgWhatsapp);

        // --- INISIALISASI RECYCLER VIEW ---
        rvPengumuman = findViewById(R.id.rvPengumuman);
        // Penting: Set Layout Manager
        rvPengumuman.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setGreeting() {
        tvGreeting.setText("Hai, " + namaUser + " !");
    }

    private void setTodayDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        tanggalCurrent.setText(sdf.format(cal.getTime()));
    }

    private void setupMenuNavigation(LinearLayout menu, Class<?> targetActivity) {
        if (menu == null) return;
        menu.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, targetActivity);
            intent.putExtra("id_rt", idRt);
            intent.putExtra("id_warga", idWarga);
            intent.putExtra("nama", namaUser);
            startActivity(intent);
        });

        if (menu == menuJadwal) {
            menuJadwal.setOnClickListener(v -> {
                String todayForApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().getTime());
                callApiCloudflare(idRt, todayForApi);
            });
        }
    }

    private void setupLogoutLogic() {
        if (profileContainer != null) {
            profileContainer.setOnClickListener(v -> {
                // 1. Hapus Session Lokal (SharedPreferences)
                SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
                prefs.edit().clear().apply();

                // 2. Logout dari Firebase Auth
                FirebaseAuth.getInstance().signOut();

                // 3. Pindah ke Halaman Login & Bersihkan Stack Activity
                // (Agar user tidak bisa kembali ke dashboard dengan tombol Back)
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    public void callApiCloudflare(String id_rt, String tanggal) {
        String url = BASE_URL + "get_jadwal.php?id_rt=" + id_rt + "&tanggal=" + tanggal;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Intent i = new Intent(DashboardActivity.this, JadwalRondaActivity.class);
                    i.putExtra("json_jadwal", response.toString());
                    startActivity(i);
                },
                error -> {
                    Toast.makeText(this, "Gagal menghubungi server", Toast.LENGTH_SHORT).show();
                });
        queue.add(req);
    }

    private void setupContactCard() {
        if (imgWhatsapp != null) {
            imgWhatsapp.setOnClickListener(v -> openWhatsapp());
        }
    }

    private void openWhatsapp() {
        if (currentPosPhone == null || currentPosPhone.isEmpty()) {
            Toast.makeText(this, "Nomor pos ronda belum tersedia", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Tidak ada aplikasi WhatsApp", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadPosRondaForUser() {
        if (idRt == null || idRt.isEmpty()) return;
        StringRequest request = new StringRequest(Request.Method.POST, GET_POS_RONDA_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success", false)) {
                            currentPosPhone = json.optString("nomor_telepon", "");
                            tvContactNumber.setText(currentPosPhone);
                            tvContactLocation.setText(json.optString("lokasi_pos", ""));
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id_rt", idRt);
                return params;
            }
        };
        Volley.newRequestQueue(this).add(request);
    }


    // --- FUNGSI LOAD PENGUMUMAN ---
    private void loadPengumuman() {
        // Karena idRt sudah diambil di loadUserData(), langsung pakai saja
        if(idRt == null) return;

        // Pastikan endpoint API ini sesuai dengan file PHP get_pengumuman.php kamu
        String url = BASE_URL + "get_pengumuman.php?id_rt=" + idRt;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.getBoolean("success")) {
                            JSONArray data = obj.getJSONArray("data");
                            // Set Adapter
                            PengumumanAdapter adapter = new PengumumanAdapter(data);
                            rvPengumuman.setAdapter(adapter);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> Log.e("API_PENGUMUMAN", "Error: " + error.toString())
        );
        Volley.newRequestQueue(this).add(req);
    }

    // --- INNER CLASS ADAPTER ---
    class PengumumanAdapter extends RecyclerView.Adapter<PengumumanAdapter.Holder> {
        JSONArray data;
        public PengumumanAdapter(JSONArray data) { this.data = data; }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pengumuman, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            try {
                JSONObject item = data.getJSONObject(position);
                holder.tvJudul.setText(item.getString("judul"));
                holder.tvIsi.setText(item.getString("isi"));

                // Pastikan key JSON 'tanggal_fmt' ada di PHP get_pengumuman.php
                if(item.has("tanggal_fmt")) {
                    holder.tvTanggal.setText(item.getString("tanggal_fmt"));
                } else {
                    holder.tvTanggal.setText(item.getString("tanggal"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        public int getItemCount() { return data.length(); }

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