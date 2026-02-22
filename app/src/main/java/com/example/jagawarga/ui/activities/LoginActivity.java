package com.example.jagawarga.ui.activities;

import com.example.jagawarga.R;
import com.example.jagawarga.PrefUtils;
import com.example.jagawarga.NotificationHelper;
import com.example.jagawarga.RondaReminderManager;
import com.example.jagawarga.TukarJadwalListener;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.jagawarga.data.model.User;
import com.example.jagawarga.data.repository.SessionManager;
import com.example.jagawarga.utils.Constants;
import com.example.jagawarga.viewmodel.LoginViewModel;
import com.google.firebase.messaging.FirebaseMessaging;

import android.text.InputType;

public class LoginActivity extends AppCompatActivity {

    // UI Components
    private ViewGroup mainContainer;
    private LinearLayout layoutLogin, layoutRegister;
    private Button btnMasukTab, btnDaftarTab;

    // UI Login
    private EditText inputPhoneLogin, inputPasswordLogin;
    private Button btnLogin;

    // UI Register
    private EditText inputNamaReg, inputPhoneReg, inputPassReg;
    private Spinner inputRtReg;
    private Button btnRegisterAction;

    // Password Toggle
    private ImageView btnTogglePassLogin, btnTogglePassReg;
    private boolean isPasswordVisibleLogin = false;
    private boolean isPasswordVisibleReg = false;

    // Remember Me
    private CheckBox checkRemember;
    private boolean isAutoLogin = false;

    // MVVM
    private LoginViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi MVVM
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        sessionManager = new SessionManager(this);

        // Cek auto-login (jika "Ingat Saya" aktif)
        if (sessionManager.isRememberMe()
                && sessionManager.getIdWarga() != null) {
            isAutoLogin = true;
            viewModel.checkAutoLogin();
        } else if (sessionManager.getIdWarga() != null) {
            // User tidak centang "Ingat Saya", clear session
            viewModel.logoutIfNeeded();
            sessionManager.clearSession();
        }

        initViews();
        setupSpinnerRt();
        setupTabs();
        setupActionButtons();
        observeViewModel();
    }

    // ========================================================================
    // ViewModel Observers
    // ========================================================================

    private void observeViewModel() {
        // Loading state
        viewModel.getIsLoading().observe(this, this::showLoading);

        // Login result
        viewModel.getLoginResult().observe(this, result -> {
            if (result == null)
                return;

            switch (result.getStatus()) {
                case SUCCESS:
                    User user = result.getUser();
                    saveSession(user);
                    redirectDashboard(user.getRole(), user.getNama());
                    break;

                case PENDING:
                    Toast.makeText(this, getString(R.string.toast_account_pending),
                            Toast.LENGTH_LONG).show();
                    break;

                case REJECTED:
                    Toast.makeText(this, getString(R.string.toast_account_rejected),
                            Toast.LENGTH_LONG).show();
                    break;

                case NOT_FOUND:
                    Toast.makeText(this, getString(R.string.toast_profile_not_found),
                            Toast.LENGTH_LONG).show();
                    break;

                case ERROR:
                    Toast.makeText(this,
                            getString(R.string.toast_login_failed, result.getErrorMessage()),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        });

        // Register result
        viewModel.getRegisterResult().observe(this, result -> {
            if (result == null)
                return;

            if (result.isSuccess()) {
                // Redirect ke halaman Pending Register
                Intent intent = new Intent(LoginActivity.this, PendingRegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this,
                        getString(R.string.toast_register_failed, result.getErrorMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ========================================================================
    // UI Initialization (tetap di Activity)
    // ========================================================================

    private void initViews() {
        mainContainer = findViewById(R.id.mainContainer);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);

        btnMasukTab = findViewById(R.id.btnMasukTab);
        btnDaftarTab = findViewById(R.id.btnDaftarTab);

        // Login Inputs
        inputPhoneLogin = findViewById(R.id.inputPhoneLogin);
        inputPasswordLogin = findViewById(R.id.inputPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        checkRemember = findViewById(R.id.checkRemember);
        checkRemember.setChecked(false);

        // Register Inputs
        inputNamaReg = findViewById(R.id.inputNamaReg);
        inputPhoneReg = findViewById(R.id.inputPhoneReg);
        inputRtReg = findViewById(R.id.inputRtReg);
        inputPassReg = findViewById(R.id.inputPassReg);
        btnRegisterAction = findViewById(R.id.btnRegisterAction);

        inputPasswordLogin.setEnabled(true);

        // Setup Password Toggle untuk Login
        btnTogglePassLogin = findViewById(R.id.btnTogglePassLogin);
        btnTogglePassLogin.setOnClickListener(v -> {
            isPasswordVisibleLogin = !isPasswordVisibleLogin;
            if (isPasswordVisibleLogin) {
                inputPasswordLogin.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassLogin.setImageResource(R.drawable.icon_mata1);
            } else {
                inputPasswordLogin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassLogin.setImageResource(R.drawable.icon_mata2);
            }
            inputPasswordLogin.setSelection(inputPasswordLogin.getText().length());
        });

        // Setup Password Toggle untuk Register
        btnTogglePassReg = findViewById(R.id.btnTogglePassReg);
        btnTogglePassReg.setOnClickListener(v -> {
            isPasswordVisibleReg = !isPasswordVisibleReg;
            if (isPasswordVisibleReg) {
                inputPassReg.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassReg.setImageResource(R.drawable.icon_mata1);
            } else {
                inputPassReg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassReg.setImageResource(R.drawable.icon_mata2);
            }
            inputPassReg.setSelection(inputPassReg.getText().length());
        });
    }

    private void setupSpinnerRt() {
        String[] rtOptions = { "01", "02", "03", "04" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rtOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputRtReg.setAdapter(adapter);
    }

    private void setupTabs() {
        btnMasukTab.setOnClickListener(v -> {
            if (layoutLogin.getVisibility() == View.VISIBLE)
                return;
            TransitionManager.beginDelayedTransition(mainContainer);
            layoutRegister.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
            updateTabStyle(true);
        });

        btnDaftarTab.setOnClickListener(v -> {
            if (layoutRegister.getVisibility() == View.VISIBLE)
                return;
            TransitionManager.beginDelayedTransition(mainContainer);
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.VISIBLE);
            updateTabStyle(false);
        });
    }

    private void updateTabStyle(boolean isLoginActive) {
        if (isLoginActive) {
            btnMasukTab.setBackgroundResource(R.drawable.rounded_button);
            btnMasukTab.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnMasukTab.setTextColor(Color.BLACK);
            btnDaftarTab.setBackgroundColor(Color.TRANSPARENT);
            btnDaftarTab.setTextColor(ContextCompat.getColor(this, R.color.gray));
        } else {
            btnDaftarTab.setBackgroundResource(R.drawable.rounded_button);
            btnDaftarTab.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnDaftarTab.setTextColor(Color.BLACK);
            btnMasukTab.setBackgroundColor(Color.TRANSPARENT);
            btnMasukTab.setTextColor(ContextCompat.getColor(this, R.color.gray));
        }
    }

    // ========================================================================
    // Action Buttons → delegate ke ViewModel
    // ========================================================================

    private void setupActionButtons() {
        // LOGIN
        btnLogin.setOnClickListener(v -> {
            String rawPhone = inputPhoneLogin.getText().toString().trim();
            String password = inputPasswordLogin.getText().toString().trim();

            if (rawPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_phone_password), Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.login(rawPhone, password);
        });

        // REGISTER
        btnRegisterAction.setOnClickListener(v -> {
            String nama = inputNamaReg.getText().toString().trim();
            String rawPhone = inputPhoneReg.getText().toString().trim();
            String password = inputPassReg.getText().toString().trim();
            String rt = inputRtReg.getSelectedItem().toString();

            if (nama.isEmpty() || rawPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_all_data_required), Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, getString(R.string.toast_password_min_length), Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.register(nama, rawPhone, password, rt);
        });
    }

    // ========================================================================
    // Session & Navigation (dipicu oleh observer)
    // ========================================================================

    private void saveSession(User user) {
        // Save user data ke SharedPreferences
        sessionManager.saveSession(user.getId(), user.getRole(), user.getNama(),
                user.getIdRt(), user.getJadwalId(), user.getJadwalHari());

        // Remember me logic
        boolean rememberMe;
        if (isAutoLogin) {
            rememberMe = sessionManager.isRememberMe();
        } else if (checkRemember != null) {
            rememberMe = checkRemember.isChecked();
        } else {
            rememberMe = sessionManager.isRememberMe();
        }
        sessionManager.setRememberMe(rememberMe);
    }

    private void redirectDashboard(String role, String namaUser) {
        // Setup notifikasi setelah login berhasil
        setupNotifications();

        Intent intent;
        if (role != null && role.equalsIgnoreCase(Constants.ROLE_KETUA_RT)) {
            intent = new Intent(this, DashboardRtActivity.class);
        } else if (role != null && role.equalsIgnoreCase(Constants.ROLE_KETUA_RW)) {
            intent = new Intent(this, DashboardRwActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
        intent.putExtra(Constants.EXTRA_NAMA_USER, namaUser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ========================================================================
    // Notifications (tetap di Activity karena butuh Context)
    // ========================================================================

    private void setupNotifications() {
        requestNotificationPermission();
        NotificationHelper.createNotificationChannels(this);

        String idRt = sessionManager.getIdRt();
        if (idRt != null) {
            String topic = "rt_" + idRt;
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FCM", "Subscribed to topic: " + topic);
                        } else {
                            Log.e("FCM", "Failed to subscribe to topic: " + topic);
                        }
                    });
        }

        String role = sessionManager.getRole();
        String jadwalHari = sessionManager.getJadwalHari();
        if (Constants.ROLE_WARGA.equals(role) && jadwalHari != null) {
            RondaReminderManager.scheduleWeeklyReminder(this, jadwalHari);
            Log.d("Ronda", "Scheduled reminder for: " + jadwalHari);
        }

        String userId = sessionManager.getIdWarga();
        if (Constants.ROLE_WARGA.equals(role) && userId != null) {
            TukarJadwalListener.startListening(this, userId);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Notification permission granted");
            } else {
                Log.d("Permission", "Notification permission denied");
            }
        }
    }

    // ========================================================================
    // UI Helper
    // ========================================================================

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            Toast.makeText(this, getString(R.string.toast_loading), Toast.LENGTH_SHORT).show();
            btnLogin.setEnabled(false);
            btnRegisterAction.setEnabled(false);
        } else {
            btnLogin.setEnabled(true);
            btnRegisterAction.setEnabled(true);
        }
    }
}