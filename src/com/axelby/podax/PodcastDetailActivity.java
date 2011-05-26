package com.axelby.podax;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class PodcastDetailActivity extends Activity {
	Podcast _podcast;
	
	TextView _titleView;
	TextView _subscriptionTitleView;
	WebView _descriptionView;
	Button _queueButton;
	TextView _queuePosition;
	Button _playButton;

	private DBAdapter _dbAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.podcast_detail);
		
        _dbAdapter = DBAdapter.getInstance(this);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        int podcastId = bundle.getInt("podcastId");
		_podcast = _dbAdapter.loadPodcast(podcastId);

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

		_playButton = (Button)findViewById(R.id.play_btn);
		_playButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				PodaxApp.getApp().playPodcast(_podcast);
			}
			
		});
		
		PlayerActivity.injectPlayerFooter(this);
	}

	private void updateQueueViews() {
		if (_podcast.getQueuePosition() == null) {
			_queueButton.setText(R.string.add_to_queue);
			_queuePosition.setText("");
		} else {
			_queueButton.setText(R.string.remove_from_queue);
			_queuePosition.setText("#" + String.valueOf(_podcast.getQueuePosition() + 1) + " in queue");
		}
	}
}
