package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.termux.R;
import com.termux.app.TermuxInstaller;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            TermuxInstaller.setupBootstrapIfNeeded(SplashActivity.this, () -> {
                startActivity(new Intent(SplashActivity.this, UltroidDeploymentActivity.class));
                finish();
            });
        }, SPLASH_DELAY);
    }
} 