package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Context;
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
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;
import com.trello.rxlifecycle.components.RxFragment;

import org.acra.ACRA;

import rx.android.schedulers.AndroidSchedulers;

public class SubscriptionSettingsFragment extends RxFragment {

	private long _subscriptionId;

	private boolean _initializedUI = false;
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

		_subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
		if (_subscriptionId == -1) {
            ACRA.getErrorReporter().handleSilentException(new Exception("subscription settings got a -1"));
            return;
        }

		PodaxDB.subscriptions.watch(_subscriptionId)
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
				SubscriptionEditor editor = new SubscriptionEditor(_subscriptionId);
				if (newTitle.equals(_feedTitle)) {
					editor.setTitleOverride(null);
					_autoName.setChecked(true);
				} else {
					editor.setTitleOverride(null);
					_autoName.setChecked(false);
				}
				editor.commit();
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
			new SubscriptionEditor(_subscriptionId)
				.setTitleOverride(checked ? null : _feedTitle)
				.commit();

			if (checked) {
				_name.removeTextChangedListener(_nameWatcher);
				_name.setText(_feedTitle);
				_name.addTextChangedListener(_nameWatcher);
			}
		});

		_autoPlaylist.setOnCheckedChangeListener((group, checkedId) ->
			new SubscriptionEditor(_subscriptionId)
				.setPlaylistNew(checkedId == R.id.autoPlaylistYes)
				.commit());

		_expiration.setOnCheckedChangeListener((group, checkedId) -> {
			SubscriptionEditor editor = new SubscriptionEditor(_subscriptionId);
			switch (checkedId) {
				case R.id.expire0:
					editor.setExpirationDays(null);
					break;
				case R.id.expire7:
					editor.setExpirationDays(7);
					break;
				case R.id.expire14:
					editor.setExpirationDays(14);
					break;
			}
			editor.commit();
		});
	}

}
