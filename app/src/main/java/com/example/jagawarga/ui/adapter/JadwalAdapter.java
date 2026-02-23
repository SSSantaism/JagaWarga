package com.example.jagawarga.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jagawarga.R;
import com.example.jagawarga.data.repository.DataRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter untuk menampilkan daftar jadwal ronda secara dinamis.
 * Mendukung highlight untuk current user.
 */
public class JadwalAdapter extends RecyclerView.Adapter<JadwalAdapter.ViewHolder> {

    private List<DataRepository.JadwalItem> items = new ArrayList<>();
    private String currentUserId;

    public JadwalAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void updateData(List<DataRepository.JadwalItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_jadwal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataRepository.JadwalItem item = items.get(position);

        holder.tvNama.setText(item.nama != null ? item.nama : "-");

        String idLabel = holder.itemView.getContext()
                .getString(R.string.label_id_jadwal_format,
                        item.jadwalId != null ? item.jadwalId : "-");
        holder.tvIdJadwal.setText(idLabel);

        holder.tvShift.setText(holder.itemView.getContext()
                .getString(R.string.text_jadwal_time));

        // Highlight current user
        if (currentUserId != null && item.docId.equals(currentUserId)) {
            holder.container.setBackgroundResource(R.drawable.bg_schedule_item_selected);
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_schedule_item_normal);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout container;
        final TextView tvNama;
        final TextView tvIdJadwal;
        final TextView tvShift;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.layoutItem);
            tvNama = itemView.findViewById(R.id.textNama);
            tvIdJadwal = itemView.findViewById(R.id.textIdJadwal);
            tvShift = itemView.findViewById(R.id.textShift);
        }
    }
}
