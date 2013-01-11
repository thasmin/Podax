package com.axelby.podax.ui;

import android.os.Bundle;

public class SearchActivity extends PodaxFragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupPodaxFragment(SearchFragment.class);
	}
}
