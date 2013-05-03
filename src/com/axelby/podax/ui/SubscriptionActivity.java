package com.axelby.podax.ui;

import android.os.Bundle;

public class SubscriptionActivity extends PodaxFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.createFragment(SubscriptionFragment.class);
	}
}
