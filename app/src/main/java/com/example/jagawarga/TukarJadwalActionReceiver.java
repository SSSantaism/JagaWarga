package com.example.jagawarga;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.data.repository.TukarJadwalRepository;
import com.example.jagawarga.utils.Constants;

/**
 * BroadcastReceiver untuk menangani aksi tombol pada notifikasi tukar jadwal.
 * Menangani aksi "Setuju" dan "Tolak" langsung dari notification action
 * buttons.
 * Menggunakan TukarJadwalRepository untuk operasi Firestore.
 */
public class TukarJadwalActionReceiver extends BroadcastReceiver {

    private static final String TAG = "TukarJadwalAction";

    public static final String ACTION_ACCEPT = "com.example.jagawarga.ACTION_ACCEPT_SWAP";
    public static final String ACTION_REJECT = "com.example.jagawarga.ACTION_REJECT_SWAP";

    public static final String EXTRA_DOC_ID = "doc_id";
    public static final String EXTRA_NOTIF_ID = "notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String docId = intent.getStringExtra(EXTRA_DOC_ID);
        int notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0);

        if (docId == null || docId.isEmpty()) {
            Log.e(TAG, "Document ID is null or empty");
            return;
        }

        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notifId);

        TukarJadwalRepository repository = new TukarJadwalRepository();

        if (ACTION_ACCEPT.equals(action)) {
            handleAccept(context, repository, docId);
        } else if (ACTION_REJECT.equals(action)) {
            handleReject(context, repository, docId);
        }
    }

    /**
     * Handle aksi Setuju - tukar jadwal antara kedua user
     */
    private void handleAccept(Context context, TukarJadwalRepository repository, String docId) {
        Log.d(TAG, "Accepting swap request: " + docId);

        repository.getSwapDocument(docId, new TukarJadwalRepository.SwapDocCallback() {
            @Override
            public void onSuccess(String dariId, String kepadaId,
                    String hariDari, String hariKepada,
                    String jadwalIdDari, String jadwalIdKepada) {
                if (dariId == null || kepadaId == null) {
                    showToast(context, context.getString(R.string.toast_swap_data_incomplete));
                    return;
                }

                repository.performSwap(docId, dariId, kepadaId,
                        hariDari, hariKepada, jadwalIdDari, jadwalIdKepada,
                        new TukarJadwalRepository.SwapActionCallback() {
                            @Override
                            public void onSuccess() {
                                showToast(context, context.getString(R.string.toast_swap_success));

                                // Update local session jika user yang accept adalah current user
                                SessionManager session = new SessionManager(context);
                                String currentUserId = session.getIdWarga();
                                if (kepadaId.equals(currentUserId)) {
                                    session.setJadwalHari(hariDari);
                                    RondaReminderManager.scheduleWeeklyReminder(context, hariDari);
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showToast(context,
                                        context.getString(R.string.toast_swap_failed, errorMessage));
                            }
                        });
            }

            @Override
            public void onNotFound() {
                showToast(context, context.getString(R.string.toast_swap_request_not_found));
            }

            @Override
            public void onError(String errorMessage) {
                showToast(context,
                        context.getString(R.string.toast_swap_process_failed, errorMessage));
            }
        });
    }

    /**
     * Handle aksi Tolak
     */
    private void handleReject(Context context, TukarJadwalRepository repository, String docId) {
        Log.d(TAG, "Rejecting swap request: " + docId);

        repository.rejectSwap(docId, new TukarJadwalRepository.SwapActionCallback() {
            @Override
            public void onSuccess() {
                showToast(context, context.getString(R.string.toast_swap_rejected));
            }

            @Override
            public void onError(String errorMessage) {
                showToast(context,
                        context.getString(R.string.toast_generic_failed, errorMessage));
            }
        });
    }

    private void showToast(Context context, String message) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
