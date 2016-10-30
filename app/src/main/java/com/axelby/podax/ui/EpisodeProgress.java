package com.axelby.podax.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

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

	void clear() {
		if (isInEditMode()) {
			_position.setText(R.string.editmode_position);
			_progressbar.setVisibility(VISIBLE);
			_progressbar.setMax(100);
			_progressbar.setProgress(50);
			_remaining.setText(R.string.editmode_remaining);
		} else {
			_position.setText("");
			_progressbar.setVisibility(INVISIBLE);
			_remaining.setText("");
		}
	}

	public static void remoteClear(RemoteViews views) {
		views.setTextViewText(R.id.position, "");
		views.setViewVisibility(R.id.progress, INVISIBLE);
		views.setTextViewText(R.id.remaining, "");
	}

	public static void remoteSet(RemoteViews views, int position, int duration) {
		views.setTextViewText(R.id.position, Helper.getTimeString(position));
		views.setViewVisibility(R.id.progress, VISIBLE);
		views.setProgressBar(R.id.progress, duration, position, false);
		views.setTextViewText(R.id.remaining, "-" + Helper.getTimeString(duration - position));
	}
}
