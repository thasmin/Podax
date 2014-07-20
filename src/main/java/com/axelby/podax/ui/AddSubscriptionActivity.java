package com.axelby.podax.ui;

import android.app.Activity;
import android.os.Bundle;

public class AddSubscriptionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PodaxFragmentActivity.createFragment(this, AddSubscriptionFragment.class, getIntent().getExtras());
    }
}
