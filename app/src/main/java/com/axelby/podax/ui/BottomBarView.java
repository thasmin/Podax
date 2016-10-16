package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
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
import com.axelby.podax.model.SubscriptionData;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
		PlayerStatus.watch()
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(this::update, this::onError);
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

			Palette.Swatch swatch = SubscriptionData.getThumbnailSwatch(playerState.getSubscriptionId());
			if (swatch != null) {
				_progressline.setBackgroundColor(swatch.getTitleTextColor());
				_progressbg.setBackgroundColor(swatch.getRgb());
				_episodeTitle.setBackgroundColor(swatch.getRgb());
				_play.setBackgroundColor(swatch.getRgb());
				_expand.setBackgroundColor(swatch.getRgb());
				_episodeTitle.setTextColor(swatch.getBodyTextColor());
				_play.setColorFilter(swatch.getBodyTextColor());
				_expand.setColorFilter(swatch.getBodyTextColor());
			} else {
				resetColors();
			}
        } else {
			_progressbg.setVisibility(View.GONE);
			_progressline.setVisibility(View.GONE);
            _play.setImageDrawable(null);
			_play.setOnClickListener(null);
            _episodeTitle.setText(R.string.playlist_empty);
            _expand.setImageDrawable(null);
			_expand.setOnClickListener(null);

			resetColors();
        }
	}

	private void resetColors() {
		_progressline.setBackgroundColor(ContextCompat.getColor(getContext(), getAccentColor()));
		_progressbg.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.podaxColor));
		_episodeTitle.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.podaxColor));
		_play.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.podaxColor));
		_expand.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.podaxColor));
		_episodeTitle.setTextColor(null);
		_play.setColorFilter(null);
		_expand.setColorFilter(null);
	}

	private int getAccentColor() {
		TypedValue typedValue = new TypedValue();
		TypedArray a = getContext().obtainStyledAttributes(typedValue.data, new int[] { R.attr.colorAccent });
		int color = a.getColor(0, 0);
		a.recycle();
		return color;
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
