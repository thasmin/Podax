package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

public class PlayerStatus {

	public static PlayerStatus getCurrentState(Context context) {
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_ID,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Cursor cursor = context.getContentResolver().query(PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
		PlayerStatus status = new PlayerStatus();
		if (cursor == null) {
			status._state = PlayerStates.QUEUEEMPTY;
			return status;
		}
		if (cursor.moveToNext()) {
			PodcastCursor podcast = new PodcastCursor(cursor);
			SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
			status._state = PlayerStates.fromInt(prefs.getInt("playingState", PlayerStates.STOPPED.toInt()));
			status._podcastId = podcast.getId();
			status._subscriptionId = podcast.getSubscriptionId();
			status._title = podcast.getTitle();
			status._subscriptionTitle = podcast.getSubscriptionTitle();
			status._subscriptionThumbnailUrl = podcast.getSubscriptionThumbnailUrl();
			status._position = podcast.getLastPosition();
			status._duration = podcast.getDuration();
			status._filename = podcast.getFilename(context);
		}
		cursor.close();
		return status;
	}

	public static void updateState(Context context, PlayerStates state) {
		SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
		prefs.edit().putInt("playingState", state.toInt()).commit();
	}

	public static PlayerStates getPlayerState(Context context) {
		SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
		return PlayerStates.fromInt(prefs.getInt("playingState", -1));
	}

	// instance variables
	public enum PlayerStates {
		INVALID(-1),
		QUEUEEMPTY(0),
		STOPPED(1),
		PAUSED(2),
		PLAYING(3);

		private final int _code;

		PlayerStates(int code) {
			_code = code;
		}

		public int toInt() {
			return _code;
		}

		public static PlayerStates fromInt(int code) {
			for (PlayerStates p : PlayerStates.values())
				if (p.toInt() == code)
					return p;
			return QUEUEEMPTY;
		}
	}

	private PlayerStates _state;
	private long _podcastId;
	private long _subscriptionId;
	private int _position;
	private int _duration;
	private String _title;
	private String _subscriptionTitle;
	private String _subscriptionThumbnailUrl;
	private String _filename;

	private PlayerStatus() {
		_state = PlayerStates.QUEUEEMPTY;
		_podcastId = -1;
	}

	public PlayerStates getState() {
		return _state;
	}

	public boolean isPlaying() { return _state == PlayerStates.PLAYING; }

	public long getPodcastId() {
		return _podcastId;
	}

	public long getSubscriptionId() {
		return _subscriptionId;
	}

	public int getPosition() { return _position; }

	public int getDuration() {
		return _duration;
	}

	public String getTitle() {
		return _title;
	}

	public String getSubscriptionTitle() {
		return _subscriptionTitle;
	}

	public String getSubscriptionThumbnailUrl() {
		return _subscriptionThumbnailUrl;
	}

	public String getFilename() { return _filename; }

	public boolean hasActivePodcast() {
		return getState() != PlayerStates.QUEUEEMPTY;
	}

	public boolean isPlayerServiceActive() {
		return getState() == PlayerStates.PLAYING || getState() == PlayerStates.PAUSED;
	}
}
