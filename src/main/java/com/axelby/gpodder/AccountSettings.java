package com.axelby.gpodder;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.FrameLayout;

import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class AccountSettings extends Activity {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		FrameLayout frame = new FrameLayout(this);
		frame.setId(R.id.fragment);
		setContentView(frame, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.fragment, new AccountSettingsFragment()).commit();
	}

	public static class AccountSettingsFragment extends PreferenceFragment {
		private SharedPreferences _gpodderPrefs;

		public AccountSettingsFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.gpodder_preferences);
			PreferenceScreen screen = getPreferenceScreen();
			_gpodderPrefs = getActivity().getSharedPreferences("gpodder", Context.MODE_PRIVATE);

			EditTextPreference device_name = (EditTextPreference) screen.findPreference("device_name");
			device_name.setText(_gpodderPrefs.getString("caption", "podax"));
			device_name.setSummary("Currently: " + _gpodderPrefs.getString("caption", "podax"));
			device_name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object value) {
					_gpodderPrefs.edit()
							.putString("caption", value.toString())
							.putBoolean("configurationNeedsUpdate", true)
							.commit();
					preference.setSummary("Currently: " + value);
					return true;
				}
			});

			ListPreference device_type = (ListPreference) screen.findPreference("device_type");
			device_type.setValue(_gpodderPrefs.getString("type", Helper.isTablet(getActivity()) ? "laptop" : "phone"));
			device_type.setSummary("Currently: " + _gpodderPrefs.getString("type", Helper.isTablet(getActivity()) ? "laptop" : "phone"));
			device_type.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object value) {
					_gpodderPrefs.edit()
							.putString("type", value.toString())
							.putBoolean("configurationNeedsUpdate", true)
							.commit();
					preference.setSummary("Currently: " + value);
					return true;
				}
			});

			setPreferenceScreen(screen);
		}
	}
}
