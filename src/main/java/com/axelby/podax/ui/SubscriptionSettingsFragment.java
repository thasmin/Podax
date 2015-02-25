package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionProvider;

import org.acra.ACRA;

public class SubscriptionSettingsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private boolean init = false;
	private Uri _subscriptionUri;
	private String _feedTitle;
	private EditText _name;
	private CheckBox _autoName;
	private RadioGroup _autoPlaylist;
	private RadioGroup _expiration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		long _subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
		if (_subscriptionId == -1) {
            ACRA.getErrorReporter().handleSilentException(new Exception("subscription settings got a -1"));
            return;
        }
		_subscriptionUri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);

		Bundle bundle = new Bundle();
		bundle.putLong("id", _subscriptionId);
		getLoaderManager().initLoader(0, bundle, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_settings, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_name = (EditText) getActivity().findViewById(R.id.name);
		_name.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String newTitle = s.toString();
				ContentValues values = new ContentValues();
				if (newTitle.equals(_feedTitle)) {
					values.putNull(SubscriptionProvider.COLUMN_TITLE_OVERRIDE);
					_autoName.setChecked(true);
				} else {
					values.put(SubscriptionProvider.COLUMN_TITLE_OVERRIDE, newTitle);
					_autoName.setChecked(false);
				}
				getActivity().getContentResolver().update(_subscriptionUri, values, null, null);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});

		_autoName = (CheckBox) getActivity().findViewById(R.id.nameAuto);
		_autoPlaylist = (RadioGroup) getActivity().findViewById(R.id.autoPlaylistGroup);
		_expiration = (RadioGroup) getActivity().findViewById(R.id.expireGroup);
	}

	@Override
	public void onPause() {
		super.onPause();

		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(_name.getWindowToken(), 0);
	}

	void initializeControls() {
		_autoName.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean checked) {
				ContentValues values = new ContentValues();
				values.putNull(SubscriptionProvider.COLUMN_TITLE_OVERRIDE);
				getActivity().getContentResolver().update(_subscriptionUri, values, null, null);
				if (checked)
					_name.setText(_feedTitle);
			}
		});

		_autoPlaylist.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                ContentValues values = new ContentValues();
                values.put(SubscriptionProvider.COLUMN_PLAYLIST_NEW, checkedId == R.id.autoPlaylistYes);
                getActivity().getContentResolver().update(_subscriptionUri, values, null, null);
            }
        });

		_expiration.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				ContentValues values = new ContentValues();
				switch (checkedId) {
					case R.id.expire0:
						values.putNull(SubscriptionProvider.COLUMN_EXPIRATION);
						break;
					case R.id.expire7:
						values.put(SubscriptionProvider.COLUMN_EXPIRATION, 7);
						break;
					case R.id.expire14:
						values.put(SubscriptionProvider.COLUMN_EXPIRATION, 14);
						break;
				}
				getActivity().getContentResolver().update(_subscriptionUri, values, null, null);
			}
		});
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		String[] projection = {
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_TITLE_OVERRIDE,
				SubscriptionProvider.COLUMN_PLAYLIST_NEW,
				SubscriptionProvider.COLUMN_EXPIRATION,
		};
		long subscriptionId = bundle.getLong("id");
		if (subscriptionId == -1)
			return null;
		Uri uri = ContentUris.withAppendedId(SubscriptionProvider.URI, subscriptionId);
		return new CursorLoader(getActivity(), uri, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (loader.getId() != 0)
			return;

		if (init)
			return;
		init = true;

		if (getActivity() == null)
			return;
		if (!cursor.moveToFirst()) {
			cursor.close();
			getActivity().finish();
		}

		_feedTitle = cursor.getString(0);
		if (!cursor.isNull(1)) {
			_name.setText(cursor.getString(1));
			_autoName.setChecked(false);
		} else {
			_name.setText(_feedTitle);
			_autoName.setChecked(true);
		}

		if (!cursor.isNull(2) && cursor.getInt(2) == 0)
			_autoPlaylist.check(R.id.autoPlaylistNo);

		if (!cursor.isNull(3)) {
			switch (cursor.getInt(3)) {
				case 7:
					_expiration.check(R.id.expire7);
					break;
				case 14:
					_expiration.check(R.id.expire14);
					break;
			}
		}

		initializeControls();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

}
