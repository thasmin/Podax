package com.axelby.podax.ui;

import android.os.Bundle;

public class QueueActivity extends PodaxFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.createFragment(QueueFragment.class);
	}
}
