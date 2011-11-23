package com.axelby.podax;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;


public class BottomBar extends LinearLayout {

	private TextView _podcastTitle;
	private TextView _positionstring;
	private ImageButton _pausebtn;
	private ImageButton _showplayerbtn;

	private Cursor _cursor = null;
	private PodcastCursor _podcast;

	public BottomBar(Context context) {
		super(context);
		
		if (isInEditMode())
			return;
		loadViews(context);
		setupHandler();
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.player, this);
		
		loadViews(context);
		setupHandler();
	}

	private Long _lastPodcastId = null;
	private Handler _handler = new Handler();
	
	private class ActivePodcastObserver extends ContentObserver {
		public ActivePodcastObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			_podcast.unregisterContentObserver(_observer);
			setupHandler();
		}
	}
	private ActivePodcastObserver _observer = new ActivePodcastObserver(_handler);

	private void setupHandler() {
		try {
			retrievePocast();
			updateUI();
		} catch (MissingFieldException e) {
			e.printStackTrace();
		} finally {
			if (_cursor != null)
				_cursor.close();
		}
	}

	public void updateUI() throws MissingFieldException {
		boolean isPlaying = PlayerService.isPlaying();
		if (!_podcast.isNull()) {
			if (isPlaying || _positionstring.getText().length() == 0)
				_positionstring.setText(PlayerService.getPositionString(_podcast.getDuration(), _podcast.getLastPosition()));
			if (_lastPodcastId != _podcast.getId()) {
				_podcastTitle.setText(_podcast.getTitle());
				_pausebtn.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
				_showplayerbtn.setEnabled(true);
			}
		} else if (_lastPodcastId != null) {
			_podcastTitle.setText("");
			_positionstring.setText("");
			_showplayerbtn.setEnabled(false);
		}

		_lastPodcastId = _podcast.isNull() ? null : _podcast.getId();
	}

	public void retrievePocast() throws MissingFieldException {
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Uri activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
		_cursor = getContext().getContentResolver().query(activeUri, projection, null, null, null);
		_podcast = new PodcastCursor(getContext(), _cursor);
		getContext().getContentResolver().registerContentObserver(activeUri, false, _observer);
	}

	private void loadViews(final Context context) {
		_podcastTitle = (TextView) findViewById(R.id.podcasttitle);
		_positionstring = (TextView) findViewById(R.id.positionstring);
		_pausebtn = (ImageButton) findViewById(R.id.pausebtn);
		_showplayerbtn = (ImageButton) findViewById(R.id.showplayer);
		
		_podcastTitle.setText("");
		_positionstring.setText("");
		_showplayerbtn.setEnabled(false);
		
		_pausebtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				PodaxApp app = (PodaxApp) context.getApplicationContext();
				app.playpause();
				setupHandler();
			}
		});
		
		_showplayerbtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(context, PodcastDetailActivity.class);
				context.startActivity(intent);
			}
		});
	}

}
