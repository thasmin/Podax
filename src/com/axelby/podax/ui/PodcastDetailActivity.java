package com.axelby.podax.ui;

import android.os.Bundle;

public class PodcastDetailActivity extends PodaxFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.createFragment(PodcastDetailFragment.class);
	}
}
