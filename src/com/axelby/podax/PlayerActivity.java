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

	protected PlayerService _player;
	
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
		
		_player = PlayerService.getInstance();
		
		_pausebtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				if (_player.isPlaying()) {
					_player.pause();
					_pausebtn.setImageResource(android.R.drawable.ic_media_play);
				} else {
					_player.play();
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
				if (_player != null)
					updateUI();				
				
				handler.postDelayed(this, 400);
			}
		}, 200);
	}

	protected void playPodcast(Podcast podcast) {
		if (_player == null)
			return;
		_player.load(podcast);
		_player.play();
	}

	private void updatePositionText() {
		_position.setText(PlayerService.getPositionString(_player.getDuration(), _player.getPosition()));
	}

	private void updateUI() {
		_podcast = _player.getActivePodcast();

		if (_podcast == null) {
			_podcastTitle.setText("");
			_position.setText("");
			_pausebtn.setEnabled(false);
			_showplayerbtn.setEnabled(false);
		} else {
			_podcastTitle.setText(_podcast.getTitle());
			_pausebtn.setEnabled(true);
			_pausebtn.setImageResource(_player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
			_showplayerbtn.setEnabled(true);
			updatePositionText();
			_dbApapter.updatePodcastPosition(_podcast.getId(), _player.getPosition());
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