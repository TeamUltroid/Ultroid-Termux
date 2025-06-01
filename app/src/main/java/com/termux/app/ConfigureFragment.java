package com.termux.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.termux.R;

public class ConfigureFragment extends Fragment {

    private EditText mConfigTextArea;
    private CheckBox mCheckPython;
    private CheckBox mCheckRedis;
    private CheckBox mCheckFfmpeg;
    private CheckBox mCheckNodejs;
    private CheckBox mCheckGit;
    private Button mBtnSaveConfig;
    private Toolbar mToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configure, container, false);

        // Initialize toolbar
        mToolbar = view.findViewById(R.id.toolbar_configure);
        
        // Initialize views
        mConfigTextArea = view.findViewById(R.id.config_text_area);
        mCheckPython = view.findViewById(R.id.check_python);
        mCheckRedis = view.findViewById(R.id.check_redis);
        mCheckFfmpeg = view.findViewById(R.id.check_ffmpeg);
        mCheckNodejs = view.findViewById(R.id.check_nodejs);
        mCheckGit = view.findViewById(R.id.check_git);
        mBtnSaveConfig = view.findViewById(R.id.btn_save_config);

        // Set initial configuration text
        mConfigTextArea.setText("# Ultroid Environment Configuration\n" +
                "# Edit this file to configure your environment\n\n" +
                "# Python virtual environment\n" +
                "PYTHON_VENV=~/ultroid-env\n\n" +
                "# Redis configuration\n" +
                "REDIS_HOST=localhost\n" +
                "REDIS_PORT=6379\n" +
                "REDIS_PASSWORD=\n\n" +
                "# Ultroid repository\n" +
                "ULTROID_REPO=https://github.com/TeamUltroid/Ultroid.git\n" +
                "ULTROID_BRANCH=main\n\n" +
                "# Telegram API credentials\n" +
                "API_ID=\n" +
                "API_HASH=\n");

        // Set click listener for save button
        mBtnSaveConfig.setOnClickListener(v -> saveConfiguration());

        return view;
    }

    private void saveConfiguration() {
        // TODO: Implement actual saving of configuration
        StringBuilder status = new StringBuilder("Configuration saved with dependencies:\n");
        
        if (mCheckPython.isChecked()) {
            status.append("- Python 3.8+\n");
        }
        
        if (mCheckRedis.isChecked()) {
            status.append("- Redis Database\n");
        }
        
        if (mCheckFfmpeg.isChecked()) {
            status.append("- FFmpeg\n");
        }
        
        if (mCheckNodejs.isChecked()) {
            status.append("- NodeJS\n");
        }
        
        if (mCheckGit.isChecked()) {
            status.append("- Git\n");
        }
        
        Toast.makeText(getContext(), status.toString(), Toast.LENGTH_LONG).show();
    }
} 