package com.axelby.podax.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.axelby.podax.R;
import com.axelby.podax.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class StorageListPreference extends ListPreference {
	@TargetApi(21)
	@SuppressWarnings("UnusedDeclaration")
	public StorageListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@TargetApi(21)
	@SuppressWarnings("UnusedDeclaration")
	public StorageListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@SuppressWarnings("UnusedDeclaration")
	public StorageListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@SuppressWarnings("UnusedDeclaration")
	public StorageListPreference(Context context) {
		super(context);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		setStorageDirectories();
		if (getEntries().length == 1) {
			setValueIndex(0);
			setEnabled(false);
		}

		if (getValue() == null || findIndexOfValue(getValue()) == -1)
			setValueIndex(0);

		String title = getContext().getString(R.string.pref_sdcard_title) + ": " + getEntry();
		setTitle(title);
		setOnPreferenceChangeListener((preference, newValue) -> {
			Storage.moveFilesTo(getContext(), newValue.toString());

			int index = findIndexOfValue(newValue.toString());
			setTitle(getContext().getString(R.string.pref_sdcard_title) + ": " + getEntries()[index]);
			return true;
		});
	}

	/**
	 * Returns all available SD cards in the system
	 * Based on Android source code of version 4.3 (API 18)
	 */
	private void setStorageDirectories()
	{
		ArrayList<String> entries = new ArrayList<>(6);
		ArrayList<String> values = new ArrayList<>(6);

		final String externalStorage = System.getenv("EXTERNAL_STORAGE");
		final String emulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");

		if (TextUtils.isEmpty(emulatedStorageTarget)) {
			addPathEntry(entries, values, "Phone", TextUtils.isEmpty(externalStorage) ? "/storage/sdcard0" : externalStorage);
		} else {
			// Device has emulated storage
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
				addPathEntry(entries, values, "Phone", emulatedStorageTarget);
			else
			{
				final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
				final String[] pathParts = path.split(File.pathSeparator);
				final String lastFolder = pathParts[pathParts.length - 1];
				try
				{
					Integer.valueOf(lastFolder);
					addPathEntry(entries, values, "Phone", emulatedStorageTarget + File.separator + lastFolder);
				}
				catch(NumberFormatException ignored)
				{
					addPathEntry(entries, values, "Phone", emulatedStorageTarget);
				}
			}
		}

		// All Secondary SD-CARDs (all exclude primary) separated by ":"
		final String secondaryStorage = System.getenv("SECONDARY_STORAGE");
		if (secondaryStorage != null) {
			final String[] secondarySplit = secondaryStorage.split(File.pathSeparator);
			if (secondarySplit.length == 1) {
				addPathEntry(entries, values, "SD Card", secondaryStorage);
			} else {
				for (int i = 0; i < secondarySplit.length; ++i)
					addPathEntry(entries, values, "SD Card " + (i + 1), secondarySplit[i]);
			}
		}

		setEntries(entries.toArray(new String[entries.size()]));
		setEntryValues(values.toArray(new String[entries.size()]));
	}

	private void addPathEntry(ArrayList<String> entries, ArrayList<String> values, String desc, String path) {
		values.add(path);
		entries.add(String.format(Locale.getDefault(), "%s, %d GB free", desc, getFreeGB(path)));
	}

	private long getFreeGB(String sdcardPath) {
		StatFs statFs = new StatFs(sdcardPath);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
			return Math.round(statFs.getAvailableBytes() / 1024.0 / 1024 / 1024);
		return Math.round(statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong() / 1024.0 / 1024 / 1024);
	}
}
