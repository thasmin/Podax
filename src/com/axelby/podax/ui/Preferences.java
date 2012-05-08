package com.axelby.podax.ui;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.axelby.podax.R;

public class Preferences extends SherlockPreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
