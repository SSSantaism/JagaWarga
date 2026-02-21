package com.example.jagawarga;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Helper class untuk menampilkan notifikasi lokal
 */
public class NotificationHelper {

    // Notification Channel IDs
    public static final String CHANNEL_PENGUMUMAN = "channel_pengumuman";
    public static final String CHANNEL_JADWAL = "channel_jadwal";
    public static final String CHANNEL_TUKAR = "channel_tukar";

    // Notification IDs
    public static final int NOTIF_ID_PENGUMUMAN = 1001;
    public static final int NOTIF_ID_JADWAL = 1002;
    public static final int NOTIF_ID_TUKAR = 1003;

    /**
     * Membuat semua notification channels (wajib untuk Android 8.0+)
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Channel untuk Pengumuman
            NotificationChannel channelPengumuman = new NotificationChannel(
                    CHANNEL_PENGUMUMAN,
                    context.getString(R.string.notif_channel_pengumuman),
                    NotificationManager.IMPORTANCE_HIGH);
            channelPengumuman.setDescription(context.getString(R.string.notif_channel_pengumuman_desc));
            manager.createNotificationChannel(channelPengumuman);

            // Channel untuk Jadwal Ronda
            NotificationChannel channelJadwal = new NotificationChannel(
                    CHANNEL_JADWAL,
                    context.getString(R.string.notif_channel_jadwal),
                    NotificationManager.IMPORTANCE_HIGH);
            channelJadwal.setDescription(context.getString(R.string.notif_channel_jadwal_desc));
            manager.createNotificationChannel(channelJadwal);

            // Channel untuk Tukar Jadwal
            NotificationChannel channelTukar = new NotificationChannel(
                    CHANNEL_TUKAR,
                    context.getString(R.string.notif_channel_tukar),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channelTukar.setDescription(context.getString(R.string.notif_channel_tukar_desc));
            manager.createNotificationChannel(channelTukar);
        }
    }

    /**
     * Menampilkan notifikasi pengumuman baru
     */
    public static void showPengumumanNotification(Context context, String judul, String isi) {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PENGUMUMAN)
                .setSmallIcon(R.drawable.jjagawarga_icon)
                .setContentTitle(context.getString(R.string.notif_title_pengumuman_format, judul))
                .setContentText(isi)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(isi))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIF_ID_PENGUMUMAN, builder.build());
        } catch (SecurityException e) {
            // Permission not granted
            e.printStackTrace();
        }
    }

    /**
     * Menampilkan notifikasi pengingat jadwal ronda
     */
    public static void showJadwalRondaNotification(Context context) {
        Intent intent = new Intent(context, JadwalRondaActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_JADWAL)
                .setSmallIcon(R.drawable.jjagawarga_icon)
                .setContentTitle(context.getString(R.string.notif_title_jadwal_ronda))
                .setContentText(context.getString(R.string.notif_body_jadwal_ronda))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIF_ID_JADWAL, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Menampilkan notifikasi permintaan tukar jadwal dengan tombol Setuju/Tolak
     * 
     * @param context   Context aplikasi
     * @param dariNama  Nama pengirim permintaan
     * @param hariTukar Hari jadwal yang ingin ditukar
     * @param docId     Document ID dari permintaan di Firestore
     */
    public static void showTukarJadwalNotification(Context context, String dariNama, String hariTukar, String docId) {
        // Generate unique notification ID based on document ID
        int notifId = NOTIF_ID_TUKAR + docId.hashCode();

        // Intent untuk buka app saat notifikasi di-tap
        Intent tapIntent = new Intent(context, DashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent tapPendingIntent = PendingIntent.getActivity(
                context, notifId, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent untuk tombol SETUJU
        Intent acceptIntent = new Intent(context, TukarJadwalActionReceiver.class);
        acceptIntent.setAction(TukarJadwalActionReceiver.ACTION_ACCEPT);
        acceptIntent.putExtra(TukarJadwalActionReceiver.EXTRA_DOC_ID, docId);
        acceptIntent.putExtra(TukarJadwalActionReceiver.EXTRA_NOTIF_ID, notifId);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                context, notifId + 1, acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent untuk tombol TOLAK
        Intent rejectIntent = new Intent(context, TukarJadwalActionReceiver.class);
        rejectIntent.setAction(TukarJadwalActionReceiver.ACTION_REJECT);
        rejectIntent.putExtra(TukarJadwalActionReceiver.EXTRA_DOC_ID, docId);
        rejectIntent.putExtra(TukarJadwalActionReceiver.EXTRA_NOTIF_ID, notifId);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                context, notifId + 2, rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = context.getString(R.string.notif_body_tukar_jadwal, dariNama, hariTukar);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TUKAR)
                .setSmallIcon(R.drawable.jjagawarga_icon)
                .setContentTitle(context.getString(R.string.notif_title_tukar_jadwal))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Tingkatkan priority untuk action buttons
                .setContentIntent(tapPendingIntent)
                .setAutoCancel(true)
                // Tambahkan action buttons
                .addAction(R.drawable.ic_check, context.getString(R.string.notif_action_accept), acceptPendingIntent)
                .addAction(R.drawable.ic_close, context.getString(R.string.notif_action_reject), rejectPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notifId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
