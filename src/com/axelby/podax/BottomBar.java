package com.axelby.podax;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
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

	private class ActivePodcastObserver extends ContentObserver {
		public ActivePodcastObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			refreshCursor();
			updateUI();
		}
	}
	private ActivePodcastObserver _observer = new ActivePodcastObserver(new Handler());

	public BottomBar(Context context) {
		super(context);
		
		if (isInEditMode())
			return;
		loadViews(context);

		retrievePodcast();
		updateUI();
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.player, this);
		
		if (isInEditMode())
			return;
		loadViews(context);

		retrievePodcast();
		updateUI();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		if (!_cursor.isClosed())
			_cursor.close();
		getContext().getContentResolver().unregisterContentObserver(_observer);
	}

	Uri _activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
	public void refreshCursor() {
		if (_cursor != null && !_cursor.isClosed())
			_cursor.close();

		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		_cursor = getContext().getContentResolver().query(_activeUri, projection, null, null, null);
	}

	public void retrievePodcast() {
		refreshCursor();
		getContext().getContentResolver().registerContentObserver(_activeUri, false, _observer);
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
				PlayerService.playpause(BottomBar.this.getContext());
			}
		});

		_showplayerbtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(context, PodcastDetailActivity.class);
				context.startActivity(intent);
			}
		});
	}

	private Long _lastPodcastId = null;
	public void updateUI() {
		boolean isPlaying = Helper.isPlaying(getContext());
		_pausebtn.setImageResource(isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play);

		PodcastCursor podcast = new PodcastCursor(getContext(), _cursor);
		if (!podcast.isNull()) {
			Log.d("Podax", "position " + podcast.getLastPosition());
			if (isPlaying || _positionstring.getText().length() == 0)
				_positionstring.setText(PlayerService.getPositionString(podcast.getDuration(), podcast.getLastPosition()));
			if (_lastPodcastId != podcast.getId()) {
				_podcastTitle.setText(podcast.getTitle());
				_showplayerbtn.setEnabled(true);
			}
		} else if (_lastPodcastId != null) {
			_podcastTitle.setText("");
			_positionstring.setText("");
			_showplayerbtn.setEnabled(false);
		}

		_lastPodcastId = podcast.isNull() ? null : podcast.getId();
	}

}
