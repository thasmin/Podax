package com.axelby.podax;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PodcastDetailActivity extends Activity {
	Cursor _cursor;
	PodcastCursor _podcast;
	long _podcastId;
	Handler _handler = new Handler();

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
	boolean _seekbar_dragging;

	TextView _position;
	TextView _duration;

	class PodcastObserver extends ContentObserver {
		public PodcastObserver(Handler handler) {
			super(handler);
		}
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			_cursor.requery();
			_podcast = new PodcastCursor(PodcastDetailActivity.this, _cursor);
			updateQueueViews();
			updatePlayerControls(false);
		}
	}

	String[] _projection = new String[] {
			PodcastProvider.COLUMN_ID,
			PodcastProvider.COLUMN_TITLE,
			PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
			PodcastProvider.COLUMN_DESCRIPTION,
			PodcastProvider.COLUMN_DURATION,
			PodcastProvider.COLUMN_LAST_POSITION,
			PodcastProvider.COLUMN_QUEUE_POSITION,
			PodcastProvider.COLUMN_MEDIA_URL,
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();
		if (intent.hasExtra(Constants.EXTRA_PODCAST_ID)) {
			int podcastId = intent.getIntExtra(Constants.EXTRA_PODCAST_ID, -1);
			if (podcastId == -1) {
				finish();
				return;
			}
			Uri uri = ContentUris.withAppendedId(PodcastProvider.URI, podcastId);
			_cursor = getContentResolver().query(uri, _projection, null, null, null);
		} else {
			Uri uri = Uri.withAppendedPath(PodcastProvider.URI, "active");
			_cursor = managedQuery(uri, _projection, null, null, null);
			// if the queue is empty, don't show this
			if (_cursor.isAfterLast()) {
				finish();
				startActivity(new Intent(this, QueueActivity.class));
				return;
			}
		}

		_podcast = new PodcastCursor(this, _cursor);
		_podcastId = _podcast.getId();
		_observer = new PodcastObserver(_handler);
		_podcast.registerContentObserver(_observer);

        setTitle(_podcast.getTitle());
        setContentView(R.layout.podcast_detail);

		_subscriptionTitleView = (TextView)findViewById(R.id.subscription_title);
		_subscriptionTitleView.setText(_podcast.getSubscriptionTitle());

		_descriptionView = (WebView)findViewById(R.id.description);
		String html = "<html><head><style type=\"text/css\">" +
				"a { color: #E59F39 }" +
				"</style></head>" +
				"<body style=\"background:black;color:white\">" + _podcast.getDescription() + "</body></html>"; 
		_descriptionView.loadData(html, "text/html", "utf-8");
		_descriptionView.setBackgroundColor(Color.BLACK);

		_queuePosition = (TextView)findViewById(R.id.queue_position);
		_queueButton = (Button)findViewById(R.id.queue_btn);
		updateQueueViews();
		_queueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_podcast.getQueuePosition() == null)
					_podcast.addToQueue();
				else
					_podcast.removeFromQueue();
			}
		});

		_restartButton = (ImageButton)findViewById(R.id.restart_btn);
		_restartButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.restart(PodcastDetailActivity.this);
			}
		});

		_rewindButton = (ImageButton)findViewById(R.id.rewind_btn);
		_rewindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.skipBack(PodcastDetailActivity.this);
			}
		});

		_playButton = (ImageButton)findViewById(R.id.play_btn);
		_playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Long activeId = null;
				String[] projection = new String[] { PodcastProvider.COLUMN_ID };
				Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
				try {
					if (c.moveToNext())
						activeId = c.getLong(0);

					if (PlayerService.isPlaying() && activeId != null && activeId.equals(_podcast.getId()))
						PlayerService.pause(PodcastDetailActivity.this);
					else
						PlayerService.play(PodcastDetailActivity.this, _podcast);
				} finally {
					c.close();
				}
			}
		});

		_forwardButton = (ImageButton)findViewById(R.id.forward_btn);
		_forwardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.skipForward(PodcastDetailActivity.this);
			}
		});

		_skipToEndButton = (ImageButton)findViewById(R.id.skiptoend_btn);
		_skipToEndButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PlayerService.skipToEnd(PodcastDetailActivity.this);
			}
		});

		_seekbar = (SeekBar)findViewById(R.id.seekbar);
		_seekbar.setMax(_podcast.getDuration());
		_seekbar.setProgress(_podcast.getLastPosition());
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
				PlayerService.skipTo(PodcastDetailActivity.this, seekBar.getProgress() / 1000);
			}
		});
		_seekbar_dragging = false;

		_position = (TextView)findViewById(R.id.position);
		_position.setText(Helper.getTimeString(_podcast.getLastPosition()));
		_duration = (TextView)findViewById(R.id.duration);
		_duration.setText(Helper.getTimeString(_podcast.getDuration()));

		updatePlayerControls(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		_podcast.unregisterContentObserver(_observer);
		_cursor.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Uri uri = ContentUris.withAppendedId(PodcastProvider.URI, _podcastId);
		_cursor = getContentResolver().query(uri, _projection, null, null, null);
		_podcast = new PodcastCursor(this, _cursor);
		_podcast.registerContentObserver(_observer);
	}

	boolean _controlsEnabled = true;
	private PodcastObserver _observer;
	private void updatePlayerControls(boolean force) {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_DURATION,
		};
		Cursor c = getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		try {
			PodcastCursor p = new PodcastCursor(this, c);

			if (!p.isNull() && p.getId().equals(_podcast.getId())) {
				if (!_seekbar_dragging) {
					_position.setText(Helper.getTimeString(p.getLastPosition()));
					_duration.setText(Helper.getTimeString(p.getDuration()));
					_seekbar.setProgress(p.getLastPosition());
				}

				int playResource = PlayerService.isPlaying() ? R.drawable.ic_media_pause
						: R.drawable.ic_media_play;
				_playButton.setImageResource(playResource);

				if (!force && _controlsEnabled == true)
					return;

				_restartButton.setEnabled(true);
				_rewindButton.setEnabled(true);
				_forwardButton.setEnabled(true);
				_skipToEndButton.setEnabled(true);
				_seekbar.setEnabled(true);

				_controlsEnabled = true;
			} else {
				if (!force && !_controlsEnabled)
					return;

				_playButton.setImageResource(R.drawable.ic_media_play);
				_restartButton.setEnabled(false);
				_rewindButton.setEnabled(false);
				_forwardButton.setEnabled(false);
				_skipToEndButton.setEnabled(false);
				_seekbar.setEnabled(false);

				_controlsEnabled = false;
			}
		} finally {
			c.close();
		}
	}

	private void updateQueueViews() {
		if (_podcast.getQueuePosition() == null) {
			_queueButton.setText(R.string.add_to_queue);
			_queuePosition.setText("");
		} else {
			_queueButton.setText(R.string.remove_from_queue);
			_queuePosition.setText("#"
					+ String.valueOf(_podcast.getQueuePosition() + 1)
					+ " in queue");
		}
	}
}
