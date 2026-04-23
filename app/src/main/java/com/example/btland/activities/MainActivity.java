package com.example.btland.activities;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.btland.R;
import com.example.btland.databinding.ActivityMainBinding;
import com.example.btland.fragments.AdminFragment;
import com.example.btland.fragments.HomeFragment;
import com.example.btland.fragments.MapFragment;
import com.example.btland.fragments.MessagesFragment;
import com.example.btland.fragments.ProfileFragment;
import com.example.btland.utils.ThemePreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String KEY_SELECTED_NAV_ID = "selected_nav_id";

    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private final Handler themeHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingThemeRunnable;
    private Integer pendingNightMode;
    private int selectedNavId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            selectedNavId = savedInstanceState.getInt(KEY_SELECTED_NAV_ID, R.id.nav_home);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            selectedNavId = id;

            if (id == R.id.nav_home) {
                replaceFragment(new HomeFragment());
                return true;
            }
            if (id == R.id.nav_map) {
                replaceFragment(new MapFragment());
                return true;
            }
            if (id == R.id.nav_messages) {
                replaceFragment(new MessagesFragment());
                return true;
            }
            if (id == R.id.nav_profile) {
                replaceFragment(new ProfileFragment());
                return true;
            }
            if (id == R.id.nav_admin) {
                replaceFragment(new AdminFragment());
                return true;
            }
            return false;
        });

        loadAdminMenuState();
        binding.bottomNavigation.setSelectedItemId(selectedNavId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLightSensorIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (pendingThemeRunnable != null) {
            themeHandler.removeCallbacks(pendingThemeRunnable);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV_ID, selectedNavId);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit();
    }

    private void loadAdminMenuState() {
        binding.bottomNavigation.getMenu().findItem(R.id.nav_admin).setVisible(false);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            if (selectedNavId == R.id.nav_admin) {
                selectedNavId = R.id.nav_home;
                binding.bottomNavigation.setSelectedItemId(selectedNavId);
            }
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = Boolean.TRUE.equals(documentSnapshot.getBoolean("isAdmin"));
                    binding.bottomNavigation.getMenu().findItem(R.id.nav_admin).setVisible(isAdmin);
                    if (!isAdmin && selectedNavId == R.id.nav_admin) {
                        selectedNavId = R.id.nav_home;
                        binding.bottomNavigation.setSelectedItemId(selectedNavId);
                    }
                });
    }

    private void registerLightSensorIfNeeded() {
        if (!ThemePreferences.isAutoThemeEnabled(this) || sensorManager == null || lightSensor == null) {
            return;
        }
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT || !ThemePreferences.isAutoThemeEnabled(this)) {
            return;
        }

        float lux = event.values[0];
        Integer targetMode = null;
        if (lux < 100f) {
            targetMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (lux > 300f) {
            targetMode = AppCompatDelegate.MODE_NIGHT_NO;
        }

        if (targetMode == null
                || targetMode.equals(pendingNightMode)
                || targetMode == ThemePreferences.getLastAppliedMode(this)) {
            return;
        }

        pendingNightMode = targetMode;
        if (pendingThemeRunnable != null) {
            themeHandler.removeCallbacks(pendingThemeRunnable);
        }

        pendingThemeRunnable = () -> {
            if (pendingNightMode != null) {
                ThemePreferences.setLastAppliedMode(this, pendingNightMode);
            }
        };
        themeHandler.postDelayed(pendingThemeRunnable, 2000L);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
