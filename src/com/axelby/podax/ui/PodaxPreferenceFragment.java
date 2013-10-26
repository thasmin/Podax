package com.axelby.podax.ui;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.axelby.podax.R;

import java.io.File;

public class PodaxPreferenceFragment extends PreferenceListFragment implements Preference.OnPreferenceChangeListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceScreen screen = inflateFromResource(R.xml.preferences);

		// properly trim sdcard options
		ListPreference sdcard = (ListPreference) screen.findPreference("storageCard");
		CharSequence[] entries = sdcard.getEntries();
		boolean[] exists = new boolean[entries.length];
		int newSize = 0;
		for (int i = 0; i < entries.length; ++i) {
			exists[i] = new File(entries[i].toString()).exists();
			newSize += exists[i] ? 1 : 0;
		}

		CharSequence[] newEntries = new CharSequence[newSize];
		int r = 0;
		for (int i = 0; i < newSize; ++i)
			if (exists[i])
				newEntries[r++] = entries[i];

		String title = getString(R.string.pref_sdcard_title) + ": " + sdcard.getValue();
		sdcard.setTitle(title);
		/*
		sdcard.setEntries(newEntries);
		sdcard.setEntryValues(newEntries);
		sdcard.setEnabled(newSize > 1);
		*/

		sdcard.setOnPreferenceChangeListener(this);
		setPreferenceScreen(screen);
	}


	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("storageCard")) {
			ListPreference sdcard = (ListPreference) preference;
			String title = getString(R.string.pref_sdcard_title) + ": " + newValue;
			sdcard.setTitle(title);
		}
		return true;
	}
}