package com.axelby.podax.ui;

import android.content.Context;
import android.graphics.Paint;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.axelby.podax.R;

import javax.annotation.Nonnull;

class LimitedSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
	// controls
	private SeekBar _seekBar;
	private TextView _statusText;

	// config
	private float _defaultValue;
	private String[] _entries;
	private float[] _entryValues;

	// current state
	private int _currentProgress = 0;
	private boolean _enabled = true;

	private static final String ANDROID_NS ="http://schemas.android.com/apk/res/android";

	public LimitedSeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPreference(context, attrs);
	}

	public LimitedSeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPreference(context, attrs);
	}

	private void initPreference(Context context, AttributeSet attrs) {
		// get default values
		try {
			_defaultValue = attrs.getAttributeFloatValue(ANDROID_NS, "defaultValue", 1.0f);
		} catch (Exception ex) {
			try {
				_defaultValue = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", 1);
			} catch (Exception ex2) {
				_defaultValue = 1.0f;
			}
		}

		// get entry and entryValues attributes
		int entriesId = attrs.getAttributeResourceValue(ANDROID_NS, "entries", -1);
		_entries = context.getResources().getStringArray(entriesId);

		int entryValuesId = attrs.getAttributeResourceValue(ANDROID_NS, "entryValues", -1);
		String[] entryValues = context.getResources().getStringArray(entryValuesId);
		_entryValues = new float[entryValues.length];
		for (int i = 0; i < entryValues.length; ++i)
			_entryValues[i] = Float.valueOf(entryValues[i]);

		setWidgetLayoutResource(R.layout.limited_seek_bar_preference);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);

		// The basic preference layout puts the widget frame to the right of the title and summary,
		// so we need to change it a bit - the seekbar should be under them.
		LinearLayout layout = (LinearLayout) view;
		if (layout != null)
			layout.setOrientation(LinearLayout.VERTICAL);

		return view;
	}

	@Override
	public void onBindView(@Nonnull View view) {
		super.onBindView(view);

		_seekBar = (SeekBar) view.findViewById(R.id.seekBar);
		_seekBar.setMax(_entryValues.length - 1);
		_seekBar.setEnabled(_enabled);

		// make sure that the preference isn't already saved as something other than a float
		try {
			getPersistedFloat(1.0f);
		} catch (java.lang.ClassCastException ex) {
			if (getEditor() != null)
				getEditor().remove(getKey()).commit();
		}

		// set to persisted value
		float value = getPersistedFloat(_defaultValue);
		for (int i = 0; i < _entryValues.length; ++i) {
			if (_entryValues[i] == value) {
				_currentProgress = i;
				_seekBar.setProgress(i);
			}
		}
		_seekBar.setOnSeekBarChangeListener(this);

		_statusText = (TextView) view.findViewById(R.id.statusText);
		Paint p = _statusText.getPaint();
		p.setTextSize(_statusText.getTextSize());
		p.setTypeface(_statusText.getTypeface());
		float maxWidth = 0;
		for (String entry : _entries)
			maxWidth = Math.max(maxWidth, p.measureText(entry));
		_statusText.setWidth((int)Math.ceil(maxWidth));
		_statusText.setText(_entries[_currentProgress]);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		float newValue = _entryValues[progress];

		if (!callChangeListener(newValue)) {
			// change rejected, revert to the previous value
			seekBar.setProgress(_currentProgress);
			return;
		}

		// change accepted, store it
		_currentProgress = progress;
		_statusText.setText(_entries[progress]);
		persistFloat(newValue);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) { }

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) { }

	/**
	 * make sure that the seekbar is disabled if the preference is disabled
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		_enabled = enabled;
		if (_seekBar != null)
			_seekBar.setEnabled(enabled);
	}

	@Override
	public void onDependencyChanged(Preference dependency, boolean disableDependent) {
		super.onDependencyChanged(dependency, disableDependent);

		// disable movement of seek bar when dependency is false
		if (_seekBar != null)
			_seekBar.setEnabled(!disableDependent);
	}
}
