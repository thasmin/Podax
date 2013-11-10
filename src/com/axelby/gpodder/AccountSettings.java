package com.axelby.gpodder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.widget.FrameLayout;

import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.ui.PreferenceListFragment;

public class AccountSettings extends FragmentActivity {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		FrameLayout frame = new FrameLayout(this);
		frame.setId(R.id.fragment);
		setContentView(frame, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(R.id.fragment, new AccountSettingsFragment()).commit();
	}

	public class AccountSettingsFragment extends PreferenceListFragment {
		private SharedPreferences _gpodderPrefs;

		public AccountSettingsFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			PreferenceScreen screen = inflateFromResource(R.xml.gpodder_preferences);
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
			device_type.setValue(_gpodderPrefs.getString("type", Helper.isTablet(getActivity()) ? "tablet" : "phone"));
			device_type.setSummary("Currently: " + _gpodderPrefs.getString("type", Helper.isTablet(getActivity()) ? "tablet" : "phone"));
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
