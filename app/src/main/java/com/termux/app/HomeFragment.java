package com.termux.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.termux.R;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.ExecutionCommand.ExecutionState;
import com.termux.shared.shell.command.runner.app.AppShell;
import com.termux.shared.errors.Errno;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private TextView mStatusText;
    private MaterialButton mBtnStartSetup;
    private Toolbar mToolbar;
    private LinearLayout mSetupContainer;
    private LinearLayout mDeploymentContainer;
    private UltroidDeploymentActivity mActivity;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private TextView mUltroidTitle;
    private TextView mUltroidSubtitle;

    // Logs related views
    private FloatingActionButton mFabShowLogs;
    private ImageButton mBtnClearLogs;
    private NestedScrollView mLogsBottomSheet;
    private TextView mLogsText;
    private BottomSheetBehavior<NestedScrollView> mBottomSheetBehavior;
    private StringBuilder mLogsBuilder = new StringBuilder();

    private static final String LOG_TAG = "HomeFragment";
    private static final String ULTROID_DIR = "Ultroid";
    private static final int CROSSFADE_DURATION = 300;

    private Queue<CommandStep> commandQueue = new LinkedList<>();
    private boolean isExecutingQueue = false;

    private MaterialButton mBtnStartUltroid;
    private AppShell ultroidAppShell = null;
    private boolean isUltroidRunning = false;

    private static class CommandStep {
        String statusMessage;
        String command;
        String successLogMessage;

        CommandStep(String statusMessage, String command, String successLogMessage) {
            this.statusMessage = statusMessage;
            this.command = command;
            this.successLogMessage = successLogMessage;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof UltroidDeploymentActivity) {
            mActivity = (UltroidDeploymentActivity) context;
        } else {
            throw new IllegalStateException("HomeFragment must be attached to UltroidDeploymentActivity");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mStatusText = view.findViewById(R.id.status_text);
        mSetupContainer = view.findViewById(R.id.setup_container);
        mDeploymentContainer = view.findViewById(R.id.deployment_container);
        mBtnStartSetup = view.findViewById(R.id.btn_start_setup);
        mUltroidTitle = view.findViewById(R.id.ultroid_title);
        mUltroidSubtitle = view.findViewById(R.id.ultroid_subtitle);

        mFabShowLogs = view.findViewById(R.id.fab_show_logs);
        mBtnClearLogs = view.findViewById(R.id.btn_clear_logs);
        mLogsBottomSheet = view.findViewById(R.id.logs_bottom_sheet);

        mLogsText = view.findViewById(R.id.logs_text);
        mLogsText.setTextIsSelectable(true);

        Button mBtnCopyLogs = view.findViewById(R.id.btn_copy_logs);
        if (mBtnCopyLogs != null) {
            mBtnCopyLogs.setOnClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Ultroid Logs", mLogsBuilder.toString().replace("\\n", "\n"));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
            });
        }

        mBtnStartSetup.setOnClickListener(this);
        mFabShowLogs.setOnClickListener(this);
        mBtnClearLogs.setOnClickListener(this);

        // Add listener for Update Env button
        MaterialButton btnUpdateEnv = view.findViewById(R.id.btn_update_env);
        if (btnUpdateEnv != null) {
            btnUpdateEnv.setOnClickListener(v -> openConfigureFragment());
        }

        // Add listener for Open Termux button
        MaterialButton btnOpenTermux = view.findViewById(R.id.btn_open_termux);
        if (btnOpenTermux != null) {
            btnOpenTermux.setOnClickListener(v -> openTermuxActivity());
        }

        mBtnStartUltroid = view.findViewById(R.id.btn_start_ultroid);
        if (mBtnStartUltroid != null) {
            mBtnStartUltroid.setOnClickListener(v -> {
                if (!isUltroidRunning) {
                    startUltroid();
                } else {
                    stopUltroid();
                }
            });
        }

        // Add listener for Uninstall Ultroid button
        MaterialButton btnUninstallUltroid = view.findViewById(R.id.btn_uninstall_ultroid);
        if (btnUninstallUltroid != null) {
            btnUninstallUltroid.setOnClickListener(v -> uninstallUltroid());
        }

        mFabShowLogs.setVisibility(View.GONE); // Initially hidden

        setupBottomSheet();
        checkUltroidInstallation();
        
        // Set Poppins Bold font
        Typeface poppinsBold = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold);
        Typeface poppinsRegular = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold);
        mStatusText.setTypeface(poppinsBold);
        mBtnStartSetup.setTypeface(poppinsBold);
        mLogsText.setTypeface(poppinsBold);
        if (mUltroidTitle != null) mUltroidTitle.setTypeface(poppinsBold);
        if (mUltroidSubtitle != null) mUltroidSubtitle.setTypeface(poppinsRegular);

        return view;
    }

    /**
     * Uninstall Ultroid by removing the Ultroid directory from $HOME.
     * This is triggered by the Uninstall Ultroid button.
     */
    private void uninstallUltroid() {
        if (isExecutingQueue) {
            appendToLogs("Cannot uninstall while deployment is in progress.");
            Toast.makeText(getContext(), "Cannot uninstall during deployment.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isUltroidRunning) {
            appendToLogs("Cannot uninstall while Ultroid is running.");
            Toast.makeText(getContext(), "Stop Ultroid before uninstalling.", Toast.LENGTH_SHORT).show();
            return;
        }
        appendToLogs("Uninstalling Ultroid: removing Ultroid directory...");
        updateStatus("Uninstalling Ultroid...");

        // Run rm -rf Ultroid in Termux $HOME
        if (mActivity == null || !mActivity.isTermuxServiceBound()) {
            appendToLogs("Error: Termux service not available for uninstall.");
            Toast.makeText(getContext(), "Termux service not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        TermuxService termuxService = mActivity.getTermuxService();
        if (termuxService == null) {
            appendToLogs("Error: TermuxService is null for uninstall.");
            Toast.makeText(getContext(), "Termux service not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        String command = "rm -rf Ultroid";
        AppShell shell = termuxService.createTermuxTask(
            TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/sh",
            new String[]{"-c", command},
            null,
            TermuxConstants.TERMUX_HOME_DIR_PATH
        );
        if (shell == null) {
            appendToLogs("Failed to start shell for uninstall.");
            Toast.makeText(getContext(), "Failed to start uninstall.", Toast.LENGTH_SHORT).show();
            return;
        }
        final ExecutionCommand cmd = shell.getExecutionCommand();
        new Thread(() -> {
            int elapsed = 0;
            while (!cmd.hasExecuted() && !cmd.isStateFailed() && elapsed < 60_000) {
                try { Thread.sleep(500); elapsed += 500; } catch (InterruptedException ignored) {}
                if (elapsed % 5000 == 0 && elapsed > 0) {
                    mHandler.post(() -> appendToLogs("Still removing Ultroid directory..."));
                }
            }
            mHandler.post(() -> {
                String stdout = (cmd.resultData != null && cmd.resultData.stdout != null) ? cmd.resultData.stdout.toString().trim() : "";
                String stderr = (cmd.resultData != null && cmd.resultData.stderr != null) ? cmd.resultData.stderr.toString().trim() : "";
                int exitCode = (cmd.resultData != null && cmd.resultData.exitCode != null) ? cmd.resultData.exitCode : -1;
                
                if (!stdout.isEmpty()) appendToLogs("Uninstall STDOUT: " + stdout);
                if (!stderr.isEmpty()) appendToLogs("Uninstall STDERR: " + stderr);
                
                if (exitCode == 0) {
                    appendToLogs("Ultroid directory removed successfully.");
                    updateStatus("Ultroid uninstalled.");
                    checkUltroidInstallation();
                    Toast.makeText(getContext(), "Ultroid uninstalled.", Toast.LENGTH_SHORT).show();
                } else {
                    appendToLogs("Failed to remove Ultroid directory. Exit code: " + exitCode);
                    updateStatus("Uninstall failed.");
                    Toast.makeText(getContext(), "Failed to uninstall Ultroid.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void setupBottomSheet() {
        mBottomSheetBehavior = BottomSheetBehavior.from(mLogsBottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        
        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dimBackground(false);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Dimming effect can be adjusted or removed if not desired for FAB setup
                // dimBackground(true, slideOffset);
            }
        });
    }

    private void dimBackground(boolean dim) {
         // Get the root view of the fragment
        View rootView = getView();
        if (rootView == null) return;

        // Find the FrameLayout that contains the setup and deployment containers
        FrameLayout mainContentContainer = rootView.findViewById(R.id.main_content_frame); 
        
        if (mainContentContainer != null) {
            mainContentContainer.setAlpha(dim ? 0.5f : 1f);
        } else {
            // Fallback or error logging if main_content_frame is not found
            Logger.logError(LOG_TAG, "main_content_frame not found for dimBackground");
            // As a last resort, try to dim the deployment or setup container directly if visible
            if (mDeploymentContainer != null && mDeploymentContainer.getVisibility() == View.VISIBLE) {
                mDeploymentContainer.setAlpha(dim ? 0.5f : 1f);
            } else if (mSetupContainer != null && mSetupContainer.getVisibility() == View.VISIBLE) {
                mSetupContainer.setAlpha(dim ? 0.5f : 1f);
            }
        }
    }


    private void toggleLogs() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            // dimBackground(true); // Optional: dim background when logs are shown
        } else {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            // dimBackground(false); // Optional: undim background when logs are hidden
        }
    }

    private void clearLogs() {
        mLogsBuilder.setLength(0);
        mLogsText.setText("");
        appendToLogs("Log cleared.");
    }

    private void appendToLogs(String text) {
        if (text == null || mLogsText == null || mLogsBuilder == null || mLogsBottomSheet == null) return;
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        mLogsBuilder.append(timestamp).append(": ").append(text).append("\n");
        // Ensure UI updates are on the main thread
        mHandler.post(() -> {
            // Replace literal \n with actual newlines for display
            mLogsText.setText(mLogsBuilder.toString().replace("\\n", "\n"));
            mLogsBottomSheet.post(() -> mLogsBottomSheet.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ensureTermuxSession();
        
        // Re-check state in onResume as well, in case view is recreated
        checkUltroidInstallation(); 
    }

    @Override
    public void onPause() {
        super.onPause();
        // if (commandResultReceiver != null && getContext() != null) {
        //     getContext().unregisterReceiver(commandResultReceiver);
        //     commandResultReceiver = null;
        // }
    }

    public void ensureTermuxSession() {
        if (mActivity != null && mActivity.isTermuxServiceBound()) {
            TermuxService termuxService = mActivity.getTermuxService();
            if (termuxService != null && termuxService.isTermuxSessionsEmpty()) {
                Logger.logDebug(LOG_TAG, "No active session, creating a new one for Ultroid.");
                appendToLogs("Creating new Termux session for Ultroid.");
                termuxService.createTermuxSession(null, null, null, 
                    TermuxConstants.TERMUX_HOME_DIR_PATH, false, "UltroidDeploymentSession");
            }
        } else {
             mHandler.postDelayed(this::ensureTermuxSession, 1000); 
             Logger.logDebug(LOG_TAG, "TermuxService not bound yet, retrying session creation for Ultroid.");
             appendToLogs("Waiting for Termux service to connect for Ultroid...");
        }
    }

    private void checkUltroidInstallation() {
        if (getContext() == null) return;
        File ultroidDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR);
        View actionButtons = getView() != null ? getView().findViewById(R.id.ultroid_action_buttons) : null;
        if (ultroidDir.exists() && ultroidDir.isDirectory()) {
            showDeploymentState();
            updateStatus("✓ Ultroid is ready!");
            appendToLogs("Ultroid directory found: " + ultroidDir.getAbsolutePath());
            mBtnStartSetup.setEnabled(true);
            if (actionButtons != null) actionButtons.setVisibility(View.VISIBLE);
        } else {
            showSetupState();
            updateStatus("Ultroid not found. Please start the initial setup.");
            if (actionButtons != null) actionButtons.setVisibility(View.GONE);
        }
    }

    private void showSetupState() {
        if (mToolbar != null) {
            mToolbar.getMenu().clear();
            mToolbar.setOnMenuItemClickListener(null);
        }
        mSetupContainer.setAlpha(0f);
        mSetupContainer.setVisibility(View.VISIBLE);
        mDeploymentContainer.setVisibility(View.GONE);
        mFabShowLogs.setVisibility(View.GONE);

        mSetupContainer.animate()
            .alpha(1f)
            .setDuration(CROSSFADE_DURATION)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    private void showDeploymentState() {
        if (mToolbar != null) {
            mToolbar.getMenu().clear(); 
            mToolbar.inflateMenu(R.menu.menu_home_deployment);
            mToolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_open_termux_activity) {
                    if (getContext() != null) {
                        Intent intent = new Intent(getContext(), TermuxActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        try {
                            getContext().startActivity(intent);
                        } catch (Exception e) {
                            appendToLogs("Error opening Termux: " + e.getMessage());
                            Logger.logStackTraceWithMessage(LOG_TAG, "Error starting TermuxActivity", e);
                        }
                    }
                    return true;
                }
                return false;
            });
        }

        // Show success state
        if (mDeploymentContainer != null) {
            mDeploymentContainer.setAlpha(0f);
            mDeploymentContainer.setVisibility(View.VISIBLE);
        }
        
        if (mSetupContainer != null) {
            mSetupContainer.setVisibility(View.GONE);
        }
        
        if (mFabShowLogs != null) {
            mFabShowLogs.setVisibility(View.VISIBLE);
            mFabShowLogs.setAlpha(0f);
        }

        View view = getView();
        if (view != null) {
            // Find and update the status text with checkmark
            TextView statusText = view.findViewById(R.id.status_text);
            if (statusText != null) {
                statusText.setText("✓ Ultroid is ready!");
                statusText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24); // Specify SP unit
            }

            // Find and update the status icon
            ImageView statusIcon = view.findViewById(R.id.status_icon);
            if (statusIcon != null) {
                statusIcon.setImageResource(android.R.drawable.ic_dialog_info);
            }

            // Hide the deployment status card if not actively deploying
            View statusCard = view.findViewById(R.id.status_card);
            if (statusCard != null && !isExecutingQueue) {
                statusCard.setVisibility(View.GONE);
            }
        }

        if (mDeploymentContainer != null) {
            mDeploymentContainer.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
        
        if (mFabShowLogs != null) {
            mFabShowLogs.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        if (id == R.id.fab_show_logs) {
            toggleLogs();
        } else if (id == R.id.btn_clear_logs) {
            clearLogs();
        } else if (id == R.id.btn_start_setup) {
            startAutomatedDeployment();
        }
    }

    private void startAutomatedDeployment() {
        if (isExecutingQueue) {
            appendToLogs("Deployment already in progress.");
            if (getContext() != null) Toast.makeText(getContext(), "Deployment already in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mActivity == null || !mActivity.isTermuxServiceBound()) {
            appendToLogs("Error: Termux service not available for deployment. Please try again.");
            if (getContext() != null) Toast.makeText(getContext(), "Termux service not available for deployment.", Toast.LENGTH_SHORT).show();
            return;
        }

        showDeploymentState(); 
        mBtnStartSetup.setEnabled(false); 
        appendToLogs("Starting automated Ultroid deployment sequence...");
        updateStatus("Deployment starting... See logs for details.");

        commandQueue.clear();
commandQueue.add(new CommandStep("Cleaning package cache...", "apt clean", "Package cache cleaned."));
commandQueue.add(new CommandStep("Updating package lists...", "apt update && apt-get update --fix-missing", "Package lists updated."));
commandQueue.add(new CommandStep("Installing tur-repo...", "DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold tur-repo", "tur-repo installed."));
commandQueue.add(new CommandStep("Installing dependencies...", "DEBIAN_FRONTEND=noninteractive apt install -y -o Dpkg::Options::=--force-confold git redis coreutils libexpat openssl libffi", "Dependencies installed (including libexpat, openssl, libffi)."));
commandQueue.add(new CommandStep("Installing Python 3.10...", "DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold python3.10", "Python 3.10 installed."));
commandQueue.add(new CommandStep("Ensuring NDK/Clang environment...", "DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold clang ndk-sysroot libc++", "NDK/Clang environment updated."));
        commandQueue.add(new CommandStep("Fixing Python sysconfigdata...", 
            "_file=\"$(find $PREFIX/lib/python3* -name \"_sysconfigdata*.py\" | head -n 1)\"; " +
            "if [ ! -z \"$_file\" ]; then " +
            "rm -rf $PREFIX/lib/python3*/__pycache__; " +
            "cp \"$_file\" \"$_file.backup\"; " +
            "sed -i 's|-fno-openmp-implicit-rpath||g' \"$_file\"; " +
            "echo \"Fixed Python sysconfigdata file: $_file\"; " +
            "else echo \"Python sysconfigdata file not found.\"; fi", 
            "Python sysconfigdata fixed for proper package compilation."));
        commandQueue.add(new CommandStep("Checking git installation...", "git --version", "Git is installed and available."));
        commandQueue.add(new CommandStep("Cloning/Pulling Ultroid repository...", "cd ~/ && (git clone https://github.com/TeamUltroid/Ultroid.git Ultroid || (cd Ultroid && git reset --hard && git pull))", "Ultroid repository cloned/updated."));
        commandQueue.add(new CommandStep("Installing Python requirements for Ultroid...", "cd ~/Ultroid && pip install -r requirements.txt", "Python requirements installed."));
        
        isExecutingQueue = true;
        executeNextCommand();
    }

    private void executeNextCommand() {
        if (commandQueue.isEmpty()) {
            isExecutingQueue = false;
            if (mBtnStartSetup != null) mBtnStartSetup.setEnabled(true); 
            updateStatus("All deployment steps processed!");
            appendToLogs("--- Automated deployment sequence complete. ---");
            if (getContext() != null) Toast.makeText(getContext(), "Deployment sequence finished!", Toast.LENGTH_LONG).show();
            return;
        }

        CommandStep currentStep = commandQueue.poll();
        if (currentStep == null) { 
            executeNextCommand(); // Should not happen with isEmpty check, but good practice
            return;
        }

        updateStatus(currentStep.statusMessage);
        appendToLogs("Next step: " + currentStep.statusMessage);
        
        // Execute command immediately
        executeCommand(currentStep);
    }

    private void executeCommand(CommandStep currentStep) {
        if (mActivity == null || getContext() == null) {
            appendToLogs("Error: Activity or Context is null for command: " + currentStep.command);
            isExecutingQueue = false;
            if (mBtnStartSetup != null) mBtnStartSetup.setEnabled(true);
            // Try to proceed to next command or stop queue
             mHandler.postDelayed(this::executeNextCommand, 1000);
            return;
        }

        TermuxService termuxService = mActivity.getTermuxService();
        if (termuxService == null) {
            appendToLogs("Error: TermuxService is null for command: " + currentStep.command);
            isExecutingQueue = false;
            if (mBtnStartSetup != null) mBtnStartSetup.setEnabled(true);
            // Try to proceed to next command or stop queue
            mHandler.postDelayed(this::executeNextCommand, 1000);
            return;
        }

        appendToLogs("Executing: " + currentStep.command);
        Logger.logDebug(LOG_TAG, "Executing command: `" + currentStep.command + "` (Label: " + currentStep.statusMessage + ")");
        
        final AppShell appShell;
        try {
            // Use a subshell to handle complex commands and ensure proper environment
            String wrappedCommand = "sh -c '" + currentStep.command.replace("'", "'\''") + "'";
            appShell = termuxService.createTermuxTask(
                TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/sh", // login shell to source profile if needed
                new String[]{"-c", currentStep.command}, // Pass command to sh -c
                null, // stdin
                TermuxConstants.TERMUX_HOME_DIR_PATH // working directory
            );
            
            if (appShell == null) {
                appendToLogs("Error: Failed to create AppShell task for: " + currentStep.statusMessage);
                mHandler.postDelayed(this::executeNextCommand, 1000);
                return;
            }
        } catch (Exception e) {
            appendToLogs("Exception creating AppShell task for '" + currentStep.statusMessage + "': " + e.getMessage());
            Logger.logStackTraceWithMessage(LOG_TAG, "AppShell creation exception for: " + currentStep.command, e);
            mHandler.postDelayed(this::executeNextCommand, 1000);
            return;
        }
            
        final ExecutionCommand cmd = appShell.getExecutionCommand();
        cmd.commandLabel = currentStep.statusMessage; 

        Logger.logDebug(LOG_TAG, "AppShell task created for '" + cmd.commandLabel + "'. PID: " + cmd.mPid + ". Monitoring state...");

        new Thread(() -> {
            int timeoutSeconds = 300; // 5 minutes for most steps
            if (currentStep.command.contains("pip install") || currentStep.command.contains("pkg update")) {
                timeoutSeconds = 600; // 10 minutes for package installations
            }

            int elapsedTime = 0; // in 100ms intervals
            boolean isConcluded = false;
            
            // Track last known output lengths for real-time output
            int lastStdoutLength = 0;
            int lastStderrLength = 0;

            while (!isConcluded && elapsedTime < timeoutSeconds * 10) { 
                try {
                    Thread.sleep(100);
                    elapsedTime++;

                    synchronized (cmd) { 
                        isConcluded = cmd.hasExecuted() || cmd.isStateFailed();
                        
                        // Check for real-time output
                        if (cmd.resultData != null) {
                            // Check for new stdout content
                            if (cmd.resultData.stdout != null) {
                                String stdout = cmd.resultData.stdout.toString();
                                if (stdout.length() > lastStdoutLength) {
                                    final String newOutput = stdout.substring(lastStdoutLength);
                                    lastStdoutLength = stdout.length();
                                    if (!newOutput.trim().isEmpty()) {
                                        mHandler.post(() -> appendToLogs("LIVE [" + cmd.commandLabel + "] stdout: " + newOutput.trim()));
                                    }
                                }
                            }
                            
                            // Check for new stderr content
                            if (cmd.resultData.stderr != null) {
                                String stderr = cmd.resultData.stderr.toString();
                                if (stderr.length() > lastStderrLength) {
                                    final String newOutput = stderr.substring(lastStderrLength);
                                    lastStderrLength = stderr.length();
                                    if (!newOutput.trim().isEmpty()) {
                                        mHandler.post(() -> appendToLogs("LIVE [" + cmd.commandLabel + "] stderr: " + newOutput.trim()));
                                    }
                                }
                            }
                        }
                    }

                    if (elapsedTime % 50 == 0) { // Log progress every 5 seconds
                        final int seconds = elapsedTime / 10;
                        if (!isConcluded) { 
                            final String progressMsg = "'" + cmd.commandLabel + "' running... (" + seconds + "s / " + timeoutSeconds + "s)";
                            mHandler.post(() -> appendToLogs(progressMsg));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    com.termux.shared.logger.Logger.logWarn(LOG_TAG, "Monitoring for '" + cmd.commandLabel + "' was interrupted.");
                    isConcluded = true; 
                }
            }

            final boolean timedOut = !isConcluded;

            mHandler.post(() -> {
                synchronized (cmd) { 
                    if (timedOut) {
                        appendToLogs("Timeout: Command '" + cmd.commandLabel + "' exceeded " + "s.");
                        if (cmd.isExecuting()) {
                            if (getContext() != null) {
                                com.termux.shared.logger.Logger.logWarn(LOG_TAG, "Attempting to kill timed-out command: " + cmd.commandLabel);
                                appShell.killIfExecuting(getContext(), true); 
                            } else {
                                com.termux.shared.logger.Logger.logError(LOG_TAG, "Context null, cannot kill timed-out command: " + cmd.commandLabel);
                            }
                            if (cmd.resultData.exitCode == null) cmd.resultData.exitCode = Errno.ERRNO_FAILED.getCode(); 
                            if (!cmd.isStateFailed() && cmd.isExecuting()) {
                                cmd.setState(ExecutionState.FAILED);
                            }
                        }
                    }

                    String stdout = (cmd.resultData != null && cmd.resultData.stdout != null) ? cmd.resultData.stdout.toString().trim() : "";
                    String stderr = (cmd.resultData != null && cmd.resultData.stderr != null) ? cmd.resultData.stderr.toString().trim() : "";
                    int exitCode = (cmd.resultData != null && cmd.resultData.exitCode != null) ? cmd.resultData.exitCode : -1;

                    Logger.logDebug(LOG_TAG, "Results for '" + cmd.commandLabel + "': State=" + (cmd.isStateFailed() ? ExecutionState.FAILED.getName() : (cmd.hasExecuted() ? ExecutionState.EXECUTED.getName() : ExecutionState.EXECUTING.getName())) + ", ExitCode=" + exitCode + 
                                             ", StdoutLen=" + stdout.length() + ", StderrLen=" + stderr.length());

                    if (!stdout.isEmpty()) {
                        appendToLogs("STDOUT [" + cmd.commandLabel + "]:\n" + stdout);
                    }
                    if (!stderr.isEmpty()) {
                        appendToLogs("STDERR [" + cmd.commandLabel + "]:\n" + stderr);
                    }
                    appendToLogs("Exit Code [" + cmd.commandLabel + "]: " + exitCode);

                    if (cmd.hasExecuted() && !cmd.isStateFailed() && exitCode == 0) {
                        appendToLogs(currentStep.successLogMessage != null ? currentStep.successLogMessage 
                                                                        : "Step '" + cmd.commandLabel + "' completed successfully.");
                    } else {
                        appendToLogs("Step '" + cmd.commandLabel + "' finished with issues. State: " + (cmd.isStateFailed() ? ExecutionState.FAILED.getName() : (cmd.hasExecuted() ? ExecutionState.EXECUTED.getName() : ExecutionState.EXECUTING.getName())) + ", Exit Code: " + exitCode);
                    }
                } 
                mHandler.postDelayed(HomeFragment.this::executeNextCommand, 500);
            });
        }).start();
    }


    private void updateStatus(String status) {
        if (mStatusText != null) {
            mStatusText.setText(status);
            // Optional: Add animation for status updates
            // mStatusText.setAlpha(0f);
            // mStatusText.animate().alpha(1f).setDuration(200).start();
        }
        Logger.logDebug(LOG_TAG, "Status Updated: " + status);
    }
    
    private void openConfigureFragment() {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.main_content_frame, new ConfigureFragment())
            .addToBackStack(null)
            .commit();
    }

    private void startUltroid() {
        if (isUltroidRunning) return;
        File ultroidDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR);
        boolean needsClone = false;
        
        if (!ultroidDir.exists()) {
            appendToLogs("Ultroid directory not found. Will attempt to clone repository...");
            ultroidDir.mkdirs();
            needsClone = true;
        } else if (ultroidDir.list() == null || ultroidDir.list().length == 0) {
            appendToLogs("Ultroid directory exists but is empty. Will attempt to clone repository...");
            needsClone = true;
        }
        
        appendToLogs("Starting Ultroid: Installing requirements and launching pyUltroid...");
        updateStartButtonState(true);
        isUltroidRunning = true;
        if (mBtnStartUltroid != null) {
            mBtnStartUltroid.setEnabled(false);
        }
        
        // Prepare the command - if directory doesn't exist or is empty, clone first
        StringBuilder commandBuilder = new StringBuilder();
        
        // Install tur-repo before python
        commandBuilder.append("DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold tur-repo && ");
        // First install Python 3.10 specifically and other essential packages, regardless of clone status
        commandBuilder.append("DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold python3.10 && ");
        // Ensuring NDK/Clang environment
        commandBuilder.append("DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold clang ndk-sysroot libc++ && ");
        
        // Then apply Python sysconfigdata fix to prevent pip installation issues
        commandBuilder.append("_file=\"$(find $PREFIX/lib/python3* -name \"_sysconfigdata*.py\" | head -n 1)\"; ");
        commandBuilder.append("if [ ! -z \"$_file\" ]; then ");
        commandBuilder.append("rm -rf $PREFIX/lib/python3*/__pycache__; ");
        commandBuilder.append("cp \"$_file\" \"$_file.backup\"; ");
        commandBuilder.append("sed -i 's|-fno-openmp-implicit-rpath||g' \"$_file\"; ");
        commandBuilder.append("echo \"Fixed Python sysconfigdata file: $_file\"; ");
        commandBuilder.append("else echo \"Python sysconfigdata file not found.\"; fi && ");
        
        if (needsClone) {
            commandBuilder.append("DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold git python3-pip redis coreutils libexpat openssl libffi && ");
            commandBuilder.append("cd ~/ && git clone https://github.com/TeamUltroid/Ultroid.git Ultroid && ");
        }
        
        commandBuilder.append("cd ~/Ultroid && pip3 install -r requirements.txt && python3 -m pyUltroid");
        String command = commandBuilder.toString();
        
        TermuxService termuxService = mActivity.getTermuxService();
        ultroidAppShell = termuxService.createTermuxTask(
            TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/sh",
            new String[]{"-c", command},
            null,
            ultroidDir.getAbsolutePath()
        );
        if (ultroidAppShell == null) {
            appendToLogs("Failed to start Ultroid process.");
            updateStartButtonState(false);
            isUltroidRunning = false;
            if (mBtnStartUltroid != null) mBtnStartUltroid.setEnabled(true);
            return;
        }
        final ExecutionCommand cmd = ultroidAppShell.getExecutionCommand();
        new Thread(() -> {
            int elapsed = 0;
            boolean pipNotFoundHandled = false;
            
            // Track last known output lengths for real-time output
            int lastStdoutLength = 0;
            int lastStderrLength = 0;
            
            while (!cmd.hasExecuted() && !cmd.isStateFailed()) {
                try { Thread.sleep(500); elapsed += 500; } catch (InterruptedException ignored) {}
                
                // Real-time output monitoring
                synchronized (cmd) {
                    if (cmd.resultData != null) {
                        // Check for new stdout content
                        if (cmd.resultData.stdout != null) {
                            String stdout = cmd.resultData.stdout.toString();
                            if (stdout.length() > lastStdoutLength) {
                                final String newOutput = stdout.substring(lastStdoutLength);
                                lastStdoutLength = stdout.length();
                                if (!newOutput.trim().isEmpty()) {
                                    mHandler.post(() -> appendToLogs("LIVE Ultroid stdout: " + newOutput.trim()));
                                }
                            }
                        }
                        
                        // Check for new stderr content
                        if (cmd.resultData.stderr != null) {
                            String stderr = cmd.resultData.stderr.toString();
                            if (stderr.length() > lastStderrLength) {
                                final String newOutput = stderr.substring(lastStderrLength);
                                lastStderrLength = stderr.length();
                                if (!newOutput.trim().isEmpty()) {
                                    mHandler.post(() -> appendToLogs("LIVE Ultroid stderr: " + newOutput.trim()));
                                }
                                
                                // Check for pip3 not found error
                                if (!pipNotFoundHandled && stderr.contains("pip3: not found")) {
                                    pipNotFoundHandled = true;
                                final String installCmd = "DEBIAN_FRONTEND=noninteractive pkg install -y -o Dpkg::Options::=--force-confold python3.10 python3-pip openssl libffi libexpat";
                                    mHandler.post(() -> appendToLogs("ERROR: pip3 not found. Please ensure Python and pip are installed.\nAttempting to install pip3 automatically..."));
                                    // Attempt to install pip3 automatically
                                    try {
                                        Process installPip = Runtime.getRuntime().exec(new String[]{"sh", "-c", installCmd});
                                        int result = installPip.waitFor();
                                        final int resultFinal = result;
                                        mHandler.post(() -> appendToLogs("Python 3.10 and pip3 installation attempted (no prompts). Exit code: " + resultFinal + ". Please retry starting Ultroid if the problem persists."));
                                    } catch (Exception e) {
                                        final String errMsg = e.getMessage();
                                        mHandler.post(() -> appendToLogs("Automatic pip3/openssl installation failed: " + errMsg));
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (elapsed % 5000 == 0) {
                    final int seconds = elapsed/1000;
                    mHandler.post(() -> appendToLogs("Ultroid running... (elapsed: " + seconds + " seconds)"));
                }
            }
            mHandler.post(() -> {
                String stdout = (cmd.resultData != null && cmd.resultData.stdout != null) ? cmd.resultData.stdout.toString().trim() : "";
                String stderr = (cmd.resultData != null && cmd.resultData.stderr != null) ? cmd.resultData.stderr.toString().trim() : "";
                int exitCode = (cmd.resultData != null && cmd.resultData.exitCode != null) ? cmd.resultData.exitCode : -1;
                if (!stdout.isEmpty()) appendToLogs("Ultroid STDOUT:\n" + stdout);
                if (!stderr.isEmpty()) appendToLogs("Ultroid STDERR:\n" + stderr);
                if (stderr.contains("pip3: not found")) {
                    appendToLogs("ERROR: pip3 is still not found after attempted installation. Please install pip3 manually using 'pkg install python python3-pip' in Termux, then try again.");
                }
                appendToLogs("Ultroid process exited with code: " + exitCode);
                isUltroidRunning = false;
                updateStartButtonState(false);
                if (mBtnStartUltroid != null) mBtnStartUltroid.setEnabled(true);
            });
        }).start();
    }

    private void stopUltroid() {
        if (!isUltroidRunning || ultroidAppShell == null) return;
        appendToLogs("Stopping Ultroid...");
        TermuxService termuxService = mActivity.getTermuxService();
        if (termuxService != null) {
            ultroidAppShell.killIfExecuting(getContext(), true);
        }
        isUltroidRunning = false;
        updateStartButtonState(false);
        appendToLogs("Ultroid stopped.");
    }

    private void updateStartButtonState(boolean running) {
        if (mBtnStartUltroid == null) return;
        if (running) {
            mBtnStartUltroid.setText("Stop Ultroid");
            mBtnStartUltroid.setIconResource(android.R.drawable.ic_media_pause);
        } else {
            mBtnStartUltroid.setText("Start Ultroid");
            mBtnStartUltroid.setIconResource(android.R.drawable.ic_media_play);
        }
    }

    private void openTermuxActivity() {
        if (mActivity != null) {
            mActivity.openTermuxActivity();
        }
    }
}