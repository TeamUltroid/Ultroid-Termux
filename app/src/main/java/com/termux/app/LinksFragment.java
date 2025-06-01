package com.termux.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.termux.R;

public class LinksFragment extends Fragment {

    private CardView mCardGithub;
    private CardView mCardDocs;
    private CardView mCardTelegram;
    private CardView mCardPlugins;
    private CardView mCardDonate;
    private Toolbar mToolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_links, container, false);

        // Initialize toolbar
        mToolbar = view.findViewById(R.id.toolbar_links);
        
        // Initialize views
        mCardGithub = view.findViewById(R.id.card_github);
        mCardDocs = view.findViewById(R.id.card_docs);
        mCardTelegram = view.findViewById(R.id.card_telegram);
        mCardPlugins = view.findViewById(R.id.card_plugins);
        mCardDonate = view.findViewById(R.id.card_donate);

        // Set click listeners
        mCardGithub.setOnClickListener(v -> openUrl("https://github.com/TeamUltroid/Ultroid"));
        mCardDocs.setOnClickListener(v -> openUrl("https://ultroid.tech/docs"));
        mCardTelegram.setOnClickListener(v -> openUrl("https://t.me/TeamUltroid"));
        mCardPlugins.setOnClickListener(v -> openUrl("https://github.com/TeamUltroid/UltroidAddons"));
        mCardDonate.setOnClickListener(v -> openUrl("https://ultroid.tech/donate"));

        return view;
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
} 