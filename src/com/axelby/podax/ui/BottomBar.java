package com.axelby.podax.ui;

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

import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class BottomBar extends LinearLayout {
	private TextView _podcastTitle;
	private PodcastProgress _podcastProgress;
	private ImageButton _pausebtn;
	private ImageButton _showplayerbtn;

	private ContentObserver _activePodcastObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			PlayerStatus status = PlayerStatus.getCurrentState(getContext());
			_pausebtn.setImageResource(status.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
			if (status.hasActivePodcast()) {
				_podcastProgress.set(status.getPosition(), status.getDuration());
				_podcastTitle.setText(status.getTitle());
			} else {
				_podcastProgress.clear();
				_podcastTitle.setText("Queue empty");
			}
		}

		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}
	};

	public BottomBar(Context context) {
		super(context);

		LayoutInflater.from(context).inflate(R.layout.player, this);
	}

	public BottomBar(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater.from(context).inflate(R.layout.player, this);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (isInEditMode())
			return;

		loadViews(getContext());
		updateUI(PlayerStatus.getCurrentState(getContext()));

		getContext().getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activePodcastObserver);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		getContext().getContentResolver().unregisterContentObserver(_activePodcastObserver);
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
				PlayerService.playstop(BottomBar.this.getContext());
			}
		});

		_showplayerbtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(context, PodcastDetailActivity.class);
				context.startActivity(intent);
			}
		});
	}

	public void updateUI(PlayerStatus status) {
		boolean isPlaying = status.isPlaying();
		_pausebtn.setImageResource(isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play);

		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
		};
		Cursor c = getContext().getContentResolver().query(_activeUri, projection, null, null, null);
		try {
			if (!c.moveToNext()) {
				_podcastTitle.setText("");
				_podcastProgress.clear();
				_showplayerbtn.setEnabled(false);
			} else {
				PodcastCursor podcast = new PodcastCursor(c);
				_podcastProgress.set(podcast);
				_podcastTitle.setText(podcast.getTitle());
				_showplayerbtn.setEnabled(true);
			}
		} finally {
			c.close();
		}
	}
}
