package com.axelby.podax.ui;

import android.os.Bundle;

public class PodcastListActivity extends PodaxFragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupPodaxFragment(PodcastListFragment.class);
	}
}
