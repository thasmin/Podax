package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionData;
import com.axelby.podax.SubscriptionProvider;
import com.trello.rxlifecycle.components.RxFragment;

import org.acra.ACRA;

import rx.android.schedulers.AndroidSchedulers;

public class SubscriptionSettingsFragment extends RxFragment {

	private boolean _initializedUI = false;
	private Uri _subscriptionUri;
	private String _feedTitle;
	private EditText _name;
	private CheckBox _autoName;
	private RadioGroup _autoPlaylist;
	private RadioGroup _expiration;
	private TextWatcher _nameWatcher;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_settings, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		long subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
		if (subscriptionId == -1) {
            ACRA.getErrorReporter().handleSilentException(new Exception("subscription settings got a -1"));
            return;
        }
		_subscriptionUri = ContentUris.withAppendedId(SubscriptionProvider.URI, subscriptionId);

		SubscriptionData.getObservable(activity, subscriptionId)
			.observeOn(AndroidSchedulers.mainThread())
			.compose(bindToLifecycle())
			.subscribe(
				this::setSubscription,
				e -> Log.e("EpisodeDetailFragment", "unable to load subscription", e)
			);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_name = (EditText) view.findViewById(R.id.name);
		_autoName = (CheckBox) view.findViewById(R.id.nameAuto);
		_autoPlaylist = (RadioGroup) view.findViewById(R.id.autoPlaylistGroup);
		_expiration = (RadioGroup) view.findViewById(R.id.expireGroup);

		_nameWatcher = new TextWatcher() {
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

			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
		};
	}

	@Override
	public void onPause() {
		super.onPause();

		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(_name.getWindowToken(), 0);
	}

	public void setSubscription(SubscriptionData sub) {
		// assume any changes to subscription came from this screen
		// and it doesn't need to be updated to reflect current state
		if (_initializedUI)
			return;
		_initializedUI = true;

		_feedTitle = sub.getTitle();

		String titleOverride = sub.getTitleOverride();
		if (titleOverride != null) {
			_name.setText(titleOverride);
			_autoName.setChecked(false);
		} else {
			_name.setText(sub.getRawTitle());
			_autoName.setChecked(true);
		}

		if (!sub.areNewEpisodesAddedToPlaylist())
			_autoPlaylist.check(R.id.autoPlaylistNo);

		switch (sub.getExpirationDays()) {
			case 7:
				_expiration.check(R.id.expire7);
				break;
			case 14:
				_expiration.check(R.id.expire14);
				break;
		}

		_name.addTextChangedListener(_nameWatcher);

		_autoName.setOnCheckedChangeListener((button, checked) -> {
			ContentValues values = new ContentValues();
			if (checked)
				values.putNull(SubscriptionProvider.COLUMN_TITLE_OVERRIDE);
			else
				values.put(SubscriptionProvider.COLUMN_TITLE_OVERRIDE, _feedTitle);
			getActivity().getContentResolver().update(_subscriptionUri, values, null, null);

			if (checked) {
				_name.removeTextChangedListener(_nameWatcher);
				_name.setText(_feedTitle);
				_name.addTextChangedListener(_nameWatcher);
			}
		});

		_autoPlaylist.setOnCheckedChangeListener((group, checkedId) -> {
			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_PLAYLIST_NEW, checkedId == R.id.autoPlaylistYes);
			getActivity().getContentResolver().update(_subscriptionUri, values, null, null);
		});

		_expiration.setOnCheckedChangeListener((group, checkedId) -> {
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
		});
	}

}
