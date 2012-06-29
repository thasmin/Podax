package com.axelby.podax.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.axelby.podax.R;

public class PreferenceFragment extends SherlockListFragment{

	private PreferenceManager _preferenceManager;
	private ListView _lv;

	// The starting request code given out to preference framework.
	private static final int FIRST_REQUEST_CODE = 100;

	private static final int MSG_BIND_PREFERENCES = 0;
	
	// lint warning is suppressed because this how it's done in android code
	@SuppressLint("HandlerLeak")
	private Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MSG_BIND_PREFERENCES:
				bindPreferences();
				break;
			}
		}
	};

	public PreferenceFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
		postBindPreferences();
		return _lv;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ViewParent p = _lv.getParent();
		if(p != null)
			((ViewGroup)p).removeView(_lv);
	}

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);

		_preferenceManager = onCreatePreferenceManager();
		_lv = (ListView) LayoutInflater.from(getActivity()).inflate(R.layout.preference_list, null);
		_lv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		int preferenceResource = getArguments().getInt("xml");
		addPreferencesFromResource(preferenceResource);
		postBindPreferences();
		if (getActivity() instanceof OnPreferenceAttachedListener)
			((OnPreferenceAttachedListener)getActivity()).onPreferenceAttached(getPreferenceScreen(), preferenceResource);
	}

	@Override
	public void onStop() {
		super.onStop();
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityStop");
			m.setAccessible(true);
			m.invoke(_preferenceManager);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		_lv = null;
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityDestroy");
			m.setAccessible(true);
			m.invoke(_preferenceManager);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("xml", outState.getInt("xml"));
		super.onSaveInstanceState(outState);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityResult", int.class, int.class, Intent.class);
			m.setAccessible(true);
			m.invoke(_preferenceManager, requestCode, resultCode, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Posts a message to bind the preferences to the list view.
	 * <p>
	 * Binding late is preferred as any custom preference types created in
	 * {@link #onCreate(Bundle)} are able to have their views recycled.
	 */
	private void postBindPreferences() {
		if (_handler.hasMessages(MSG_BIND_PREFERENCES)) return;
		_handler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
	}

	private void bindPreferences() {
		final PreferenceScreen preferenceScreen = getPreferenceScreen();
		if (preferenceScreen != null) {
			preferenceScreen.bind(_lv);
		}
	}

	/**
	 * Creates the {@link PreferenceManager}.
	 * 
	 * @return The {@link PreferenceManager} used by this activity.
	 */
	private PreferenceManager onCreatePreferenceManager() {
		try {
			Constructor<PreferenceManager> c = PreferenceManager.class.getDeclaredConstructor(Activity.class, int.class);
			c.setAccessible(true);
			PreferenceManager preferenceManager = c.newInstance(this.getActivity(), FIRST_REQUEST_CODE);
			return preferenceManager;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the {@link PreferenceManager} used by this activity.
	 * @return The {@link PreferenceManager}.
	 */
	public PreferenceManager getPreferenceManager() {
		return _preferenceManager;
	}

	/**
	 * Sets the root of the preference hierarchy that this activity is showing.
	 * 
	 * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
	 */
	public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("setPreferences", PreferenceScreen.class);
			m.setAccessible(true);
			boolean result = (Boolean) m.invoke(_preferenceManager, preferenceScreen);
			if (result && preferenceScreen != null)
				postBindPreferences();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the root of the preference hierarchy that this activity is showing.
	 * 
	 * @return The {@link PreferenceScreen} that is the root of the preference
	 *         hierarchy.
	 */
	public PreferenceScreen getPreferenceScreen() {
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen");
			m.setAccessible(true);
			return (PreferenceScreen) m.invoke(_preferenceManager);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Adds preferences from activities that match the given {@link Intent}.
	 * 
	 * @param intent The {@link Intent} to query activities.
	 */
	public void addPreferencesFromIntent(Intent intent) {
		throw new RuntimeException("too lazy to include this bs");
	}

	/**
	 * Inflates the given XML resource and adds the preference hierarchy to the current
	 * preference hierarchy.
	 * 
	 * @param preferencesResId The XML resource ID to inflate.
	 */
	public void addPreferencesFromResource(int preferencesResId) {   
		try {
			Method m = PreferenceManager.class.getDeclaredMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class);
			m.setAccessible(true);
			PreferenceScreen prefScreen = (PreferenceScreen) m.invoke(_preferenceManager, getActivity(), preferencesResId, getPreferenceScreen());
			setPreferenceScreen(prefScreen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Finds a {@link Preference} based on its key.
	 * 
	 * @param key The key of the preference to retrieve.
	 * @return The {@link Preference} with the key, or null.
	 * @see PreferenceGroup#findPreference(CharSequence)
	 */
	public Preference findPreference(CharSequence key) {
		if (_preferenceManager == null) {
			return null;
		}
		return _preferenceManager.findPreference(key);
	}

	public interface OnPreferenceAttachedListener {
		public void onPreferenceAttached(PreferenceScreen root, int xmlId);
	}

}