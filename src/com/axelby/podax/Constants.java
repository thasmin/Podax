package com.axelby.podax;

import android.net.Uri;

public class Constants {
	public static final int NOTIFICATION_UPDATE = 1;
	public static final int SUBSCRIPTION_UPDATE_ERROR = 2;
	public static final int NOTIFICATION_PLAYING = 3;

	// active podcast related
	public static final Uri ACTIVE_PODCAST_DATA_RESTART = Uri.parse("podax://activepodcast/restart");
	public static final Uri ACTIVE_PODCAST_DATA_BACK = Uri.parse("podax://activepodcast/back");
	public static final Uri ACTIVE_PODCAST_DATA_FORWARD = Uri.parse("podax://activepodcast/forward");
	public static final Uri ACTIVE_PODCAST_DATA_END = Uri.parse("podax://activepodcast/end");

	public static final String ACTION_REFRESH_ALL_SUBSCRIPTIONS = "com.axelby.podax.REFRESH_ALL_SUBSCRIPTIONS";
	public static final String ACTION_REFRESH_SUBSCRIPTION = "com.axelby.podax.REFRESH_SUBSCRIPTION";
	public static final String ACTION_DOWNLOAD_PODCAST = "com.axelby.podax.DOWNLOAD_PODCAST";
	public static final String ACTION_DOWNLOAD_PODCASTS = "com.axelby.podax.DOWNLOAD_PODCASTS";

	public static final String EXTRA_PODCAST_ID = "com.axelby.podax.podcastId";
	public static final String EXTRA_SUBSCRIPTION_ID = "com.axelby.podax.subscriptionId";
	public static final String EXTRA_TITLE = "com.axelby.podax.title";
	public static final String EXTRA_URL = "com.axelby.podax.url";
	public static final String EXTRA_MANUAL_REFRESH = "com.axelby.podax.manual_refresh";
	public static final String EXTRA_FRAGMENT = "com.axelby.podax.fragmentId";

	public static final String EXTRA_PLAYER_COMMAND = "com.axelby.podax.player_command";
	public static final String EXTRA_PLAYER_COMMAND_ARG = "com.axelby.podax.player_command_arg";
	public static final int PLAYER_COMMAND_PLAYPAUSE = 0;
	public static final int PLAYER_COMMAND_PLAY = 1;
	public static final int PLAYER_COMMAND_PAUSE = 2;
	public static final int PLAYER_COMMAND_STOP = 3;
	public static final int PLAYER_COMMAND_PLAYSTOP = 4;
	public static final int PLAYER_COMMAND_RESUME = 5;

	// reasons for pausing
	public static final int PAUSE_AUDIOFOCUS = 0;
	public static final int PAUSE_MEDIABUTTON = 1;
	public static final int PAUSE_COUNT = 2;

	// gpodder constants
	public static final String GPODDER_ACCOUNT_TYPE = "com.axelby.gpodder";

}
