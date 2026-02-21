package com.example.jagawarga;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AturRtPromoteActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etIdWarga, etTelepon;
    private Button btnAction;
    private Button tabPromosikan, tabTurunkan;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_rt);

        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        etIdWarga = findViewById(R.id.inputIDWarga);
        etTelepon = findViewById(R.id.inputPhoneWarga);
        btnAction = findViewById(R.id.btnActionPromote);
        tabPromosikan = findViewById(R.id.btnTabPromote);
        tabTurunkan = findViewById(R.id.btnTabRevoke);

        btnBack.setOnClickListener(v -> finish());

        // Tab navigation
        tabTurunkan.setOnClickListener(v -> {
            Intent intent = new Intent(this, AturRtRevokeActivity.class);
            startActivity(intent);
            finish();
        });

        btnAction.setOnClickListener(v -> {
            String rawPhone = etTelepon.getText().toString().trim();

            if (rawPhone.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_phone_required), Toast.LENGTH_SHORT).show();
                return;
            }

            // Format nomor ke +62
            String formatted = formatPhoneNumber(rawPhone);

            // Cari user berdasarkan nomor telepon
            db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo(Constants.FIELD_TELEPON, formatted)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, getString(R.string.toast_user_not_found), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String uid = doc.getId();

                            // Update role jadi KetuaRT
                            Map<String, Object> updates = new HashMap<>();
                            updates.put(Constants.FIELD_ROLE, Constants.ROLE_KETUA_RT);

                            db.collection(Constants.COLLECTION_USERS).document(uid)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, getString(R.string.toast_promote_success),
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                getString(R.string.toast_update_failed, e.getMessage()),
                                                Toast.LENGTH_SHORT).show();
                                    });
                            break;
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, getString(R.string.toast_search_user_failed, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }
}
