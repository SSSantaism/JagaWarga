package com.example.jagawarga.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.R;
import com.example.jagawarga.utils.Constants;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter untuk menampilkan daftar pengumuman di RecyclerView.
 * Dipindahkan dari inner class DashboardActivity.
 */
public class PengumumanAdapter extends RecyclerView.Adapter<PengumumanAdapter.Holder> {

    private List<Map<String, Object>> data;

    public PengumumanAdapter(List<Map<String, Object>> data) {
        this.data = data;
    }

    public void updateData(List<Map<String, Object>> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pengumuman, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        try {
            Map<String, Object> item = data.get(position);

            // Format judul: "RT [id_rt] - [judul]"
            String idRtPengumuman = (String) item.get(Constants.FIELD_ID_RT);
            String judul = (String) item.get(Constants.FIELD_JUDUL);
            String formattedJudul = holder.itemView.getContext().getString(
                    R.string.pengumuman_judul_format,
                    idRtPengumuman != null ? idRtPengumuman : "-",
                    judul != null ? judul : "-");
            holder.tvJudul.setText(formattedJudul);

            // Isi pengumuman
            String isi = (String) item.get(Constants.FIELD_ISI);
            holder.tvIsi.setText(isi != null ? isi
                    : holder.itemView.getContext().getString(R.string.text_dash));

            // Format tanggal: "DD MMM" (contoh: "12 Des")
            Timestamp timestamp = (Timestamp) item.get(Constants.FIELD_TANGGAL);
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", new Locale("id", "ID"));
                holder.tvTanggal.setText(sdf.format(date));
            } else {
                holder.tvTanggal.setText(holder.itemView.getContext().getString(R.string.text_dash));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvJudul, tvIsi, tvTanggal;

        public Holder(@NonNull View v) {
            super(v);
            tvJudul = v.findViewById(R.id.tvJudulPengumuman);
            tvIsi = v.findViewById(R.id.tvIsiPengumuman);
            tvTanggal = v.findViewById(R.id.tvTanggalPengumuman);
        }
    }
}
