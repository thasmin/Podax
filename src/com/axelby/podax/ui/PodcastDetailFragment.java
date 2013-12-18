package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.axelby.podax.Constants;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.FlattrHelper.NoAppSecretFlattrException;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.ForbiddenException;
import org.shredzone.flattr4j.model.AutoSubmission;

public class PodcastDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int CURSOR_PODCAST = 1;
	private static final int CURSOR_ACTIVE = 2;
	long _podcastId;
	boolean _uiInitialized = false;
	NetworkImageView _subscriptionImage;
	TextView _titleView;
	TextView _subscriptionTitleView;
	WebView _descriptionView;
	Button _queueButton;
	TextView _queuePosition;
	ImageButton _restartButton;
	ImageButton _rewindButton;
	ImageButton _playButton;
	ImageButton _forwardButton;
	ImageButton _skipToEndButton;
	SeekBar _seekbar;
	boolean _seekbar_dragging = false;
	Button _paymentButton;
	TextView _position;
	TextView _duration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_PODCAST_ID))
			getLoaderManager().initLoader(CURSOR_PODCAST, getArguments(), this);
		else
			getLoaderManager().initLoader(CURSOR_ACTIVE, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.podcast_detail, container, false);
	}

	private void showToast(final Activity activity, final String message) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Activity activity = getActivity();
		_subscriptionImage = (NetworkImageView) activity.findViewById(R.id.subscription_img);
		_titleView = (TextView) activity.findViewById(R.id.title);
		_subscriptionTitleView = (TextView) activity.findViewById(R.id.subscription_title);
		_descriptionView = (WebView) activity.findViewById(R.id.description);
		_queuePosition = (TextView) activity.findViewById(R.id.queue_position);
		_queueButton = (Button) activity.findViewById(R.id.queue_btn);
		_restartButton = (ImageButton) activity.findViewById(R.id.restart_btn);
		_rewindButton = (ImageButton) activity.findViewById(R.id.rewind_btn);
		_playButton = (ImageButton) activity.findViewById(R.id.play_btn);
		_forwardButton = (ImageButton) activity.findViewById(R.id.forward_btn);
		_skipToEndButton = (ImageButton) activity.findViewById(R.id.skiptoend_btn);
		_seekbar = (SeekBar) activity.findViewById(R.id.seekbar);
		_position = (TextView) activity.findViewById(R.id.position);
		_duration = (TextView) activity.findViewById(R.id.duration);
		_paymentButton = (Button) activity.findViewById(R.id.payment);

		_playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerStatus playerState = PlayerStatus.getCurrentState(activity);
				if (playerState.isPlaying() && playerState.getPodcastId() == _podcastId)
					PlayerService.stop(activity);
				else {
					ContentValues values = new ContentValues();
					values.put(PodcastProvider.COLUMN_ID, _podcastId);
					activity.getContentResolver().update(PodcastProvider.ACTIVE_PODCAST_URI, values, null, null);
					PlayerService.play(activity);
				}
			}
		});

		_forwardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PodcastProvider.movePositionBy(activity, _podcastId, 30);
			}
		});

		_skipToEndButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PodcastProvider.skipToEnd(activity, _podcastId);
			}
		});

		_seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				_position.setText(Helper.getTimeString(progress));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = false;
				PodcastProvider.movePositionTo(activity, _podcastId, seekBar.getProgress());
			}
		});

		_queueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
				String[] projection = new String[]{PodcastProvider.COLUMN_ID, PodcastProvider.COLUMN_QUEUE_POSITION};
				Cursor c = activity.getContentResolver().query(podcastUri, projection, null, null, null);
				if (c == null)
					return;
				if (c.moveToNext()) {
					PodcastCursor podcast = new PodcastCursor(c);
					if (podcast.getQueuePosition() == null)
						podcast.addToQueue(activity);
					else
						podcast.removeFromQueue(activity);
				}
				c.close();
			}
		});

		_restartButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PodcastProvider.restart(activity, _podcastId);
			}
		});

		_rewindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PodcastProvider.movePositionBy(activity, _podcastId, -15);
			}
		});
		_paymentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new AsyncTask<Long, Void, Void>() {
					@Override
					protected Void doInBackground(Long... params) {
						Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
						String[] projection = new String[]{
								PodcastProvider.COLUMN_ID,
								PodcastProvider.COLUMN_TITLE,
								PodcastProvider.COLUMN_PAYMENT,
						};
						Cursor c = activity.getContentResolver().query(podcastUri, projection, null, null, null);
						if (c == null)
							return null;
						if (c.moveToNext()) {
							PodcastCursor podcast = new PodcastCursor(c);
							String payment_url = podcast.getPaymentUrl();
							if (payment_url != null) {
								AutoSubmission sub = FlattrHelper.parseAutoSubmissionLink(Uri.parse(payment_url));
								if (sub != null) {
									// it's a flattr link
									try {
										FlattrHelper.flattr(activity, sub);
										String message = "You flattred " + podcast.getTitle() + "!";
										showToast(activity, message);
									} catch (ForbiddenException e) {
										if (e.getCode().equals("flattr_once")) {
											String message = "Podcast was already flattred";
											showToast(activity, message);
										} else {
											try {
												FlattrHelper.obtainToken(activity);
											} catch (NoAppSecretFlattrException e1) {
												String message = "No flattr app secret in this build.";
												showToast(activity, message);
											}
										}
									} catch (FlattrException e) {
										String message = "Could not flattr: " + e.getMessage();
										showToast(activity, message);
									}

								} else {
									// it's another kind of payment link
									Intent intent = new Intent(Intent.ACTION_VIEW);
									intent.setData(Uri.parse(podcast.getPaymentUrl()));
									startActivity(intent);
								}
							}
						}
						c.close();

						return null;
					}
				}.execute(_podcastId);
			}
		});

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		_uiInitialized = false;
	}

	private void initializeUI(PodcastCursor podcast) {
		String url = podcast.getSubscriptionThumbnailUrl();
		if (url != null)
			_subscriptionImage.setImageUrl(podcast.getSubscriptionThumbnailUrl(), Helper.getImageLoader(getActivity()));
		_titleView.setText(podcast.getTitle());
		_subscriptionTitleView.setText(podcast.getSubscriptionTitle());

		String html = "<html><head><style type=\"text/css\">" +
				"a { color: #E59F39 }" +
				"</style></head>" +
				"<body style=\"color:white\">" + podcast.getDescription() + "</body></html>";
		_descriptionView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

		_seekbar.setMax(podcast.getDuration());
		_seekbar.setProgress(podcast.getLastPosition());

		_position.setText(Helper.getTimeString(podcast.getLastPosition()));
		_duration.setText("-" + Helper.getTimeString(podcast.getDuration() - podcast.getLastPosition()));

		String payment_url = podcast.getPaymentUrl();
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
	}

	private void updateControls(PodcastCursor podcast) {
		if (!_seekbar_dragging) {
			_position.setText(Helper.getTimeString(podcast.getLastPosition()));
			_duration.setText("-" + Helper.getTimeString(podcast.getDuration() - podcast.getLastPosition()));
			_seekbar.setProgress(podcast.getLastPosition());
		}

		PlayerStatus status = PlayerStatus.getCurrentState(getActivity());
		boolean isPlaying = status.isPlaying() && status.getPodcastId() == _podcastId;
		int playResource = isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
		_playButton.setImageResource(playResource);

		if (podcast.getQueuePosition() == null) {
			_queueButton.setText(R.string.add);
			_queuePosition.setText("");
		} else {
			_queueButton.setText(R.string.remove);
			_queuePosition.setText("#"
					+ String.valueOf(podcast.getQueuePosition() + 1)
					+ " in queue");
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[]{
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
				PodcastProvider.COLUMN_DESCRIPTION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_QUEUE_POSITION,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_PAYMENT,
		};

		if (id == CURSOR_PODCAST && args != null && args.containsKey(Constants.EXTRA_PODCAST_ID)) {
			_podcastId = args.getLong(Constants.EXTRA_PODCAST_ID);
			Uri uri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
			return new CursorLoader(getActivity(), uri, projection, null, null, null);
		} else if (id == CURSOR_ACTIVE) {
			return new CursorLoader(getActivity(), PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		if (!cursor.moveToFirst())
			return;

		PodcastCursor podcast = new PodcastCursor(cursor);
		if (podcast.getId() != _podcastId || !_uiInitialized) {
			initializeUI(podcast);
			_uiInitialized = true;
		}
		_podcastId = podcast.getId();
		updateControls(podcast);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
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
