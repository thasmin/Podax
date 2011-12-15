package com.axelby.podax;

import android.app.Activity;
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

		try {
			retrievePodcast();
			updateUI();
		} catch (MissingFieldException e) {
			e.printStackTrace();
		}
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.player, this);
		
		if (isInEditMode())
			return;
		loadViews(context);

		try {
			retrievePodcast();
			updateUI();
		} catch (MissingFieldException e) {
			e.printStackTrace();
		}
	}

	private Long _lastPodcastId = null;
	private Handler _handler = new Handler();
	
	private class ActivePodcastObserver extends ContentObserver {
		public ActivePodcastObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			_cursor.requery();
			_podcast = new PodcastCursor(getContext(), _cursor);
			try {
				updateUI();
			} catch (MissingFieldException e) {
				e.printStackTrace();
			}
		}
	}
	private ActivePodcastObserver _observer = new ActivePodcastObserver(_handler);

	public void updateUI() throws MissingFieldException {
		boolean isPlaying = PlayerService.isPlaying();
		_pausebtn.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

		if (!_podcast.isNull()) {
			if (isPlaying || _positionstring.getText().length() == 0)
				_positionstring.setText(PlayerService.getPositionString(_podcast.getDuration(), _podcast.getLastPosition()));
			if (_lastPodcastId != _podcast.getId()) {
				_podcastTitle.setText(_podcast.getTitle());
				_showplayerbtn.setEnabled(true);
			}
		} else if (_lastPodcastId != null) {
			_podcastTitle.setText("");
			_positionstring.setText("");
			_showplayerbtn.setEnabled(false);
		}

		_lastPodcastId = _podcast.isNull() ? null : _podcast.getId();
	}

	public void retrievePodcast() throws MissingFieldException {
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Uri activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
		_cursor = ((Activity)getContext()).managedQuery(activeUri, projection, null, null, null);
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
				PodaxApp.getApp().playpause();
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
