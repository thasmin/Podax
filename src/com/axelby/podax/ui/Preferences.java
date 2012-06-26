package com.axelby.podax.ui;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class Preferences extends SherlockPreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Helper.registerMediaButtons(this);
	}

}
