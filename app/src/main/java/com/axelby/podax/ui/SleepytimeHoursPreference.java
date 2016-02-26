package com.axelby.podax.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.axelby.podax.R;

public class SleepytimeHoursPreference extends Preference {

	private RangeSeekBar _times;
	private String _minKey;
	private String _maxKey;

	@SuppressWarnings("UnusedDeclaration")
	public SleepytimeHoursPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPreference(attrs);
	}

	@SuppressWarnings("UnusedDeclaration")
	public SleepytimeHoursPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPreference(attrs);
	}

	private static final String APP_NS ="http://schemas.android.com/apk/res-auto";

	private void initPreference(AttributeSet attrs) {
		_minKey = attrs.getAttributeValue(APP_NS, "minKey");
		_maxKey = attrs.getAttributeValue(APP_NS, "maxKey");
		if (_minKey == null)
			throw new IllegalArgumentException("minKey must be specified");
		if (_maxKey == null)
			throw new IllegalArgumentException("minKey must be specified");

		setWidgetLayoutResource(R.layout.preference_sleepytimehours);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
		LinearLayout layout = (LinearLayout) view;
		if (layout != null)
			layout.setOrientation(LinearLayout.VERTICAL);
		return view;
	}

	@Override
	protected void onBindView(@NonNull View view) {
		super.onBindView(view);

		_times = (RangeSeekBar) view.findViewById(R.id.times);
		_times.setOnRangeSeekBarChangeListener((rangeSeekBar, minValue, maxValue) -> {
			int[] hours = {20, 21, 22, 23, 0, 1, 2, 3, 4};
			int minHour = hours[minValue];
			int maxHour = hours[maxValue];

			SharedPreferences.Editor editor = getSharedPreferences().edit();
			editor.putInt(_minKey, minHour);
			editor.putInt(_maxKey, maxHour);
			editor.apply();
		});
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (_times != null)
			_times.setEnabled(enabled);
	}

	@Override
	public void onDependencyChanged(Preference dependency, boolean disableDependent) {
		super.onDependencyChanged(dependency, disableDependent);
		setEnabled(!disableDependent);
	}
}
