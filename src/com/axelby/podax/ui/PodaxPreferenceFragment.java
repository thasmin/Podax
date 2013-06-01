package com.axelby.podax.ui;

import android.os.Bundle;

import com.axelby.podax.R;

public class PodaxPreferenceFragment extends PreferenceListFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}