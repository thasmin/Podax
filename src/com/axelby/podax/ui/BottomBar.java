package com.axelby.podax.ui;

import android.content.BroadcastReceiver;
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

import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PlayerStatus.PlayerStates;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class BottomBar extends LinearLayout {
	private TextView _podcastTitle;
	private PodcastProgress _podcastProgress;
	private ImageButton _pausebtn;
	private ImageButton _showplayerbtn;

	private BroadcastReceiver _positionUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int position = intent.getExtras().getInt(Constants.EXTRA_POSITION);
			int duration = intent.getExtras().getInt(Constants.EXTRA_DURATION);
			_podcastProgress.set(position, duration);
		}
	};

	BroadcastReceiver _activePodcastChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long activePodcastId = intent.getExtras().getLong(Constants.EXTRA_PODCAST_ID, -1);
			if (activePodcastId == -1) {
				_podcastProgress.clear();
				_podcastTitle.setText("Queue empty");
			} else {
				Uri uri = PodcastProvider.getContentUri(activePodcastId);
				String[] projection = new String[] { PodcastProvider.COLUMN_TITLE };
				Cursor c = getContext().getContentResolver().query(uri, projection, null, null, null);
				if (c.moveToFirst())
					_podcastTitle.setText(c.getString(0));
				c.close();
			}
		}
	};

	BroadcastReceiver _stateChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PlayerStates state = PlayerStates.fromInt(intent.getExtras().getInt(Constants.EXTRA_PLAYERSTATE));
			Log.d("Podax", state.toString());
			_pausebtn.setImageResource(state == PlayerStates.PLAYING ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
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
		getContext().getContentResolver().registerContentObserver(PodcastProvider.ACTIVE_PODCAST_URI, false, _activePodcastObserver);
		updateUI(PlayerStatus.getCurrentState(getContext()));

		Helper.registerReceiver(getContext(), Constants.ACTION_PLAYER_POSITIONCHANGED, _positionUpdateReceiver);
		Helper.registerReceiver(getContext(), Constants.ACTION_PLAYER_STATECHANGED, _stateChangedReceiver);
		Helper.registerReceiver(getContext(), Constants.ACTION_PLAYER_ACTIVEPODCASTCHANGED, _activePodcastChangedReceiver);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		getContext().unregisterReceiver(_positionUpdateReceiver);
		getContext().unregisterReceiver(_stateChangedReceiver);
		getContext().unregisterReceiver(_activePodcastChangedReceiver);
	}

	Uri _activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
	private ContentObserver _activePodcastObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			Log.d("Podax", "bottombar got a content changed message");
		}

		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}
	};

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
