package com.axelby.podax;

import android.net.Uri;

public class Constants {
	public static final int NOTIFICATION_UPDATE = 1;
	public static final int SUBSCRIPTION_UPDATE_ERROR = 2;
	public static final int NOTIFICATION_PLAYING = 3;
	
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
}
