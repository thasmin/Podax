package com.axelby.podax.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class EpisodeProgress extends RelativeLayout {

	private TextView _position;
	private ProgressBar _progressbar;
	private TextView _remaining;

	public EpisodeProgress(Context context) {
		super(context);

		loadViews(context);
		clear();
	}

	public EpisodeProgress(Context context, AttributeSet attrs) {
		super(context, attrs);

		loadViews(context);
		clear();
	}

	private void loadViews(Context context) {
		LayoutInflater.from(context).inflate(R.layout.episode_progress, this);
		_position = (TextView) findViewById(R.id.position);
		_progressbar = (ProgressBar) findViewById(R.id.progress);
		_remaining = (TextView) findViewById(R.id.remaining);
	}

	public void clear() {
		if (isInEditMode()) {
			_position.setText("12:34");
			_progressbar.setVisibility(VISIBLE);
			_progressbar.setMax(100);
			_progressbar.setProgress(50);
			_remaining.setText("-43:21");
		} else {
			_position.setText("");
			_progressbar.setVisibility(INVISIBLE);
			_remaining.setText("");
		}
	}

	public boolean isEmpty() {
		return _position.getText().length() == 0;
	}

	public void set(EpisodeCursor episode) {
		_position.setText(Helper.getTimeString(episode.getLastPosition()));
		_progressbar.setVisibility(VISIBLE);
		_progressbar.setMax(episode.getDuration());
		_progressbar.setProgress(episode.getLastPosition());
		_remaining.setText("-" + Helper.getTimeString(episode.getDuration() - episode.getLastPosition()));
	}

	public void set(int position, int duration) {
		_position.setText(Helper.getTimeString(position));
		_progressbar.setVisibility(VISIBLE);
		_progressbar.setMax(duration);
		_progressbar.setProgress(position);
		_remaining.setText("-" + Helper.getTimeString(duration - position));
	}

	public static void remoteClear(RemoteViews views) {
		views.setTextViewText(R.id.position, "");
		views.setViewVisibility(R.id.progress, INVISIBLE);
		views.setTextViewText(R.id.remaining, "");
	}

	public static void remoteSet(RemoteViews views, EpisodeCursor episode) {
		views.setTextViewText(R.id.position, Helper.getTimeString(episode.getLastPosition()));
		views.setViewVisibility(R.id.progress, VISIBLE);
		views.setProgressBar(R.id.progress, episode.getDuration(), episode.getLastPosition(), false);
		views.setTextViewText(R.id.remaining, "-" + Helper.getTimeString(episode.getDuration() - episode.getLastPosition()));
	}

	public static void remoteSet(RemoteViews views, int position, int duration) {
		views.setTextViewText(R.id.position, Helper.getTimeString(position));
		views.setViewVisibility(R.id.progress, VISIBLE);
		views.setProgressBar(R.id.progress, duration, position, false);
		views.setTextViewText(R.id.remaining, "-" + Helper.getTimeString(duration - position));
	}
}
