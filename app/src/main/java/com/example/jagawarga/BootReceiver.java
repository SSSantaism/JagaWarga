package com.example.jagawarga;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver untuk menangani BOOT_COMPLETED
 * Digunakan untuk reschedule alarm pengingat ronda setelah device restart
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed - rescheduling ronda reminder");

            // Ambil jadwal hari dari SharedPreferences
            String jadwalHari = PrefUtils.getJadwalHari(context);

            if (jadwalHari != null && !jadwalHari.isEmpty()) {
                // Reschedule alarm
                RondaReminderManager.scheduleWeeklyReminder(context, jadwalHari);
                Log.d(TAG, "Ronda reminder rescheduled for: " + jadwalHari);
            } else {
                Log.d(TAG, "No jadwal_hari found, skipping reschedule");
            }
        }
    }
}
