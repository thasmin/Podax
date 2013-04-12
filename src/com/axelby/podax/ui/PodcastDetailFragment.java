package com.axelby.podax.ui;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.ForbiddenException;
import org.shredzone.flattr4j.model.AutoSubmission;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
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

import com.actionbarsherlock.app.SherlockFragment;
import com.axelby.podax.Constants;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.FlattrHelper.NoAppSecretFlattrException;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodaxLog;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class PodcastDetailFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	long _podcastId;
	boolean _controlsEnabled = true;
	Long _initializedPodcastId = null;

	private static final int CURSOR_PODCAST = 1;
	private static final int CURSOR_ACTIVE = 2;

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

		_podcastId = getActivity().getIntent().getLongExtra(Constants.EXTRA_PODCAST_ID, 0);
		if (_podcastId != 0)
			getLoaderManager().initLoader(CURSOR_PODCAST, null, this);
		else
			getLoaderManager().initLoader(CURSOR_ACTIVE, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.podcast_detail, null, false);
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

		_titleView = (TextView) getActivity().findViewById(R.id.title);
		_subscriptionTitleView = (TextView) getActivity().findViewById(R.id.subscription_title);
		_descriptionView = (WebView) getActivity().findViewById(R.id.description);
		_queuePosition = (TextView) getActivity().findViewById(R.id.queue_position);
		_queueButton = (Button) getActivity().findViewById(R.id.queue_btn);
		_restartButton = (ImageButton) getActivity().findViewById(R.id.restart_btn);
		_rewindButton = (ImageButton) getActivity().findViewById(R.id.rewind_btn);
		_playButton = (ImageButton) getActivity().findViewById(R.id.play_btn);
		_forwardButton = (ImageButton) getActivity().findViewById(R.id.forward_btn);
		_skipToEndButton = (ImageButton) getActivity().findViewById(R.id.skiptoend_btn);
		_seekbar = (SeekBar) getActivity().findViewById(R.id.seekbar);
		_position = (TextView) getActivity().findViewById(R.id.position);
		_duration = (TextView) getActivity().findViewById(R.id.duration);
		_paymentButton = (Button) getActivity().findViewById(R.id.payment);

		_playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerStatus playerState = PlayerStatus.getCurrentState(getActivity());
				if (playerState.isPlaying() && playerState.getId() == _podcastId)
					PlayerService.stop(getActivity());
				else {
					PodaxLog.log(getActivity(), "playing a specific podcast from podcastdetailfragment");
					PlayerService.play(getActivity(), _podcastId);
				}
			}
		});

		_forwardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.skipForward(getActivity());
			}
		});

		_skipToEndButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.skipToEnd(getActivity());
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
				PlayerService.skipTo(getActivity(), seekBar.getProgress() / 1000);
			}
		});

		_queueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new AsyncTask<Long, Void, Void>() {
					@Override
					protected Void doInBackground(Long... params) {
						Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
						String[] projection = new String[] { PodcastProvider.COLUMN_ID, PodcastProvider.COLUMN_QUEUE_POSITION };
						Cursor c = getActivity().getContentResolver().query(podcastUri, projection, null, null, null);
						
						if (c.moveToNext()) {
							PodcastCursor podcast = new PodcastCursor(c);
							if (podcast.getQueuePosition() == null)
								podcast.addToQueue(getActivity());
							else
								podcast.removeFromQueue(getActivity());
						}
						c.close();

						return null;
					}
				}.execute(_podcastId);
			}
		});

		_restartButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.restart(getActivity());
			}
		});

		_rewindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.skipBack(getActivity());
			}
		});
		_paymentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new AsyncTask<Long, Void, Void>() {
					@Override
					protected Void doInBackground(Long... params) {
						Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
						String[] projection = new String[] {
								PodcastProvider.COLUMN_ID,
								PodcastProvider.COLUMN_TITLE,
								PodcastProvider.COLUMN_PAYMENT,
						};
						Cursor c = getActivity().getContentResolver().query(podcastUri, projection, null, null, null);
						if (c.moveToNext()) {
							PodcastCursor podcast = new PodcastCursor(c);
							String payment_url = podcast.getPaymentUrl();
							if (payment_url != null) {
								AutoSubmission sub = FlattrHelper.parseAutoSubmissionLink(Uri.parse(payment_url));
								if (sub != null) {
									// it's a flattr link
									try {
										FlattrHelper.flattr(getActivity(), sub);
										String message = "You flattred " + podcast.getTitle() + "!";
										showToast(getActivity(), message);
									} catch (ForbiddenException e) {
										if (e.getCode().equals("flattr_once")) {
											String message = "Podcast was already flattred";
											showToast(getActivity(), message);
										} else {
											try {
												FlattrHelper.obtainToken(getActivity());
											} catch (NoAppSecretFlattrException e1) {
												String message = "No flattr app secret in this build.";
												showToast(getActivity(), message);
											}
										}
									} catch (FlattrException e) {
										String message = "Could not flattr: " + e.getMessage();
										showToast(getActivity(), message);
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

	private void initializeUI(PodcastCursor podcast) {
		_titleView.setText(podcast.getTitle());
		_subscriptionTitleView.setText(podcast.getSubscriptionTitle());

		String html = "<html><head><style type=\"text/css\">" +
				"a { color: #E59F39 }" +
				"</style></head>" +
				"<body style=\"background:transprent;color:white\">" + podcast.getDescription() + "</body></html>"; 
		_descriptionView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
		_descriptionView.setBackgroundColor(Color.TRANSPARENT);

		_seekbar.setMax(podcast.getDuration());
		_seekbar.setProgress(podcast.getLastPosition());

		_position.setText(Helper.getTimeString(podcast.getLastPosition()));
		_duration.setText("-" + Helper.getTimeString(podcast.getDuration() - podcast.getLastPosition()));
		
		String payment_url = podcast.getPaymentUrl();
		if (payment_url != null) {
			_paymentButton.setVisibility(View.VISIBLE);
			Log.d("Podax", payment_url);
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

	private void updatePlayerControls(PlayerStatus playerState, PodcastCursor podcast) {
		if (playerState.hasActivePodcast() && playerState.getId() == podcast.getId()) {
			if (!_seekbar_dragging) {
				_position.setText(Helper.getTimeString(podcast.getLastPosition()));
				_duration.setText("-" + Helper.getTimeString(podcast.getDuration() - podcast.getLastPosition()));
				_seekbar.setProgress(podcast.getLastPosition());
			}

			int playResource = playerState.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
			_playButton.setImageResource(playResource);

			if (_controlsEnabled == true)
				return;

			_restartButton.setEnabled(true);
			_rewindButton.setEnabled(true);
			_forwardButton.setEnabled(true);
			_skipToEndButton.setEnabled(true);
			_seekbar.setEnabled(true);

			_controlsEnabled = true;
		} else {
			if (!_controlsEnabled)
				return;

			_playButton.setImageResource(R.drawable.ic_media_play);
			_restartButton.setEnabled(false);
			_rewindButton.setEnabled(false);
			_forwardButton.setEnabled(false);
			_skipToEndButton.setEnabled(false);
			_seekbar.setEnabled(false);

			_controlsEnabled = false;
		}
	}

	private void updateQueueViews(PodcastCursor podcast) {
		if (podcast.getQueuePosition() == null) {
			_queueButton.setText(R.string.add_to_queue);
			_queuePosition.setText("");
		} else {
			_queueButton.setText(R.string.remove_from_queue);
			_queuePosition.setText("#"
					+ String.valueOf(podcast.getQueuePosition() + 1)
					+ " in queue");
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_DESCRIPTION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_QUEUE_POSITION,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_PAYMENT,
		};

		if (id == CURSOR_PODCAST) {
			if (_podcastId != 0) {
				Uri uri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
				return new CursorLoader(getActivity(), uri, projection, null, null, null);
			}
		} else if (id == CURSOR_ACTIVE) {
			return new CursorLoader(getActivity(), PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		if (!cursor.moveToNext()) {
			getActivity().finish();
			return;
		}

		PodcastCursor podcast = new PodcastCursor(cursor);
		_podcastId = podcast.getId();
		if (_initializedPodcastId == null || !_initializedPodcastId.equals(_podcastId)) {
			initializeUI(podcast);
			_initializedPodcastId = _podcastId;
		}
		updateQueueViews(podcast);
		updatePlayerControls(PlayerStatus.getCurrentState(getActivity()), podcast);
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
