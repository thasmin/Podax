package com.axelby.podax.ui;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.ex.variablespeed.VariableSpeedNative;
import com.axelby.podax.R;

import java.io.File;

public class PodaxPreferenceFragment extends PreferenceListFragment implements Preference.OnPreferenceChangeListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceScreen screen = inflateFromResource(R.xml.preferences);

		// properly trim sdcard options
		ListPreference sdcard = (ListPreference) screen.findPreference("storageCard");
		if (sdcard != null) {
			CharSequence[] entries = sdcard.getEntries();
			CharSequence[] values = sdcard.getEntryValues();
			if (!new File(values[1].toString()).exists()) {
				sdcard.setEntries(new CharSequence[]{entries[0]});
				sdcard.setEntryValues(new CharSequence[]{values[0]});
				sdcard.setEnabled(false);
			}

			String title = getString(R.string.pref_sdcard_title) + ": " + getEntryText(sdcard, sdcard.getValue());
			sdcard.setTitle(title);

			sdcard.setOnPreferenceChangeListener(this);
		}

		LimitedSeekBarPreference playbackRate = (LimitedSeekBarPreference) screen.findPreference("playbackRate");
		if (playbackRate != null) {
			if (!VariableSpeedNative.canLoad()) {
				playbackRate.setEnabled(false);
				playbackRate.setSummary(R.string.playbackrate_disabled);
			}
		}
		setPreferenceScreen(screen);
	}

	private CharSequence getEntryText(ListPreference listPreference, String value) {
		if (!value.equals("/storage/sdcard0") && !value.equals("/storage/extSdCard"))
			return getResources().getStringArray(R.array.pref_sdcard_entries)[0];
		return listPreference.getEntries()[listPreference.findIndexOfValue(value)];
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("storageCard")) {
			ListPreference sdcard = (ListPreference) preference;
			String title = getString(R.string.pref_sdcard_title) + ": " + getEntryText(sdcard, newValue.toString());
			sdcard.setTitle(title);
			return true;
		}
		return false;
	}
}
