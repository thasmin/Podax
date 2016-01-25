package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeData;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.FlattrHelper.NoAppSecretFlattrException;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
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

	private ImageView _subscriptionImage;
	private TextView _titleView;
	private TextView _subscriptionTitleView;
	private WebView _descriptionView;
	private Button _playlistButton;
	private TextView _playlistPosition;
	private ImageButton _playButton;
	private SeekBar _seekbar;
	private boolean _seekbar_dragging = false;
	private Button _paymentButton;
	private Button _viewInBrowserButton;
	private TextView _position;
	private TextView _duration;
	private AsyncTask<Long, Void, Void> _flattr_task = new AsyncTask<Long, Void, Void>() {
		@Override
		protected Void doInBackground(Long... params) {
			Uri podcastUri = ContentUris.withAppendedId(EpisodeProvider.URI, _podcastId);
			String[] projection = new String[]{
					EpisodeProvider.COLUMN_ID,
					EpisodeProvider.COLUMN_TITLE,
					EpisodeProvider.COLUMN_PAYMENT,
			};
			Activity activity = getActivity();
			Cursor c = activity.getContentResolver().query(podcastUri, projection, null, null, null);
			if (c == null)
				return null;
			if (c.moveToNext()) {
				EpisodeCursor episode = new EpisodeCursor(c);
				String payment_url = episode.getPaymentUrl();
				if (payment_url != null) {
					AutoSubmission sub = FlattrHelper.parseAutoSubmissionLink(Uri.parse(payment_url));
					if (sub != null) {
						// it's a flattr link
						try {
							FlattrHelper.flattr(activity, sub);
							String message = "You flattred " + episode.getTitle() + "!";
							showToast(message);
						} catch (ForbiddenException e) {
							if (e.getCode().equals("flattr_once")) {
								String message = "Podcast was already flattred";
								showToast(message);
							} else {
								try {
									FlattrHelper.obtainToken(activity);
								} catch (NoAppSecretFlattrException e1) {
									String message = "No flattr app secret in this build.";
									showToast(message);
								}
							}
						} catch (FlattrException e) {
							String message = "Could not flattr: " + e.getMessage();
							showToast(message);
						}

					} else {
						// it's another kind of payment link
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(episode.getPaymentUrl()));
						startActivity(intent);
					}
				}
			}
			c.close();

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
		return inflater.inflate(R.layout.episode_detail, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_subscriptionImage = (ImageView) view.findViewById(R.id.subscription_img);
		_titleView = (TextView) view.findViewById(R.id.title);
		_subscriptionTitleView = (TextView) view.findViewById(R.id.subscription_title);
		_descriptionView = (WebView) view.findViewById(R.id.description);
		_playlistPosition = (TextView) view.findViewById(R.id.playlist_position);
		_playlistButton = (Button) view.findViewById(R.id.playlist_btn);
		View restartButton = view.findViewById(R.id.restart_btn);
		View rewindButton = view.findViewById(R.id.rewind_btn);
		_playButton = (ImageButton) view.findViewById(R.id.play_btn);
		View forwardButton = view.findViewById(R.id.forward_btn);
		View skipToEndButton = view.findViewById(R.id.skiptoend_btn);
		_seekbar = (SeekBar) view.findViewById(R.id.seekbar);
		_position = (TextView) view.findViewById(R.id.position);
		_duration = (TextView) view.findViewById(R.id.duration);
		_paymentButton = (Button) view.findViewById(R.id.payment);
		_viewInBrowserButton = (Button) view.findViewById(R.id.view_in_browser);

		_playButton.setOnClickListener(v -> {
			PlayerStatus playerState = PlayerStatus.getCurrentState(getActivity());
			if (playerState.isPlaying() && playerState.getEpisodeId() == _podcastId) {
				PlayerService.stop(getActivity());
				return;
			}
			PlayerService.play(getActivity(), _podcastId);
		});

		forwardButton.setOnClickListener(v -> EpisodeProvider.movePositionBy(getActivity(), _podcastId, 30));

		skipToEndButton.setOnClickListener(v -> EpisodeProvider.skipToEnd(getActivity(), _podcastId));

		OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				_position.setText(Helper.getTimeString(progress));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = false;
				EpisodeProvider.movePositionTo(getActivity(), _podcastId, seekBar.getProgress());
			}
		};
		_seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

		_playlistButton.setOnClickListener(v -> {
			Uri podcastUri = ContentUris.withAppendedId(EpisodeProvider.URI, _podcastId);
			String[] projection = new String[]{EpisodeProvider.COLUMN_ID, EpisodeProvider.COLUMN_PLAYLIST_POSITION};
			Cursor c = getActivity().getContentResolver().query(podcastUri, projection, null, null, null);
			if (c == null)
				return;
			if (c.moveToNext()) {
				EpisodeCursor episode = new EpisodeCursor(c);
				if (episode.getPlaylistPosition() == null)
					episode.addToPlaylist(getActivity());
				else
					episode.removeFromPlaylist(getActivity());
			}
			c.close();
		});

		restartButton.setOnClickListener(v -> EpisodeProvider.restart(getActivity(), _podcastId));

		rewindButton.setOnClickListener(v -> EpisodeProvider.movePositionBy(getActivity(), _podcastId, -15));
		_paymentButton.setOnClickListener(v -> _flattr_task.execute(_podcastId));

		_viewInBrowserButton.setOnClickListener(v -> {
			Uri uri = (Uri) v.getTag();
			if (uri != null)
				startActivity(new Intent(Intent.ACTION_VIEW, uri));
		});

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

		EpisodeData.getObservable(getActivity(), _podcastId)
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

		_titleView.setText(episode.getTitle());
		_subscriptionTitleView.setText(episode.getSubscriptionTitle());
		SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()).into(_subscriptionImage);

		String description = episode.getDescription();
		if (description != null) {
			int textColor = getResources().getColor(R.color.primary_text_default_material_dark);
			String textColorRgba = String.format(Locale.US, "rgba(%d, %d, %d, %d)",
					Color.red(textColor), Color.green(textColor), Color.blue(textColor), Color.alpha(textColor));

			int bgColor = getResources().getColor(R.color.primary_material_dark);
			String bgColorRgba = String.format(Locale.US, "rgba(%d, %d, %d, %d)",
					Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor), Color.alpha(bgColor));

			int linkColor = getResources().getColor(R.color.orange500);
			String linkColorRgba = String.format(Locale.US, "rgba(%d, %d, %d, %d)",
					Color.red(linkColor), Color.green(linkColor), Color.blue(linkColor), Color.alpha(linkColor));

			description = description.replaceAll("color:", "color,");
			String fullhtml = "<html><head><title></title><style>body{color:" + textColorRgba + ";background:" + bgColorRgba + "} a{color:" + linkColorRgba + "}</style></head><body>" + description + "</body></html>";

			_descriptionView.setBackgroundColor(bgColor);
			_descriptionView.getSettings().setDefaultTextEncodingName("utf-8");
			_descriptionView.loadData(fullhtml, "text/html; charset=utf-8", null);
		}

		_position.setText(Helper.getTimeString(episode.getLastPosition()));
		if (episode.getDuration() != 0) {
			_seekbar.setMax(episode.getDuration());
			_seekbar.setProgress(episode.getLastPosition());
			_seekbar.setEnabled(true);
			_duration.setText("-" + Helper.getTimeString(episode.getDuration() - episode.getLastPosition()));
		} else {
			_seekbar.setEnabled(false);
			_duration.setText("");
		}

		String payment_url = episode.getPaymentUrl();
		if (payment_url != null) {
			_paymentButton.setVisibility(View.VISIBLE);
			AutoSubmission sub = FlattrHelper.parseAutoSubmissionLink(Uri.parse(payment_url));
			if (sub == null) {
				_paymentButton.setText(R.string.donate);
			} else {
				_paymentButton.setText(R.string.flattr);
			}
		} else {
			_paymentButton.setVisibility(View.GONE);
		}

		String link = episode.getLink();
		if (link == null)
			_viewInBrowserButton.setVisibility(View.GONE);
		else {
			Uri linkUri = Uri.parse(link);
			if (linkUri.getScheme() != null) {
				_viewInBrowserButton.setTag(linkUri);
				_viewInBrowserButton.setVisibility(View.VISIBLE);
			} else {
				_viewInBrowserButton.setVisibility(View.GONE);
			}
		}

		// playlist position button
		if (episode.getPlaylistPosition() == null) {
			_playlistButton.setText(R.string.add);
			_playlistPosition.setText("");
		} else {
			_playlistButton.setText(R.string.remove);
			_playlistPosition.setText("#"
                    + String.valueOf(episode.getPlaylistPosition() + 1)
                    + " in playlist");
		}
	}

	private void updateControls(PlayerStatus status) {
		if (status == null)
			return;

		if (status.getEpisodeId() != _podcastId) {

		}

		if (status.getDuration() == 0) {
			_position.setText(Helper.getTimeString(status.getPosition()));
			_duration.setText("");
			_seekbar.setEnabled(false);
		} else if (!_seekbar_dragging) {
			_position.setText(Helper.getTimeString(status.getPosition()));
			_duration.setText("-" + Helper.getTimeString(status.getDuration() - status.getPosition()));
			_seekbar.setProgress(status.getPosition());
			_seekbar.setMax(status.getDuration());
			_seekbar.setEnabled(true);
		}

		boolean isPlaying = status.isPlaying() && status.getEpisodeId() == _podcastId;
		int playResource = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
		_playButton.setImageResource(playResource);

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
