package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeData;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Episodes;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.FlattrHelper.NoAppSecretFlattrException;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.databinding.EpisodeDetailBinding;
import com.trello.rxlifecycle.components.RxFragment;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.ForbiddenException;
import org.shredzone.flattr4j.model.AutoSubmission;

import java.util.Locale;

import javax.annotation.Nullable;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class EpisodeDetailFragment extends RxFragment {
	private long _podcastId;
	private boolean _onActive;

	private Subscriber<EpisodeData> _episodeDataSubscriber;

	private EpisodeDetailBinding _binding;
	private boolean _seekbar_dragging = false;

	private AsyncTask<Long, Void, Void> _flattr_task = new AsyncTask<Long, Void, Void>() {
		@Override
		protected Void doInBackground(Long... params) {
			Activity activity = getActivity();
			EpisodeData episode = EpisodeData.create(activity, _podcastId);
			if (episode == null)
				return null;

			String payment_url = episode.getPaymentUrl();
			if (payment_url == null)
				return null;

			AutoSubmission sub = FlattrHelper.parseAutoSubmissionLink(Uri.parse(payment_url));
			if (sub == null) {
				// it's not a flattr link
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(episode.getPaymentUrl()));
				startActivity(intent);
				return null;
			}

			try {
				FlattrHelper.flattr(activity, sub);
				showToast("You flattred " + episode.getTitle() + "!");
			} catch (ForbiddenException e) {
				if (e.getCode().equals("flattr_once")) {
					showToast("Podcast was already flattred");
				} else {
					try {
						FlattrHelper.obtainToken(activity);
					} catch (NoAppSecretFlattrException e1) {
						showToast("No flattr app secret in this build.");
					}
				}
			} catch (FlattrException e) {
				showToast("Could not flattr: " + e.getMessage());
			}

			return null;
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_EPISODE_ID)) {
			_podcastId = getArguments().getLong(Constants.EXTRA_EPISODE_ID);
		} else {
			_podcastId = EpisodeCursor.getActiveEpisodeId(getActivity());
			_onActive = true;
		}

		if (_podcastId == -1) {
			Log.w("EpisodeDetailFragment", "no active episode to show");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		_binding = EpisodeDetailBinding.inflate(inflater);
		return _binding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				_binding.position.setText(Helper.getTimeString(progress));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = false;
				EpisodeProvider.movePositionTo(getActivity(), _podcastId, seekBar.getProgress());
			}
		};
		_binding.seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

		_binding.payment.setOnClickListener(v -> _flattr_task.execute(_podcastId));

		// initialize data and set up subscribers
		initializeUI(EpisodeData.create(getActivity(), _podcastId));
		if (_onActive)
			subscribeToActivePodcastChanges();
		subscribeToPodcastData();
	}

	private void subscribeToActivePodcastChanges() {
		PlayerStatus.asObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.compose(bindToLifecycle())
			.subscribe(
				this::updateControls,
				e -> Log.e("EpisodeDetailFragment", "unable to watch active episode", e)
			);
	}

	private void subscribeToPodcastData() {
		if (_episodeDataSubscriber == null) {
			_episodeDataSubscriber = new Subscriber<EpisodeData>() {
				@Override public void onCompleted() { }
				@Override public void onError(Throwable e) { Log.e("EpisodeDetailFragment", "unable to initialize active episode", e); }
				@Override public void onNext(EpisodeData episodeData) { initializeUI(episodeData); }
			};
		} else {
			_episodeDataSubscriber.unsubscribe();
		}

		Episodes.getObservable(getActivity(), _podcastId)
			.compose(bindToLifecycle())
			.subscribe(_episodeDataSubscriber);
	}


	private void showToast(final String message) {
		getActivity().runOnUiThread(() -> {
			View v = getActivity().findViewById(R.id.scrollview);
			Snackbar.make(v, message, Snackbar.LENGTH_LONG).show();
		});
	}

	private void initializeUI(EpisodeData episode) {
		_podcastId = episode.getId();
		_binding.setEpisode(episode);

		String description = episode.getDescription();
		if (description != null) {
			int textColor = ContextCompat.getColor(getActivity(), R.color.primary_text_default_material_dark);
			String textColorRgba = String.format(Locale.US, "rgba(%d, %d, %d, %d)",
					Color.red(textColor), Color.green(textColor), Color.blue(textColor), Color.alpha(textColor));

			int bgColor = ContextCompat.getColor(getActivity(), R.color.primary_material_dark);
			String bgColorRgba = String.format(Locale.US, "rgba(%d, %d, %d, %d)",
					Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor), Color.alpha(bgColor));

			int linkColor = ContextCompat.getColor(getActivity(), R.color.orange500);
			String linkColorRgba = String.format(Locale.US, "rgba(%d, %d, %d, %d)",
					Color.red(linkColor), Color.green(linkColor), Color.blue(linkColor), Color.alpha(linkColor));

			description = description.replaceAll("color:", "color,");
			String fullhtml = "<html><head><title></title><style>body{color:" + textColorRgba + ";background:" + bgColorRgba + "} a{color:" + linkColorRgba + "}</style></head><body>" + description + "</body></html>";

			_binding.description.setBackgroundColor(bgColor);
			_binding.description.getSettings().setDefaultTextEncodingName("utf-8");
			_binding.description.loadData(fullhtml, "text/html; charset=utf-8", null);
		}
	}

	private void updateControls(PlayerStatus status) {
		if (status == null)
			return;

		if (status.getDuration() == 0) {
			_binding.position.setText(Helper.getTimeString(status.getPosition()));
			_binding.duration.setText("");
			_binding.seekbar.setEnabled(false);
		} else if (!_seekbar_dragging) {
			_binding.position.setText(Helper.getTimeString(status.getPosition()));
			_binding.duration.setText("-" + Helper.getTimeString(status.getDuration() - status.getPosition()));
			_binding.seekbar.setProgress(status.getPosition());
			_binding.seekbar.setMax(status.getDuration());
			_binding.seekbar.setEnabled(true);
		}

		boolean isPlaying = status.isPlaying() && status.getEpisodeId() == _podcastId;
		int playResource = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
		_binding.playBtn.setImageResource(playResource);
	}

	@Override
	public void onResume() {
		super.onResume();

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				FlattrHelper.handleResumeActivityObtainToken(getActivity());
				return null;
			}
		}.execute();
	}
}
