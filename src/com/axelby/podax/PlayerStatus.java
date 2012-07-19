package com.axelby.podax;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import com.squareup.otto.Bus;

public class PlayerStatus {

	// internal event bus and state
	private static final Bus _bus = new Bus("PlayerStatus");
	private static PlayerStatus _current;
	private static final Handler _handler = new Handler();
	private static final Runnable _runnable = new Runnable() {
		@Override
		public void run() {
			_bus.post(_current);
		}
	};

	public static PlayerStatus getCurrentState() {
		return _current;
	}

	public static boolean isPlaying() {
		return _current.getState() == PlayerStates.PLAYING;
	}

	public static boolean isPaused() {
		return _current.getState() == PlayerStates.PAUSED;
	}

	public static boolean isStopped() {
		return _current.getState() == PlayerStates.STOPPED;
	}

	private static void postWithHandler() {
		_handler.post(_runnable);
	}

	public static void updateState(PlayerStates state) {
		_current._state = state;
		postWithHandler();
	}

	public static void updatePosition(int position) {
		_current._position = position;
		postWithHandler();
	}

	public static void updatePodcast(long id, String podcast, String subscription, int position, int duration) {
		_current._id = id;
		_current._title = podcast;
		_current._subscriptionTitle = subscription;
		_current._position = position;
		_current._duration = duration;
		postWithHandler();
	}

	public static void updateQueueEmpty() {
		_current = new PlayerStatus();
		postWithHandler();
	}

	public static void register(Object obj) {
		_bus.register(obj);
	}

	public static void unregister(Object obj) {
		_bus.unregister(obj);
	}

	public static void refresh(Context context) {
		_current = null;
		initialize(context);
		postWithHandler();
		Helper.updateWidgets(context);
	}

	public static void initialize(Context context) {
		if (_current != null)
			return;

		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_DURATION,
		};
		Uri activeUri = Uri.withAppendedPath(PodcastProvider.URI, "active");
		Cursor cursor = context.getContentResolver().query(activeUri, projection, null, null, null);
		_current = new PlayerStatus();
		if (cursor.moveToNext()) {
			_current._state = PlayerStates.STOPPED;
			_current._id = cursor.getLong(0);
			_current._title = cursor.getString(1);
			_current._subscriptionTitle = cursor.getString(2);
			_current._position = cursor.getInt(3);
			_current._duration = cursor.getInt(4);
		}
		cursor.close();
	}

	// instance variables
	public enum PlayerStates {
		QUEUEEMPTY, STOPPED, PAUSED, PLAYING
	}

	private PlayerStates _state;
	private long _id;
	private int _position;
	private int _duration;
	private String _title;
	private String _subscriptionTitle;

	private PlayerStatus() {
		_state = PlayerStates.QUEUEEMPTY;
	}

	public PlayerStates getState() {
		return _state;
	}

	public long getId() {
		return _id;
	}

	public int getPosition() {
		return _position;
	}

	public int getDuration() {
		return _duration;
	}

	public String getTitle() {
		return _title;
	}

	public String getSubscriptionTitle() {
		return _subscriptionTitle;
	}

	public boolean hasActivePodcast() {
		return getState() != PlayerStates.QUEUEEMPTY;
	}

}
