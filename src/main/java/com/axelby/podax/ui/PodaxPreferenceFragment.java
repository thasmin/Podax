package com.axelby.podax.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.axelby.podax.R;

public class PodaxPreferenceFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}
}
