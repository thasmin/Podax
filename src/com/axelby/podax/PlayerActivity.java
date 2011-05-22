package com.axelby.podax;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class PlayerActivity extends Activity {
	
	protected ImageButton _pausebtn;
	protected ImageButton _showplayerbtn;
	protected TextView _podcastTitle;
	protected TextView _position;
	
	protected Podcast _podcast;
	protected DBAdapter _dbApapter;

	public PlayerActivity() {
		super();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		_dbApapter = DBAdapter.getInstance(this);

		_podcastTitle = (TextView) findViewById(R.id.podcasttitle);
		_position = (TextView) findViewById(R.id.position);
		_pausebtn = (ImageButton) findViewById(R.id.pausebtn);
		_showplayerbtn = (ImageButton) findViewById(R.id.showplayer);
		
		_pausebtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				PodaxApp app = PodaxApp.getApp();
				if (app.isPlaying()) {
					app.pause();
					_pausebtn.setImageResource(android.R.drawable.ic_media_play);
				} else {
					app.play();
					_pausebtn.setImageResource(android.R.drawable.ic_media_pause);
				}
			}
		});
		
		_showplayerbtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(PlayerActivity.this, PlayerControlActivity.class);
				startActivity(intent);
			}
		});
		
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				updateUI();				
				handler.postDelayed(this, 400);
			}
		}, 200);
	}

	private void updatePositionText() {
		PodaxApp app = PodaxApp.getApp();
		_position.setText(PlayerService.getPositionString(app.getDuration(), app.getPosition()));
	}

	private void updateUI() {
		PodaxApp app = PodaxApp.getApp();
		_podcast = app.getActivePodcast();
		
		if (_podcast == null) {
			_podcastTitle.setText("");
			_position.setText("");
			_showplayerbtn.setEnabled(false);
		} else {
			_podcastTitle.setText(_podcast.getTitle());
			_pausebtn.setImageResource(app.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
			_showplayerbtn.setEnabled(true);
			updatePositionText();
		}
	}

	static String getTimeString(int milliseconds) {
		int seconds = milliseconds / 1000;
		final int SECONDSPERHOUR = 60 * 60;
		final int SECONDSPERMINUTE = 60;
		int hours = seconds / SECONDSPERHOUR;
		int minutes = seconds % SECONDSPERHOUR / SECONDSPERMINUTE;
		seconds = seconds % SECONDSPERMINUTE;
		
		StringBuilder builder = new StringBuilder();
		if (hours > 0) {
			builder.append(hours);
			builder.append(":");
			if (minutes < 10)
				builder.append("0");
		}
		builder.append(minutes);
		builder.append(":");
		if (seconds < 10)
			builder.append("0");
		builder.append(seconds);
		return builder.toString();
	}

}