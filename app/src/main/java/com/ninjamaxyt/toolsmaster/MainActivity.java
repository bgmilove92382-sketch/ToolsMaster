package com.ninjamaxyt.toolsmaster;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private TextView tvDeviceInfo, tvEngineStatus, tvLog;
    private Spinner spGame, spResolution, spGraphics, spFps, spStyle;
    private Switch swShadow, swZeroLag;

    private final String[] gameLabels = {"India (BGMI)", "Global (PUBG)", "Korean (KR)", "Vietnam (VN)", "Taiwan (TW)", "PUBG Lite"};
    private final String[] gamePkgs = {"com.pubg.imobile", "com.tencent.ig", "com.pubg.krmobile", "com.vng.pubgmobile", "com.rekoo.pubgm", "com.tencent.iglite"};

    private final String[] resLabels = {"640x480 (VGA - Zero Lag)", "768x480 (WVGA)", "800x600 (SVGA)", "960x540 (qHD - Balanced)", "1280x720 (HD)", "1920x1080 (FHD)"};
    private final int[] resScales = {50, 60, 70, 80, 90, 100};

    private final String[] gfxLabels = {"Super Smooth (Max FPS)", "Smooth", "Balanced", "HD", "HDR"};
    private final int[] gfxVals = {0, 0, 1, 2, 3};

    private final String[] fpsLabels = {"Extreme (60 FPS)", "Extreme+ (90 FPS)", "Ultra Extreme (120 FPS)", "Ultra (40 FPS)", "High (30 FPS)"};
    private final int[] fpsVals = {6, 7, 8, 4, 3};

    private final String[] styleLabels = {"Colorful", "Classic", "Realistic", "Soft", "Movie"};
    private final int[] styleVals = {1, 0, 2, 3, 4};

    private final Shizuku.OnRequestPermissionResultListener shizukuListener = (requestCode, grantResult) -> updateStatus();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvEngineStatus = findViewById(R.id.tvEngineStatus);
        tvLog = findViewById(R.id.tvLog);
        spGame = findViewById(R.id.spGame);
        spResolution = findViewById(R.id.spResolution);
        spGraphics = findViewById(R.id.spGraphics);
        spFps = findViewById(R.id.spFps);
        spStyle = findViewById(R.id.spStyle);
        swShadow = findViewById(R.id.swShadow);
        swZeroLag = findViewById(R.id.swZeroLag);

        tvDeviceInfo.setText("Device: " + Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");

        setupSpinner(spGame, gameLabels);
        setupSpinner(spResolution, resLabels);
        setupSpinner(spGraphics, gfxLabels);
        setupSpinner(spFps, fpsLabels);
        setupSpinner(spStyle, styleLabels);

        try { Shizuku.addRequestPermissionResultListener(shizukuListener); } catch (Throwable ignored) {}
        updateStatus();

        findViewById(R.id.btnGrantAccess).setOnClickListener(v -> requestAccess());

        findViewById(R.id.btnPreset60).setOnClickListener(v -> {
            spResolution.setSelection(0);
            spGraphics.setSelection(0);
            spFps.setSelection(0);
            swShadow.setChecked(false);
            swZeroLag.setChecked(true);
            Toast.makeText(this, "Preset: Zero Lag 60 FPS Selected", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnPreset90).setOnClickListener(v -> {
            spResolution.setSelection(3);
            spGraphics.setSelection(0);
            spFps.setSelection(1);
            swShadow.setChecked(false);
            swZeroLag.setChecked(true);
            Toast.makeText(this, "Preset: Pro 90 FPS Selected", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnPreset120).setOnClickListener(v -> {
            spResolution.setSelection(3);
            spGraphics.setSelection(0);
            spFps.setSelection(2);
            swShadow.setChecked(false);
            swZeroLag.setChecked(true);
            Toast.makeText(this, "Preset: Ultra 120 FPS Selected", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnApply).setOnClickListener(v -> applySettings());
        findViewById(R.id.btnLaunch).setOnClickListener(v -> launchGame());
    }

    private void setupSpinner(Spinner sp, String[] items) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
    }

    private void updateStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (Shizuku.pingBinder()) {
                    boolean granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
                    tvEngineStatus.setText(granted ? "Engine Mode: Shizuku Active & Ready (Android 11-16)" : "Engine Mode: Shizuku Running — Tap Grant Permission");
                } else {
                    tvEngineStatus.setText("Engine Mode: Shizuku Not Running (Start Shizuku App First)");
                }
            } catch (Throwable t) {
                tvEngineStatus.setText("Engine Mode: Shizuku Required for Android 11+");
            }
        } else {
            tvEngineStatus.setText("Engine Mode: Direct Legacy Storage (Android 7-10 Ready)");
        }
    }

    private void requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (Shizuku.pingBinder()) {
                    if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                        Shizuku.requestPermission(101);
                    } else {
                        Toast.makeText(this, "Shizuku Permission Already Granted!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please start Shizuku service first!", Toast.LENGTH_LONG).show();
                }
            } catch (Throwable t) {
                Toast.makeText(this, "Install & Start Shizuku app!", Toast.LENGTH_LONG).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
        updateStatus();
    }

    private void applySettings() {
        String pkg = gamePkgs[spGame.getSelectedItemPosition()];
        int res = resScales[spResolution.getSelectedItemPosition()];
        int gfx = gfxVals[spGraphics.getSelectedItemPosition()];
        int fps = fpsVals[spFps.getSelectedItemPosition()];
        int style = styleVals[spStyle.getSelectedItemPosition()];
        boolean shadow = swShadow.isChecked();
        boolean zeroLag = swZeroLag.isChecked();

        tvLog.setText("Status: Patching binary Active.sav & INI configs...");
        new Thread(() -> {
            try {
                ConfigEngine.patchAndApply(this, pkg, res, gfx, fps, style, shadow, zeroLag);
                runOnUiThread(() -> {
                    tvLog.setText("Status: APPLIED! Do NOT tap FPS buttons inside game settings.");
                    Toast.makeText(this, "Settings Applied Successfully!", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvLog.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void launchGame() {
        String pkg = gamePkgs[spGame.getSelectedItemPosition()];
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) startActivity(launch);
        else Toast.makeText(this, "Selected game is not installed!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { Shizuku.removeRequestPermissionResultListener(shizukuListener); } catch (Throwable ignored) {}
    }
}
