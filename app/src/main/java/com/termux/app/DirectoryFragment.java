package com.termux.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryFragment extends Fragment {

    private ListView mDirectoryListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mEmptyView;
    private static final String LOG_TAG = "DirectoryFragment";
    private static final String ULTROID_DIR = "Ultroid";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mDirectoryListView = view.findViewById(R.id.directory_list_view);
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mEmptyView = view.findViewById(R.id.empty_view);

        mSwipeRefreshLayout.setOnRefreshListener(this::refreshDirectoryListing);

        // Set empty view for the ListView
        mDirectoryListView.setEmptyView(mEmptyView);

        refreshDirectoryListing();

        return view;
    }

    private void refreshDirectoryListing() {
        File ultroidDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR);
        List<String> fileList = new ArrayList<>();

        if (ultroidDir.exists() && ultroidDir.isDirectory()) {
            File[] files = ultroidDir.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    String prefix = file.isDirectory() ? "📁 " : "📄 ";
                    fileList.add(prefix + file.getName());
                }
            }
            mEmptyView.setText("No files found in Ultroid directory");
        } else {
            mEmptyView.setText("Ultroid directory not found");
            Logger.logDebug(LOG_TAG, "Ultroid directory not found at: " + ultroidDir.getAbsolutePath());
        }

        if (getContext() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, fileList);
            mDirectoryListView.setAdapter(adapter);
        }

        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDirectoryListing();
    }
} 