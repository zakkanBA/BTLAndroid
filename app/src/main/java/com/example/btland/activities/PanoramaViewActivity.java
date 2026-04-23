package com.example.btland.activities;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.btland.databinding.ActivityPanoramaViewBinding;

public class PanoramaViewActivity extends AppCompatActivity implements SensorEventListener {

    private ActivityPanoramaViewBinding binding;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private long lastTimestamp;
    private float normalizedOffset = 0.5f;
    private int maxScrollX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPanoramaViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        String panoramaUrl = getIntent().getStringExtra("panorama_url");
        if (panoramaUrl == null || panoramaUrl.isEmpty()) {
            Toast.makeText(this, "Không có ảnh panorama", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.txtHint.setText(gyroscope == null
                ? "Thiết bị không có gyroscope. Vuốt ngang để xoay ảnh 360."
                : "Xoay điện thoại để đổi góc nhìn. Bạn vẫn có thể vuốt ngang để xoay.");

        loadPanorama(panoramaUrl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        lastTimestamp = 0L;
    }

    private void loadPanorama(String panoramaUrl) {
        Glide.with(this)
                .asBitmap()
                .load(panoramaUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        binding.imgPanorama.setImageBitmap(resource);

                        binding.imgPanorama.getViewTreeObserver().addOnGlobalLayoutListener(
                                new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        binding.imgPanorama.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                        int screenHeight = binding.horizontalScroll.getHeight();
                                        if (screenHeight <= 0) {
                                            return;
                                        }

                                        float ratio = (float) resource.getWidth() / (float) resource.getHeight();
                                        int panoramaWidth = (int) (screenHeight * ratio);
                                        binding.imgPanorama.getLayoutParams().width = panoramaWidth;
                                        binding.imgPanorama.getLayoutParams().height = screenHeight;
                                        binding.imgPanorama.requestLayout();

                                        binding.horizontalScroll.post(() -> {
                                            maxScrollX = Math.max(0, panoramaWidth - binding.horizontalScroll.getWidth());
                                            int centerX = maxScrollX / 2;
                                            binding.horizontalScroll.scrollTo(centerX, 0);
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE || maxScrollX <= 0) {
            return;
        }

        if (lastTimestamp != 0L) {
            float deltaSeconds = (event.timestamp - lastTimestamp) / 1_000_000_000f;
            normalizedOffset -= event.values[1] * deltaSeconds * 0.08f;
            normalizedOffset = Math.max(0f, Math.min(1f, normalizedOffset));
            binding.horizontalScroll.scrollTo((int) (normalizedOffset * maxScrollX), 0);
        }
        lastTimestamp = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
