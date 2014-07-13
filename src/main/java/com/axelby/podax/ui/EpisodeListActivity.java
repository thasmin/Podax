package com.axelby.podax.ui;

import android.os.Bundle;

public class EpisodeListActivity extends PodaxFragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createFragment(EpisodeListFragment.class, getIntent().getExtras());
    }
}
