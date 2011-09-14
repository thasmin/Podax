package com.axelby.podax;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PodcastDetailActivity extends Activity {
	Podcast _podcast;

	TextView _titleView;
	TextView _subscriptionTitleView;
	WebView _descriptionView;

	Button _queueButton;
	TextView _queuePosition;

	ImageButton _restartButton;
	ImageButton _rewindButton;
	ImageButton _playButton;
	ImageButton _forwardButton;
	ImageButton _skipToEndButton;
	SeekBar _seekbar;
	boolean _seekbar_dragging;

	TextView _position;
	TextView _duration;

	DBAdapter _dbAdapter;
	PodaxApp _app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.podcast_detail);
		
        _dbAdapter = DBAdapter.getInstance(this);
		_app = PodaxApp.getApp();

        Intent intent = this.getIntent();
        if (intent.hasExtra(Constants.EXTRA_PODCAST_ID))
        	_podcast = _dbAdapter.loadPodcast(intent.getIntExtra(Constants.EXTRA_PODCAST_ID, -1));
        else
        	_podcast = _dbAdapter.loadLastPlayedPodcast();

        if (_podcast == null) {
            finish();
            startActivity(new Intent(this, QueueActivity.class));
        }

		_titleView = (TextView)findViewById(R.id.title);
		_titleView.setText(_podcast.getTitle());
		_subscriptionTitleView = (TextView)findViewById(R.id.subscription_title);
		_subscriptionTitleView.setText(_podcast.getSubscription().getTitle());
		
		_descriptionView = (WebView)findViewById(R.id.description);
		String html = _podcast.getDescription();
		html = "<html><body style=\"background:black;color:white\">" + html + "</body></html>"; 
		_descriptionView.loadData(html, "text/html", "utf-8");

		_queuePosition = (TextView)findViewById(R.id.queue_position);
		_queueButton = (Button)findViewById(R.id.queue_btn);
		updateQueueViews();
		_queueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_podcast.getQueuePosition() == null)
					_dbAdapter.addPodcastToQueue(_podcast.getId());
				else
					_dbAdapter.removePodcastFromQueue(_podcast.getId());
				_podcast = _dbAdapter.loadPodcast(_podcast.getId());
				updateQueueViews();
			}
		});

		_restartButton = (ImageButton)findViewById(R.id.restart_btn);
		_restartButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_app.restart();
			}
		});
		
		_rewindButton = (ImageButton)findViewById(R.id.rewind_btn);
		_rewindButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_app.skipBack();
			}
		});

		_playButton = (ImageButton)findViewById(R.id.play_btn);
		_playButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_app.isPlaying() && _app.getActivePodcast().getId() == _podcast.getId())
					_app.playpause();
				else
					_app.play(_podcast);
			}
		});
		
		_forwardButton = (ImageButton)findViewById(R.id.forward_btn);
		_forwardButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_app.skipForward();
			}
		});
		
		_skipToEndButton = (ImageButton)findViewById(R.id.skiptoend_btn);
		_skipToEndButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_app.skipToEnd();
			}
		});
		
		_seekbar = (SeekBar)findViewById(R.id.seekbar);
		_seekbar.setMax(_podcast.getDuration());
		_seekbar.setProgress(_podcast.getLastPosition());
		_seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				_position.setText(PlayerActivity.getTimeString(progress));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = true;
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				_seekbar_dragging = false;
				_app.skipTo(seekBar.getProgress() / 1000);
			}
		});
		_seekbar_dragging = false;
		
		_position = (TextView)findViewById(R.id.position);
		_position.setText(PlayerActivity.getTimeString(_podcast.getLastPosition()));
		_duration = (TextView)findViewById(R.id.duration);
		_duration.setText(PlayerActivity.getTimeString(_podcast.getDuration()));
		
		PlayerActivity.injectPlayerFooter(this);
		
		updatePlayerControls(true);
    	final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				updatePlayerControls(false);	
				handler.postDelayed(this, 250);
			}
		}, 250);
	}

	boolean _controlsEnabled = true;
	private void updatePlayerControls(boolean force) {
		if (_app.isPlaying() && _app.getActivePodcast().getId() == _podcast.getId()) {
			if (!_seekbar_dragging) {
				_position.setText(PlayerActivity.getTimeString(_app.getPosition()));
				_duration.setText(PlayerActivity.getTimeString(_app.getDuration()));
				_seekbar.setProgress(_app.getPosition());
			}

			if (!force && _controlsEnabled == true)
				return;
			
	    	_playButton.setImageResource(android.R.drawable.ic_media_pause);
	    	_restartButton.setEnabled(true);
	    	_rewindButton.setEnabled(true);
	    	_forwardButton.setEnabled(true);
	    	_skipToEndButton.setEnabled(true);
	    	_seekbar.setEnabled(true);

			_controlsEnabled = true;
		} else {
			if (!force && !_controlsEnabled)
				return;

	    	_playButton.setImageResource(android.R.drawable.ic_media_play);
	    	_restartButton.setEnabled(false);
	    	_rewindButton.setEnabled(false);
	    	_forwardButton.setEnabled(false);
	    	_skipToEndButton.setEnabled(false);
	    	_seekbar.setEnabled(false);

	    	_controlsEnabled = false;
		}
	}

	private void updateQueueViews() {
		if (_podcast.getQueuePosition() == null) {
			_queueButton.setText(R.string.add_to_queue);
			_queuePosition.setText("");
		} else {
			_queueButton.setText(R.string.remove_from_queue);
			_queuePosition.setText("#"
					+ String.valueOf(_podcast.getQueuePosition() + 1)
					+ " in queue");
		}
	}
}
