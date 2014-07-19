package com.axelby.podax.ui;

import android.app.Activity;
import android.os.Bundle;

public class EpisodeDetailActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PodaxFragmentActivity.createFragment(this, EpisodeDetailFragment.class, getIntent().getExtras());
    }
}
