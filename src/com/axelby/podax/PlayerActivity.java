package com.axelby.podax;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlayerActivity {
	public static void injectPlayerFooter(final Activity activity) {
		return;
		/*
		injectView(activity, R.layout.player);

		final TextView _podcastTitle = (TextView) activity.findViewById(R.id.podcasttitle);
		final TextView _positionstring = (TextView) activity.findViewById(R.id.positionstring);
		final ImageButton _pausebtn = (ImageButton) activity.findViewById(R.id.pausebtn);
		final ImageButton _showplayerbtn = (ImageButton) activity.findViewById(R.id.showplayer);
		
		_podcastTitle.setText("");
		_positionstring.setText("");
		_showplayerbtn.setEnabled(false);

		_pausebtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				PodaxApp app = PodaxApp.getApp();
				app.playpause();
				_pausebtn.setImageResource(PlayerService.isPlaying() ? android.R.drawable.ic_media_play
						: android.R.drawable.ic_media_pause);
			}
		});
		
		_showplayerbtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(activity, PodcastDetailActivity.class);
				activity.startActivity(intent);
			}
		});
		
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			private Podcast _lastPodcast;

			public void run() {
				boolean isPlaying = PlayerService.isPlaying();
				Podcast podcast = PlayerService.getActivePodcast(activity);

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
		*/
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