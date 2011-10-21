package com.axelby.podax;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class BottomBar extends RelativeLayout {

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

	private void setupHandler(final Context context) {
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			private Podcast _lastPodcast;

			public void run() {
				boolean isPlaying = PlayerService.isPlaying();
				Podcast podcast = PlayerService.getActivePodcast(context);

				if (podcast != null) {
					if (isPlaying || _positionstring.getText().length() == 0)
						_positionstring.setText(PlayerService.getPositionString(podcast.getDuration(), podcast.getLastPosition()));
					if (_lastPodcast != podcast) {
						_podcastTitle.setText(podcast.getTitle());
						_pausebtn.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
						_showplayerbtn.setEnabled(true);
					}
				} else if (_lastPodcast != null) {
					_podcastTitle.setText("");
					_positionstring.setText("");
					_showplayerbtn.setEnabled(false);
				} 

				_lastPodcast = podcast;

				handler.postDelayed(this, 250);
			}
		}, 250);
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
