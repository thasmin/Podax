package com.axelby.podax.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;
import com.squareup.otto.Subscribe;

public class BottomBar extends LinearLayout {

	private TextView _podcastTitle;
	private PodcastProgress _podcastProgress;
	private ImageButton _pausebtn;
	private ImageButton _showplayerbtn;

	public BottomBar(Context context) {
		super(context);

		LayoutInflater.from(context).inflate(R.layout.player, this);

		if (isInEditMode())
			return;
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater.from(context).inflate(R.layout.player, this);
		
		if (isInEditMode())
			return;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		loadViews(getContext());
		updateUI();
		PlayerStatus.register(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		PlayerStatus.unregister(this);
	}

	Uri _activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");

	private void loadViews(final Context context) {
		_podcastTitle = (TextView) findViewById(R.id.podcasttitle);
		_podcastProgress = (PodcastProgress) findViewById(R.id.podcastprogress);
		_pausebtn = (ImageButton) findViewById(R.id.pausebtn);
		_showplayerbtn = (ImageButton) findViewById(R.id.showplayer);
		
		_podcastTitle.setText("");
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
		boolean isPlaying = PlayerStatus.isPlaying();
		_pausebtn.setImageResource(isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play);

		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Cursor c = getContext().getContentResolver().query(_activeUri, projection, null, null, null);
		try {
			if (c.isAfterLast())
				return;
	
			PodcastCursor podcast = new PodcastCursor(c);
			if (!podcast.isNull()) {
				if (isPlaying || _podcastProgress.isEmpty())
					_podcastProgress.set(podcast);
				if (_lastPodcastId != podcast.getId()) {
					_podcastTitle.setText(podcast.getTitle());
					_showplayerbtn.setEnabled(true);
				}
			} else if (_lastPodcastId != null) {
				_podcastTitle.setText("");
				_podcastProgress.clear();
				_showplayerbtn.setEnabled(false);
			}
	
			_lastPodcastId = podcast.isNull() ? null : podcast.getId();
		} finally {
			c.close();
		}
	}

	@Subscribe public void playerStateChange(PlayerStatus e) {
		boolean isPlaying = e.getState() == PlayerStates.PLAYING;
		_pausebtn.setImageResource(isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
		_podcastProgress.set(e.getPosition(), e.getDuration());
	}
}
