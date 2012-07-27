package com.axelby.podax.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
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

	private Timer _timer;
	private Handler _handler = new Handler();

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

		_timer = new Timer();
		_timer.schedule(new TimerTask() {
			public void run() {
				_handler.post(new Runnable() {
					public void run() {
						_pausebtn.setImageResource(PlayerStatus.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
						PlayerStatus status = PlayerStatus.getCurrentState();
						if (status != null) {
							_podcastProgress.set(status.getPosition(), status.getDuration());
							_podcastTitle.setText(status.getTitle());
						} else {
							_podcastProgress.clear();
							_podcastTitle.setText("");
						}
					}
				});
			}
		}, 500, 500);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		if (_timer != null)
			_timer.cancel();
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
