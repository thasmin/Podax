package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

public class PlayerStatus {

	public static PlayerStatus getCurrentState(Context context) {
		String[] projection = {
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
				EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
				EpisodeProvider.COLUMN_LAST_POSITION,
				EpisodeProvider.COLUMN_DURATION,
				EpisodeProvider.COLUMN_MEDIA_URL,
		};
		Cursor cursor = context.getContentResolver().query(EpisodeProvider.ACTIVE_EPISODE_URI, projection, null, null, null);
		PlayerStatus status = new PlayerStatus();
		if (cursor == null) {
			status._state = PlayerStates.PLAYLISTEMPTY;
			return status;
		}
		if (cursor.moveToNext()) {
			EpisodeCursor episode = new EpisodeCursor(cursor);
			SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
			status._state = PlayerStates.fromInt(prefs.getInt("playingState", PlayerStates.STOPPED.toInt()));
			status._episodeId = episode.getId();
			status._subscriptionId = episode.getSubscriptionId();
			status._title = episode.getTitle();
			status._subscriptionTitle = episode.getSubscriptionTitle();
			status._position = episode.getLastPosition();
			status._duration = episode.getDuration();
			status._filename = episode.getFilename(context);
		}
		cursor.close();
		return status;
	}

	public static void updateState(Context context, PlayerStates state) {
		SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
		prefs.edit().putInt("playingState", state.toInt()).apply();
		ActiveEpisodeReceiver.notifyExternal(context);
	}

	public static PlayerStates getPlayerState(Context context) {
		SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
		return PlayerStates.fromInt(prefs.getInt("playingState", -1));
	}

	// instance variables
	public enum PlayerStates {
		INVALID(-1),
		PLAYLISTEMPTY(0),
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
			return PLAYLISTEMPTY;
		}
	}

	private PlayerStates _state;
	private long _episodeId;
	private long _subscriptionId;
	private int _position;
	private int _duration;
	private String _title;
	private String _subscriptionTitle;
	private String _filename;

	private PlayerStatus() {
		_state = PlayerStates.PLAYLISTEMPTY;
		_episodeId = -1;
	}

	public PlayerStates getState() {
		return _state;
	}

	public boolean isPlaying() { return _state == PlayerStates.PLAYING; }

	public long getEpisodeId() {
		return _episodeId;
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

	public String getFilename() { return _filename; }

	public boolean hasActiveEpisode() {
		return getState() != PlayerStates.PLAYLISTEMPTY;
	}

	public boolean isPlayerServiceActive() {
		return getState() == PlayerStates.PLAYING || getState() == PlayerStates.PAUSED;
	}
}
