package com.termux.app.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final Context mContext;
    private final List<File> mFiles;
    private final OnFileClickListener mListener;
    private final SimpleDateFormat mDateFormat;

    public interface OnFileClickListener {
        void onFileClick(File file);
        void onMoreClick(View view, File file);
    }

    public FileAdapter(Context context, OnFileClickListener listener) {
        mContext = context;
        mListener = listener;
        mFiles = new ArrayList<>();
        mDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = mFiles.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public void setFiles(List<File> files) {
        mFiles.clear();
        mFiles.addAll(files);
        notifyDataSetChanged();
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mFileIcon;
        private final TextView mFileName;
        private final TextView mFileInfo;
        private final ImageButton mBtnMore;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            mFileIcon = itemView.findViewById(R.id.file_icon);
            mFileName = itemView.findViewById(R.id.file_name);
            mFileInfo = itemView.findViewById(R.id.file_info);
            mBtnMore = itemView.findViewById(R.id.btn_more);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && mListener != null) {
                    mListener.onFileClick(mFiles.get(position));
                }
            });

            mBtnMore.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && mListener != null) {
                    mListener.onMoreClick(v, mFiles.get(position));
                }
            });
        }

        void bind(File file) {
            mFileName.setText(file.getName());
            
            if (file.isDirectory()) {
                mFileIcon.setImageResource(R.drawable.ic_folder);
                int filesCount = file.listFiles() != null ? file.listFiles().length : 0;
                mFileInfo.setText(String.format(Locale.getDefault(), "%d items • %s",
                    filesCount, mDateFormat.format(new Date(file.lastModified()))));
            } else {
                mFileIcon.setImageResource(R.drawable.ic_file);
                mFileInfo.setText(String.format(Locale.getDefault(), "%s • %s",
                    Formatter.formatFileSize(mContext, file.length()),
                    mDateFormat.format(new Date(file.lastModified()))));
            }
        }
    }
} 