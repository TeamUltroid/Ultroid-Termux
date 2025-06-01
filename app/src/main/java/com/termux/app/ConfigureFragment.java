package com.termux.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.termux.R;
import com.termux.shared.termux.TermuxConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigureFragment extends Fragment {

    private EditText mConfigTextArea;
    private CheckBox mCheckPython;
    private CheckBox mCheckRedis;
    private CheckBox mCheckFfmpeg;
    private CheckBox mCheckNodejs;
    private CheckBox mCheckGit;
    private Button mBtnSaveConfig;
    private Toolbar mToolbar;
    private MaterialCardView mConfigCard;
    private LinearLayout mSetupFirstContainer;
    private TextView mSetupFirstText;
    private String mCurrentEnvContent = "";

    private static final String ULTROID_DIR = "Ultroid";
    private static final String ENV_FILE = ".env";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configure, container, false);

        // Initialize views
        mToolbar = view.findViewById(R.id.toolbar_configure);
        mConfigTextArea = view.findViewById(R.id.config_text_area);
        mBtnSaveConfig = view.findViewById(R.id.btn_save_config);
        mConfigCard = view.findViewById(R.id.config_card);
        mSetupFirstContainer = view.findViewById(R.id.setup_first_container);
        mSetupFirstText = view.findViewById(R.id.setup_first_text);

        // Set Poppins Bold font
        mSetupFirstText.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins_bold));

        // Add text change listener
        mConfigTextArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(mCurrentEnvContent)) {
                    mBtnSaveConfig.setVisibility(View.VISIBLE);
                } else {
                    mBtnSaveConfig.setVisibility(View.GONE);
                }
            }
        });

        // Set click listener for save button
        mBtnSaveConfig.setOnClickListener(v -> saveConfiguration());

        checkAndLoadConfiguration();

        return view;
    }

    private void checkAndLoadConfiguration() {
        File ultroidDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR);
        File envFile = new File(ultroidDir, ENV_FILE);

        if (!ultroidDir.exists() || !ultroidDir.isDirectory()) {
            // Show setup first message
            mConfigCard.setVisibility(View.GONE);
            mSetupFirstContainer.setVisibility(View.VISIBLE);
            mBtnSaveConfig.setVisibility(View.GONE);
        } else {
            mConfigCard.setVisibility(View.VISIBLE);
            mSetupFirstContainer.setVisibility(View.GONE);

            if (envFile.exists()) {
                // Load existing configuration
                try {
                    StringBuilder content = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new FileReader(envFile));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();
                    
                    mCurrentEnvContent = content.toString();
                    mConfigTextArea.setText(mCurrentEnvContent);
                    mBtnSaveConfig.setVisibility(View.GONE);
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error reading configuration: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                // Set default configuration
                mConfigTextArea.setText("# Ultroid Environment Configuration\n" +
                    "API_ID=\n" +
                    "API_HASH=\n" +
                    "SESSION=\n" +
                    "REDIS_URI=redis://localhost:6379/1\n" +
                    "REDIS_PASSWORD=\n" +
                    "LOG_CHANNEL=\n" +
                    "BOT_TOKEN=");
                mBtnSaveConfig.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveConfiguration() {
        File ultroidDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR);
        File envFile = new File(ultroidDir, ENV_FILE);

        try {
            FileWriter writer = new FileWriter(envFile);
            writer.write(mConfigTextArea.getText().toString());
            writer.close();
            
            mCurrentEnvContent = mConfigTextArea.getText().toString();
            mBtnSaveConfig.setVisibility(View.GONE);
            
            Toast.makeText(getContext(), "Configuration saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error saving configuration: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 