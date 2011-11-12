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

	public BottomBar(Context context) {
		super(context);
		
		loadViews(context);
		setupHandler(context);
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.player, this);
		
		loadViews(context);
		setupHandler(context);
	}

	private Long _lastPodcastId = null;
	private Handler _handler = new Handler();

	private void setupHandler(final Context context) {
		Cursor cursor = null;
		try {
			boolean isPlaying = PlayerService.isPlaying();
			String[] projection = { 
					PodcastProvider.COLUMN_ID,
					PodcastProvider.COLUMN_TITLE,
					PodcastProvider.COLUMN_DURATION,
					PodcastProvider.COLUMN_LAST_POSITION,
			};
			Uri activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
			cursor = context.getContentResolver().query(activeUri, projection, null, null, null);
			PodcastCursor podcast = new PodcastCursor(context, cursor);
			podcast.registerContentObserver(new ContentObserver(_handler) {
				@Override
				public void onChange(boolean selfChange) {
					setupHandler(context);
				}
			});

			if (!podcast.isNull()) {
				if (isPlaying || _positionstring.getText().length() == 0)
					_positionstring.setText(PlayerService.getPositionString(podcast.getDuration(), podcast.getLastPosition()));
				if (_lastPodcastId != podcast.getId()) {
					_podcastTitle.setText(podcast.getTitle());
					_pausebtn.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
					_showplayerbtn.setEnabled(true);
				}
			} else if (_lastPodcastId != null) {
				_podcastTitle.setText("");
				_positionstring.setText("");
				_showplayerbtn.setEnabled(false);
			} 

			_lastPodcastId = podcast.getId();
		} catch (MissingFieldException e) {
			e.printStackTrace();
		} finally {
			if (cursor != null)
				cursor.close();
		}
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
				_pausebtn.setImageResource(PlayerService.isPlaying() ? android.R.drawable.ic_media_play
						: android.R.drawable.ic_media_pause);
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
