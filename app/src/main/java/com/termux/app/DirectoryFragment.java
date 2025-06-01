package com.termux.app;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.app.adapter.FileAdapter;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.logger.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class DirectoryFragment extends Fragment implements FileAdapter.OnFileClickListener {

    private RecyclerView mFilesRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mEmptyView;
    private TextView mCurrentPathText;
    private ImageButton mBtnCreateFolder;
    private ImageButton mBtnCreateFile;
    private FileAdapter mFileAdapter;
    private Stack<File> mPathStack;
    private File mCurrentDirectory;

    private static final String LOG_TAG = "DirectoryFragment";
    private static final String ULTROID_DIR = "Ultroid";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPathStack = new Stack<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mFilesRecyclerView = view.findViewById(R.id.directory_recycler_view);
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mEmptyView = view.findViewById(R.id.empty_view);
        mCurrentPathText = view.findViewById(R.id.current_path_text);
        mBtnCreateFolder = view.findViewById(R.id.btn_create_folder);
        mBtnCreateFile = view.findViewById(R.id.btn_create_file);

        initializeFileManager();

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            loadFiles(mCurrentDirectory);
            mSwipeRefreshLayout.setRefreshing(false);
        });

        return view;
    }

    private void initializeFileManager() {
        mFileAdapter = new FileAdapter(getContext(), this);
        mFilesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mFilesRecyclerView.setAdapter(mFileAdapter);

        File ultroidDir = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR);
        if (!ultroidDir.exists()) {
            ultroidDir.mkdirs();
        }
        mCurrentDirectory = ultroidDir;
        mPathStack.push(ultroidDir);
        loadFiles(ultroidDir);

        mBtnCreateFolder.setOnClickListener(v -> showCreateFolderDialog());
        mBtnCreateFile.setOnClickListener(v -> showCreateFileDialog());
    }

    private void navigateToDirectory(File directory) {
        if (directory.isDirectory()) {
            mCurrentDirectory = directory;
            mPathStack.push(directory);
            loadFiles(directory);
            updatePathDisplay();
        }
    }

    private String getRelativePath(File file) {
        String basePath = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ULTROID_DIR).getAbsolutePath();
        String filePath = file.getAbsolutePath();
        return filePath.substring(basePath.length());
    }

    private void loadFiles(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mFileAdapter.setFiles(new ArrayList<>());
            return;
        }

        mEmptyView.setVisibility(View.GONE);
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        mFileAdapter.setFiles(new ArrayList<>(Arrays.asList(files)));
        updatePathDisplay();
    }

    private void updatePathDisplay() {
        mCurrentPathText.setText("~/" + ULTROID_DIR + getRelativePath(mCurrentDirectory));
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create Folder");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString();
            createFolder(folderName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showCreateFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create File");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String fileName = input.getText().toString();
            createFile(fileName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createFolder(String folderName) {
        File newFolder = new File(mCurrentDirectory, folderName);
        if (newFolder.exists()) {
            Toast.makeText(getContext(), "Folder already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newFolder.mkdir()) {
            loadFiles(mCurrentDirectory);
            Toast.makeText(getContext(), "Folder created", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to create folder", Toast.LENGTH_SHORT).show();
        }
    }

    private void createFile(String fileName) {
        File newFile = new File(mCurrentDirectory, fileName);
        if (newFile.exists()) {
            Toast.makeText(getContext(), "File already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (newFile.createNewFile()) {
                loadFiles(mCurrentDirectory);
                Toast.makeText(getContext(), "File created", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to create file", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error creating file", Toast.LENGTH_SHORT).show();
            Logger.logStackTraceWithMessage(LOG_TAG, "Error creating file", e);
        }
    }

    @Override
    public void onFileClick(File file) {
        if (file.isDirectory()) {
            navigateToDirectory(file);
        } else {
            viewFile(file);
        }
    }

    @Override
    public void onMoreClick(View view, File file) {
        showFileOptionsDialog(file);
    }

    private void showFileOptionsDialog(File file) {
        String[] options = {"Rename", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("File Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showRenameDialog(file);
                        break;
                    case 1:
                        showDeleteConfirmationDialog(file);
                        break;
                }
            });
        builder.show();
    }

    private void showRenameDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Rename");

        final EditText input = new EditText(getContext());
        input.setText(file.getName());
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString();
            renameFile(file, newName);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void renameFile(File file, String newName) {
        File newFile = new File(file.getParentFile(), newName);
        if (newFile.exists()) {
            Toast.makeText(getContext(), "A file with that name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        if (file.renameTo(newFile)) {
            loadFiles(mCurrentDirectory);
            Toast.makeText(getContext(), "File renamed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to rename file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(File file) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete " + file.getName())
            .setMessage("Are you sure you want to delete this " + (file.isDirectory() ? "folder" : "file") + "?")
            .setPositiveButton("Delete", (dialog, which) -> deleteFile(file))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            deleteRecursive(file);
        } else {
            if (file.delete()) {
                loadFiles(mCurrentDirectory);
                Toast.makeText(getContext(), "File deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        
        if (fileOrDirectory.delete()) {
            loadFiles(mCurrentDirectory);
            Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewFile(File file) {
        try {
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(file.getName())
                .setMessage(content.toString())
                .setPositiveButton("Edit", (dialog, which) -> editFile(file))
                .setNegativeButton("Close", null)
                .show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
            Logger.logStackTraceWithMessage(LOG_TAG, "Error reading file", e);
        }
    }

    private void editFile(File file) {
        try {
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Edit " + file.getName());

            final EditText input = new EditText(getContext());
            input.setText(content.toString());
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                try {
                    FileWriter writer = new FileWriter(file);
                    writer.write(input.getText().toString());
                    writer.close();
                    Toast.makeText(getContext(), "File saved", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error saving file", Toast.LENGTH_SHORT).show();
                    Logger.logStackTraceWithMessage(LOG_TAG, "Error saving file", e);
                }
            });
            builder.setNegativeButton("Cancel", null);

            builder.show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
            Logger.logStackTraceWithMessage(LOG_TAG, "Error reading file", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentDirectory != null) {
            loadFiles(mCurrentDirectory);
        }
    }
} 