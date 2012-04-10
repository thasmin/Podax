package com.axelby.podax;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

public class PodcastProgress extends RelativeLayout {

	private TextView _position;
	private ProgressBar _progressbar;
	private TextView _remaining;

	public PodcastProgress(Context context) {
		super(context);

		loadViews(context);
		clear();
	}

	public PodcastProgress(Context context, AttributeSet attrs) {
		super(context, attrs);

		loadViews(context);
		clear();
	}

	private void loadViews(Context context) {
		LayoutInflater.from(context).inflate(R.layout.podcast_progress, this);
		_position = (TextView) findViewById(R.id.position);
		_progressbar = (ProgressBar) findViewById(R.id.progress);
		_remaining = (TextView) findViewById(R.id.remaining);
	}

	public void clear() {
		_position.setText("");
		_progressbar.setVisibility(INVISIBLE);
		_remaining.setText("");
	}

	public boolean isEmpty() { 
		return _position.getText().length() == 0;
	}

	public void set(PodcastCursor podcast) {
		_position.setText(Helper.getTimeString(podcast.getLastPosition()));
		_progressbar.setVisibility(VISIBLE);
		_progressbar.setMax(podcast.getDuration());
		_progressbar.setProgress(podcast.getLastPosition());
		_remaining.setText("-" + Helper.getTimeString(podcast.getDuration() - podcast.getLastPosition()));
	}

	public static void remoteClear(RemoteViews views) {
		views.setTextViewText(R.id.position, "");
		views.setViewVisibility(R.id.progress, INVISIBLE);
		views.setTextViewText(R.id.remaining, "");
	}

	public static void remoteSet(RemoteViews views, PodcastCursor podcast) {
		views.setTextViewText(R.id.position, Helper.getTimeString(podcast.getLastPosition()));
		views.setViewVisibility(R.id.progress, VISIBLE);
		views.setProgressBar(R.id.progress, podcast.getDuration(), podcast.getLastPosition(), false);
		views.setTextViewText(R.id.remaining, "-" + Helper.getTimeString(podcast.getDuration() - podcast.getLastPosition()));
	}
}
