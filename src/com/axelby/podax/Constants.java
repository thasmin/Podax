package com.axelby.podax;

import android.net.Uri;

public class Constants {
	public static final int NOTIFICATION_UPDATE = 1;
	public static final int SUBSCRIPTION_UPDATE_ERROR = 2;
	public static final int NOTIFICATION_PLAYING = 3;
	
	public static final String PERMISSION_PLAYERCHANGES = "com.axelby.podax.player.CHANGES";

	public static final String ACTION_PLAYER_POSITIONCHANGED = "com.axelby.podax.player.POSITIONCHANGED";
	public static final String ACTION_PLAYER_STATECHANGED = "com.axelby.podax.player.STATECHANGED";
	public static final String ACTION_PLAYER_ACTIVEPODCASTCHANGED = "com.axelby.podax.player.ACTIVEPODCASTCHANGED";
	public static final String ACTION_PODCAST_DOWNLOADED = "com.axelby.podax.PODCAST_DOWNLOADED";
	public static final String ACTION_REFRESH_ALL_SUBSCRIPTIONS = "com.axelby.podax.REFRESH_ALL_SUBSCRIPTIONS";
	public static final String ACTION_REFRESH_SUBSCRIPTION = "com.axelby.podax.REFRESH_SUBSCRIPTION";
	public static final String ACTION_DOWNLOAD_PODCAST = "com.axelby.podax.DOWNLOAD_PODCAST";
	public static final String ACTION_DOWNLOAD_PODCASTS = "com.axelby.podax.DOWNLOAD_PODCASTS";

	public static final Uri GPODDER_URI = Uri.parse("content://com.axelby.gpodder.podcasts");
	
	public static final String EXTRA_PODCAST_ID = "com.axelby.podax.podcastId";
	public static final String EXTRA_SUBSCRIPTION_ID = "com.axelby.podax.subscriptionId";
	public static final String EXTRA_CATEGORY = "com.axelby.podax.category";
	public static final String EXTRA_TITLE = "com.axelby.podax.title";
	public static final String EXTRA_URL = "com.axelby.podax.url";
	public static final String EXTRA_POPULAR_SOURCE_URL = "com.axelby.podax.popular_source_url";
	public static final String EXTRA_POPULAR_SOURCE_NAME = "com.axelby.popular_source_name";
	public static final String EXTRA_MANUAL_REFRESH = "com.axelby.podax.manual_refresh";
	public static final String EXTRA_TAB = "com.axelby.podax.tab";
	public static final String EXTRA_POSITION = "com.axelby.podax.position";
	public static final String EXTRA_DURATION = "com.axelby.podax.duration";
	public static final String EXTRA_PLAYERSTATE = "com.axelby.podax.playerstate";

	public static final String EXTRA_PLAYER_COMMAND = "com.axelby.podax.player_command";
	public static final String EXTRA_PLAYER_COMMAND_ARG = "com.axelby.podax.player_command_arg";
	public static final int PLAYER_COMMAND_SKIPTOEND = 0;
	public static final int PLAYER_COMMAND_RESTART = 1;
	public static final int PLAYER_COMMAND_SKIPBACK = 2;
	public static final int PLAYER_COMMAND_SKIPFORWARD = 3;
	public static final int PLAYER_COMMAND_PLAYPAUSE = 4;
	public static final int PLAYER_COMMAND_PLAY = 5;
	public static final int PLAYER_COMMAND_PAUSE = 6;
	public static final int PLAYER_COMMAND_SKIPTO = 7;
	public static final int PLAYER_COMMAND_PLAY_SPECIFIC_PODCAST = 8;
	public static final int PLAYER_COMMAND_STOP = 9;
	public static final int PLAYER_COMMAND_PLAYSTOP = 10;
	public static final int PLAYER_COMMAND_RESUME = 11;

	// reasons for pausing
	public static final int PAUSE_AUDIOFOCUS = 0;
	public static final int PAUSE_MEDIABUTTON = 1;
	public static final int PAUSE_COUNT = 2;
}
