package com.axelby.podax;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerControlActivity extends Activity {
	protected DBAdapter _dbAdapter;
	protected PodaxApp _app;

	protected TextView _title;
	protected TextView _subscription_title;
	protected TextView _position;
	protected ImageButton _play_btn;
	protected SeekBar _seekbar;
	protected Button _restart; 
	protected Button _skiptoend; 
	protected Button _secs30_rewind_btn;
	protected Button _secs30_skip_btn;
	protected Button _secs15_rewind_btn;
	protected Button _secs15_skip_btn;
	protected Button _secs5_rewind_btn;
	protected Button _secs5_skip_btn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.player_control);

		super.onCreate(savedInstanceState);
		
        _dbAdapter = DBAdapter.getInstance(this);
		_app = PodaxApp.getApp();
		
    	_title = (TextView)findViewById(R.id.title);
    	_subscription_title = (TextView)findViewById(R.id.subscription_title);
    	_position = (TextView)findViewById(R.id.position);
    	_play_btn = (ImageButton)findViewById(R.id.play_btn);
    	_seekbar = (SeekBar)findViewById(R.id.seekbar);
    	_restart = (Button)findViewById(R.id.restart); 
    	_skiptoend = (Button)findViewById(R.id.skiptoend);
    	_secs30_rewind_btn = (Button)findViewById(R.id.secs30_rewind_btn);
    	_secs30_skip_btn = (Button)findViewById(R.id.secs30_skip_btn);
    	_secs15_rewind_btn = (Button)findViewById(R.id.secs15_rewind_btn);
    	_secs15_skip_btn = (Button)findViewById(R.id.secs15_skip_btn);
    	_secs5_rewind_btn = (Button)findViewById(R.id.secs5_rewind_btn);
    	_secs5_skip_btn = (Button)findViewById(R.id.secs5_skip_btn);
    	
    	_play_btn.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
				if (_app.isPlaying()) {
					_app.pause();
					_play_btn.setImageResource(android.R.drawable.ic_media_play);
				} else {
					_app.play();
					_play_btn.setImageResource(android.R.drawable.ic_media_pause);
				}
    		}
    	});
    	_restart.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_app != null)
					_app.restart();
			}
    	});
    	_skiptoend.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_app != null)
					_app.skipToEnd();
			}
    	});
    	
    	_secs30_rewind_btn.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			if (_app != null)
    				_app.skipBack();
    		}
    	});
    	_secs30_skip_btn.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			if (_app != null)
    				_app.skipForward();
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

	private void updateUI() {
		Podcast p = _app.getActivePodcast();
    	if (p == null)
    	{
        	_title.setText("");
        	_subscription_title.setText("");
        	_position.setText("");
        	_play_btn.setEnabled(false);
        	_seekbar.setEnabled(false);
        	_restart.setEnabled(false); 
        	_skiptoend.setEnabled(false);
        	_secs30_rewind_btn.setEnabled(false);
        	_secs30_skip_btn.setEnabled(false);
        	_secs15_rewind_btn.setEnabled(false);
        	_secs15_skip_btn.setEnabled(false);
        	_secs5_rewind_btn.setEnabled(false);
        	_secs5_skip_btn.setEnabled(false);
        	return;
    	}

    	_title.setText(p.getTitle());
    	_subscription_title.setText(p.getSubscription().getTitle());
		_position.setText(PlayerService.getPositionString(_app.getDuration(), _app.getPosition()));
    	_play_btn.setEnabled(true);
    	_play_btn.setImageResource(_app.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
		_seekbar.setMax(_app.getDuration());
		_seekbar.setProgress(_app.getPosition());
    	_restart.setEnabled(true); 
    	_skiptoend.setEnabled(true);
    	_secs30_rewind_btn.setEnabled(true);
    	_secs30_skip_btn.setEnabled(true);
    	_secs15_rewind_btn.setEnabled(true);
    	_secs15_skip_btn.setEnabled(true);
    	_secs5_rewind_btn.setEnabled(true);
    	_secs5_skip_btn.setEnabled(true);
	}
}
