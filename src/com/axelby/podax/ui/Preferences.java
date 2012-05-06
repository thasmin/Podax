package com.axelby.podax.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.axelby.podax.R;

public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
