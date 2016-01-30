package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.R;

public class BottomBarView extends RelativeLayout {

	private View _progressbg;
	private View _progressline;
	private ImageButton _play;
	private TextView _episodeTitle;
	private ImageButton _expand;

	public BottomBarView(Context context) {
		super(context);
		init();
	}

	public BottomBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BottomBarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		inflate(getContext(), R.layout.bottom_bar_view, this);

		_progressbg = findViewById(R.id.progressbg);
		_progressline = findViewById(R.id.progressline);
		_play = (ImageButton) findViewById(R.id.play);
		_episodeTitle = (TextView) findViewById(R.id.episode_title);
		_expand = (ImageButton) findViewById(R.id.expand);

		// TODO: does this leak the activity?
		PlayerStatus.asObservable().subscribe(this::update, this::onError);
	}

	private void update(PlayerStatus playerState) {
        if (playerState.hasActiveEpisode()) {
			_progressbg.setVisibility(View.VISIBLE);
			_progressline.setVisibility(View.VISIBLE);

			ViewGroup.LayoutParams lineParams = _progressline.getLayoutParams();
			lineParams.width = getProgressLineWidthPx(playerState);
			_progressline.setLayoutParams(lineParams);

           int playResource = playerState.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play;
            _play.setImageResource(playResource);
            _play.setOnClickListener(view -> PlayerService.playpause(getContext()));

			Activity activity = Helper.getActivityFromView(this);

            _episodeTitle.setText(playerState.getTitle());
			_episodeTitle.setOnClickListener(view -> AppFlow.get(activity).displayActiveEpisode());

            _expand.setImageResource(R.drawable.ic_action_collapse);
			_expand.setOnClickListener(view -> AppFlow.get(activity).displayActiveEpisode());
        } else {
			_progressbg.setVisibility(View.GONE);
			_progressline.setVisibility(View.GONE);
            _play.setImageDrawable(null);
			_play.setOnClickListener(null);
            _episodeTitle.setText(R.string.playlist_empty);
            _expand.setImageDrawable(null);
			_expand.setOnClickListener(null);
        }
	}

	private int getProgressLineWidthPx(PlayerStatus playerState) {
		Point screenSize = new Point();
		WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getSize(screenSize);
		float progress = 1.0f * playerState.getPosition() / playerState.getDuration();
		return (int) (screenSize.x * progress);
	}

	private void onError(Throwable throwable) {
		Log.e("MainActivity", "unable to update bottom bar", throwable);
	}
}
