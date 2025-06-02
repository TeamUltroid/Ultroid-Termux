package com.termux.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.termux.R;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.android.PermissionUtils;

/**
 * Main activity for Ultroid deployment in Termux
 */
public class UltroidDeploymentActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ServiceConnection {

    private BottomNavigationView mBottomNavigationView;
    private TermuxService mTermuxService;
    private boolean mIsBound = false;
    
    // Keep track of ServiceConnection attempts
    private int mBindRetryCount = 0;
    private static final int MAX_BIND_RETRIES = 5;
    private static final long BIND_RETRY_DELAY_MS = 2000;
    
    private android.os.Handler mHandler = new android.os.Handler();
    private Runnable mBindRetryRunnable;

    private static final String LOG_TAG = "UltroidDeploymentActivity";
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1234;
    
    // For handling permission results with the new ActivityResultLauncher API
    private androidx.activity.result.ActivityResultLauncher<Intent> mOverlayPermissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ultroid_deployment);
        
        // Register the ActivityResultLauncher for overlay permission
        mOverlayPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Logger.logInfo(LOG_TAG, "Display over other apps permission granted.");
                        Toast.makeText(this, "Permission granted - functionality enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        Logger.logWarn(LOG_TAG, "Display over other apps permission was not granted.");
                        String errorMsg = getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal);
                        Logger.showToast(this, errorMsg, true);
                    }
                }
            }
        );
        
        // Set the status bar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.ultroid_primary_dark));
        
        // Initialize the bottom navigation view
        mBottomNavigationView = findViewById(R.id.bottom_navigation);
        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
        
        // Load the default fragment (Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        }

        // Start TermuxService first to ensure it's running
        startTermuxService();
        
        // Check for overlay permission needed for Android 10+ to start activities from background
        checkAndRequestOverlayPermission();
    }
    
    private void startTermuxService() {
        Intent serviceIntent = new Intent(this, TermuxService.class);
        try {
            Logger.logDebug(LOG_TAG, "Starting TermuxService");
            startService(serviceIntent);
            
            // Now bind to it
            bindToTermuxService();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to start TermuxService", e);
            Toast.makeText(this, "Failed to start Termux service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void bindToTermuxService() {
        if (mIsBound) {
            Logger.logDebug(LOG_TAG, "Already bound to TermuxService");
            return;
        }
        
        // Cancel any pending bind attempts
        if (mBindRetryRunnable != null) {
            mHandler.removeCallbacks(mBindRetryRunnable);
            mBindRetryRunnable = null;
        }
        
        Intent serviceIntent = new Intent(this, TermuxService.class);
        try {
            Logger.logDebug(LOG_TAG, "Binding to TermuxService (attempt " + (mBindRetryCount+1) + ")");
            // Use BIND_IMPORTANT to increase binding priority
            boolean bindResult = bindService(serviceIntent, this, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
            if (!bindResult) {
                Logger.logError(LOG_TAG, "Failed to bind to TermuxService");
                handleBindFailure();
            }
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Error binding to TermuxService", e);
            handleBindFailure();
        }
    }
    
    private void handleBindFailure() {
        mBindRetryCount++;
        if (mBindRetryCount < MAX_BIND_RETRIES) {
            Logger.logDebug(LOG_TAG, "Retrying TermuxService bind attempt " + mBindRetryCount + " in " + BIND_RETRY_DELAY_MS + "ms");
            // Try again after a short delay
            mBindRetryRunnable = this::bindToTermuxService;
            mHandler.postDelayed(mBindRetryRunnable, BIND_RETRY_DELAY_MS);
        } else {
            // Give up after max retries
            mBindRetryRunnable = null;
            Toast.makeText(this, "Failed to connect to Termux service after " + MAX_BIND_RETRIES + " attempts. Trying one last time.", 
                           Toast.LENGTH_LONG).show();
            
            // Make one final attempt to start the service directly and then bind
            try {
                Logger.logDebug(LOG_TAG, "Making final attempt to start and bind service");
                Intent serviceIntent = new Intent(this, TermuxService.class);
                startService(serviceIntent);
                // Brief delay to allow service to start
                mHandler.postDelayed(() -> {
                    mBindRetryCount = 0; // Reset counter
                    bindToTermuxService(); // Try binding again
                }, 3000);
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Final service start attempt failed", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure we're bound to the service
        if (!mIsBound) {
            bindToTermuxService();
        }
        
        // Check overlay permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Logger.logDebug(LOG_TAG, "Overlay permission still not granted onResume.");
            Toast.makeText(this, 
                     "Display over other apps permission is needed for proper functionality", 
                     Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any pending bind attempts
        if (mBindRetryRunnable != null) {
            mHandler.removeCallbacks(mBindRetryRunnable);
            mBindRetryRunnable = null;
        }
        
        // Unbind from service to prevent leaks
        if (mIsBound) {
            try {
                unbindService(this);
                mIsBound = false;
            } catch (Exception e) {
                // Just log any unbind errors
                Logger.logDebug(LOG_TAG, "Error unbinding from service: " + e.getMessage());
            }
        }
    }

    private void checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Logger.logInfo(LOG_TAG, "Requesting permission to display over other apps.");
            Toast.makeText(this, 
                         "Please grant Display over other apps permission for proper operation", 
                         Toast.LENGTH_LONG).show();
                         
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open overlay permission settings", e);
                // Show a toast explaining the user needs to grant it manually
                String errorMsg = getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal) +
                                  " Please grant it manually from app settings.";
                Logger.showToast(this, errorMsg, true);
            }
        } else {
            Logger.logDebug(LOG_TAG, "Overlay permission already granted or not required.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Logger.logInfo(LOG_TAG, "Display over other apps permission granted.");
                    // Permission granted, you can now proceed with features that need it.
                    Toast.makeText(this, "Permission granted - functionality enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Logger.logWarn(LOG_TAG, "Display over other apps permission was not granted.");
                    // Permission denied, inform user or disable features
                    String errorMsg = getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal);
                    Logger.showToast(this, errorMsg, true);
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        
        // Handle navigation item clicks
        int id = item.getItemId();
        if (id == R.id.navigation_home) {
            fragment = new HomeFragment();
        } else if (id == R.id.navigation_directory) {
            fragment = new DirectoryFragment();
        } else if (id == R.id.navigation_configure) {
            fragment = new ConfigureFragment();
        } else if (id == R.id.navigation_about) {
            fragment = new LinksFragment();
        }
        
        // Load the selected fragment
        return loadFragment(fragment);
    }
    
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Logger.logDebug(LOG_TAG, "TermuxService connected");
        TermuxService.LocalBinder binder = (TermuxService.LocalBinder) service;
        mTermuxService = binder.service;
        mIsBound = true;
        mBindRetryCount = 0; // Reset retry count on successful connection
        
        // Start a default session if needed
        if (mTermuxService != null && mTermuxService.isTermuxSessionsEmpty()) {
            Logger.logDebug(LOG_TAG, "Creating default background session");
            mTermuxService.createTermuxSession(null, null, null, 
                TermuxConstants.TERMUX_HOME_DIR_PATH, false, "UltroidSession");
        }
        
        // Notify current fragment about service connection
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            Logger.logDebug(LOG_TAG, "Notifying HomeFragment of service connection");
            ((HomeFragment) currentFragment).ensureTermuxSession();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "TermuxService disconnected");
        mTermuxService = null;
        mIsBound = false;
        
        // Attempt to rebind if unexpectedly disconnected and activity is still running
        if (!isFinishing() && !isDestroyed()) {
            bindToTermuxService();
        }
    }

    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public boolean isTermuxServiceBound() {
        return mIsBound;
    }

    public void openTermuxActivity() {
        Intent intent = new Intent(this, TermuxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Error starting TermuxActivity", e);
            Toast.makeText(this, "Error opening Termux: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 