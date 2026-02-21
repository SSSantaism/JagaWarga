package com.example.jagawarga;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

/**
 * Manager untuk mengatur pengingat jadwal ronda menggunakan AlarmManager
 */
public class RondaReminderManager {

    private static final String TAG = "RondaReminder";
    private static final int ALARM_REQUEST_CODE = 2001;

    // Jam pengingat (default: 18:00 / 6 PM, 2 jam sebelum ronda)
    private static final int REMINDER_HOUR = 18;
    private static final int REMINDER_MINUTE = 0;

    /**
     * Mengatur alarm pengingat berdasarkan hari jadwal ronda user
     * 
     * @param context    Context aplikasi
     * @param jadwalHari Hari jadwal ronda (Senin, Selasa, dst)
     */
    public static void scheduleWeeklyReminder(Context context, String jadwalHari) {
        if (jadwalHari == null || jadwalHari.isEmpty()) {
            Log.w(TAG, "jadwalHari is null or empty, skipping reminder setup");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RondaReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Hitung waktu alarm berikutnya
        Calendar calendar = getNextAlarmTime(jadwalHari);

        if (calendar == null) {
            Log.e(TAG, "Could not parse jadwalHari: " + jadwalHari);
            return;
        }

        // Set alarm repeating setiap minggu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Untuk Android 6.0+, gunakan setExactAndAllowWhileIdle agar lebih reliable
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
        } else {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 7, // Repeat every week
                    pendingIntent);
        }

        Log.d(TAG, "Reminder scheduled for " + jadwalHari + " at " + REMINDER_HOUR + ":" + REMINDER_MINUTE);
    }

    /**
     * Membatalkan alarm pengingat
     */
    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RondaReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Reminder cancelled");
    }

    /**
     * Menghitung waktu alarm berikutnya berdasarkan hari jadwal
     */
    private static Calendar getNextAlarmTime(String jadwalHari) {
        int targetDay = getDayOfWeek(jadwalHari);
        if (targetDay == -1)
            return null;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, REMINDER_HOUR);
        calendar.set(Calendar.MINUTE, REMINDER_MINUTE);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int daysUntilTarget = (targetDay - currentDay + 7) % 7;

        // Jika hari ini adalah hari target tapi waktu sudah lewat, jadwalkan minggu
        // depan
        if (daysUntilTarget == 0 && calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            daysUntilTarget = 7;
        }

        calendar.add(Calendar.DAY_OF_MONTH, daysUntilTarget);
        return calendar;
    }

    /**
     * Konversi nama hari Indonesia ke Calendar.DAY_OF_WEEK
     */
    private static int getDayOfWeek(String hari) {
        switch (hari.toLowerCase(Locale.ROOT)) {
            case "minggu":
                return Calendar.SUNDAY;
            case "senin":
                return Calendar.MONDAY;
            case "selasa":
                return Calendar.TUESDAY;
            case "rabu":
                return Calendar.WEDNESDAY;
            case "kamis":
                return Calendar.THURSDAY;
            case "jumat":
                return Calendar.FRIDAY;
            case "sabtu":
                return Calendar.SATURDAY;
            default:
                return -1;
        }
    }

    /**
     * BroadcastReceiver untuk menangani alarm
     */
    public static class RondaReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Alarm triggered - showing notification");

            // Pastikan notification channel sudah dibuat
            NotificationHelper.createNotificationChannels(context);

            // Tampilkan notifikasi
            NotificationHelper.showJadwalRondaNotification(context);

            // Untuk Android 6.0+ dengan setExactAndAllowWhileIdle, perlu reschedule
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String jadwalHari = PrefUtils.getJadwalHari(context);
                if (jadwalHari != null) {
                    // Schedule untuk minggu depan
                    scheduleWeeklyReminder(context, jadwalHari);
                }
            }
        }
    }
}
