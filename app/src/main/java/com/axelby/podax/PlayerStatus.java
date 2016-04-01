package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.PodaxDB;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public class PlayerStatus {

	private static BehaviorSubject<PlayerStatus> _subject = BehaviorSubject.create();
	public static Observable<PlayerStatus> watch() { return _subject; }

	private static BehaviorSubject<PlayerStatus> _nonPlayerUpdates = BehaviorSubject.create();
	public static Observable<PlayerStatus> watchNonPlayerUpdates() { return _nonPlayerUpdates; }

	public static void update(Context context) {
		PlayerStatus state = getCurrentState(context);
		_subject.onNext(state);
		_nonPlayerUpdates.onNext(state);
	}

	public static void updateFromPlayer(Context context) {
		_subject.onNext(getCurrentState(context));
	}

	public static PlayerStatus getCurrentState(Context context) {
		EpisodeData episode = PodaxDB.episodes.getActive();
		PlayerStatus status = new PlayerStatus();
		if (episode == null) {
			status._state = PlayerStates.PLAYLISTEMPTY;
			return status;
		}

		SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
		status._state = PlayerStates.fromInt(prefs.getInt("playingState", PlayerStates.STOPPED.toInt()));
		status._episodeId = episode.getId();
		status._subscriptionId = episode.getSubscriptionId();
		status._title = episode.getTitle();
		status._subscriptionTitle = episode.getSubscriptionTitle();
		status._position = episode.getLastPosition();
		status._duration = episode.getDuration();
		status._isDownloaded = episode.isDownloaded(context);

		return status;
	}

	public static void updateState(Context context, PlayerStates state) {
		SharedPreferences prefs = context.getSharedPreferences("player", Context.MODE_PRIVATE);
		prefs.edit().putInt("playingState", state.toInt()).apply();
		ActiveEpisodeReceiver.notifyExternal(context);
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
	private boolean _isDownloaded;

	private PlayerStatus() {
		_state = PlayerStates.PLAYLISTEMPTY;
		_episodeId = -1;
	}

	public PlayerStates getState() {
		return _state;
	}
	public boolean isPlaying() { return _state == PlayerStates.PLAYING; }
	public long getEpisodeId() { return _episodeId; }
	public long getSubscriptionId() { return _subscriptionId; }
	public int getPosition() { return _position; }
	public int getDuration() { return _duration; }
	public String getTitle() { return _title; }
	public String getSubscriptionTitle() { return _subscriptionTitle; }
	public boolean isEpisodeDownloaded() { return _isDownloaded; }
	public boolean hasActiveEpisode() { return getState() != PlayerStates.PLAYLISTEMPTY; }
}
