package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.FlattrHelper;
import com.axelby.podax.FlattrHelper.NoAppSecretFlattrException;
import com.axelby.podax.Helper;
import com.axelby.podax.IgnoreTagHandler;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.URLImageGetter;

import org.shredzone.flattr4j.exception.FlattrException;
import org.shredzone.flattr4j.exception.ForbiddenException;
import org.shredzone.flattr4j.model.AutoSubmission;

public class EpisodeDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int CURSOR_PODCAST = 1;
	private static final int CURSOR_ACTIVE = 2;

	private long _podcastId;
	private boolean _uiInitialized = false;

	private ImageView _subscriptionImage;
	private TextView _titleView;
	private TextView _subscriptionTitleView;
	private TextView _descriptionView;
	private Button _playlistButton;
	private TextView _playlistPosition;
	private ImageButton _playButton;
	private SeekBar _seekbar;
	private boolean _seekbar_dragging = false;
	private Button _paymentButton;
	private TextView _position;
	private TextView _duration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_EPISODE_ID))
			getLoaderManager().initLoader(CURSOR_PODCAST, getArguments(), this);
		else
			getLoaderManager().initLoader(CURSOR_ACTIVE, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.episode_detail, container, false);
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
		_subscriptionImage = (ImageView) activity.findViewById(R.id.subscription_img);
		_titleView = (TextView) activity.findViewById(R.id.title);
		_subscriptionTitleView = (TextView) activity.findViewById(R.id.subscription_title);
		_descriptionView = (TextView) activity.findViewById(R.id.description);
		_playlistPosition = (TextView) activity.findViewById(R.id.playlist_position);
		_playlistButton = (Button) activity.findViewById(R.id.playlist_btn);
		View restartButton = activity.findViewById(R.id.restart_btn);
		View rewindButton = activity.findViewById(R.id.rewind_btn);
		_playButton = (ImageButton) activity.findViewById(R.id.play_btn);
		View forwardButton = activity.findViewById(R.id.forward_btn);
		View skipToEndButton = activity.findViewById(R.id.skiptoend_btn);
		_seekbar = (SeekBar) activity.findViewById(R.id.seekbar);
		_position = (TextView) activity.findViewById(R.id.position);
		_duration = (TextView) activity.findViewById(R.id.duration);
		_paymentButton = (Button) activity.findViewById(R.id.payment);

		_playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerStatus playerState = PlayerStatus.getCurrentState(activity);
				if (playerState.isPlaying() && playerState.getEpisodeId() == _podcastId) {
					PlayerService.stop(activity);
					return;
				}
				PlayerService.play(activity, _podcastId);
			}
		});

		forwardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EpisodeProvider.movePositionBy(activity, _podcastId, 30);
			}
		});

		skipToEndButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EpisodeProvider.skipToEnd(activity, _podcastId);
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
				EpisodeProvider.movePositionTo(activity, _podcastId, seekBar.getProgress());
			}
		});

		_playlistButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Uri podcastUri = ContentUris.withAppendedId(EpisodeProvider.URI, _podcastId);
                String[] projection = new String[]{EpisodeProvider.COLUMN_ID, EpisodeProvider.COLUMN_PLAYLIST_POSITION};
                Cursor c = activity.getContentResolver().query(podcastUri, projection, null, null, null);
                if (c == null)
                    return;
                if (c.moveToNext()) {
                    EpisodeCursor episode = new EpisodeCursor(c);
                    if (episode.getPlaylistPosition() == null)
                        episode.addToPlaylist(activity);
                    else
                        episode.removeFromPlaylist(activity);
                }
                c.close();
            }
        });

		restartButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EpisodeProvider.restart(activity, _podcastId);
			}
		});

		rewindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EpisodeProvider.movePositionBy(activity, _podcastId, -15);
			}
		});
		_paymentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new AsyncTask<Long, Void, Void>() {
					@Override
					protected Void doInBackground(Long... params) {
						Uri podcastUri = ContentUris.withAppendedId(EpisodeProvider.URI, _podcastId);
						String[] projection = new String[]{
								EpisodeProvider.COLUMN_ID,
								EpisodeProvider.COLUMN_TITLE,
								EpisodeProvider.COLUMN_PAYMENT,
						};
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
									intent.setData(Uri.parse(episode.getPaymentUrl()));
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

	private void initializeUI(EpisodeCursor episode) {
		_titleView.setText(episode.getTitle());
		_subscriptionTitleView.setText(episode.getSubscriptionTitle());
		Bitmap subscriptionThumbnail = SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId());
		_subscriptionImage.setImageBitmap(subscriptionThumbnail);

		if (episode.getDescription() != null)
			_descriptionView.setText(Html.fromHtml(episode.getDescription(), new URLImageGetter(_descriptionView), new IgnoreTagHandler()));

		_seekbar.setMax(episode.getDuration());
		_seekbar.setProgress(episode.getLastPosition());

		_position.setText(Helper.getTimeString(episode.getLastPosition()));
		_duration.setText("-" + Helper.getTimeString(episode.getDuration() - episode.getLastPosition()));

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
	}

	private void updateControls(EpisodeCursor episode) {
		if (!_seekbar_dragging) {
			_position.setText(Helper.getTimeString(episode.getLastPosition()));
			_duration.setText("-" + Helper.getTimeString(episode.getDuration() - episode.getLastPosition()));
			_seekbar.setProgress(episode.getLastPosition());
		}

		PlayerStatus status = PlayerStatus.getCurrentState(getActivity());
		boolean isPlaying = status.isPlaying() && status.getEpisodeId() == _podcastId;
		int playResource = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
		_playButton.setImageResource(playResource);

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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[]{
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
				EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
				EpisodeProvider.COLUMN_DESCRIPTION,
				EpisodeProvider.COLUMN_DURATION,
				EpisodeProvider.COLUMN_LAST_POSITION,
				EpisodeProvider.COLUMN_PLAYLIST_POSITION,
				EpisodeProvider.COLUMN_MEDIA_URL,
				EpisodeProvider.COLUMN_PAYMENT,
		};

		if (id == CURSOR_PODCAST && args != null && args.containsKey(Constants.EXTRA_EPISODE_ID)) {
			_podcastId = args.getLong(Constants.EXTRA_EPISODE_ID);
			Uri uri = ContentUris.withAppendedId(EpisodeProvider.URI, _podcastId);
			return new CursorLoader(getActivity(), uri, projection, null, null, null);
		} else if (id == CURSOR_ACTIVE) {
			return new CursorLoader(getActivity(), EpisodeProvider.ACTIVE_EPISODE_URI, projection, null, null, null);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		if (!cursor.moveToFirst()) {
            _uiInitialized = true;
            return;
        }

		EpisodeCursor episode = new EpisodeCursor(cursor);
		if (episode.getId() != _podcastId || !_uiInitialized) {
			initializeUI(episode);
			_uiInitialized = true;
		}
		_podcastId = episode.getId();
		updateControls(episode);
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
