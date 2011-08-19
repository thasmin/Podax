package com.axelby.podax;

public class Constants {
	public static final int SUBSCRIPTION_UPDATE_ONGOING = 1;
	public static final int SUBSCRIPTION_UPDATE_ERROR = 2;
	public static final int PODCAST_DOWNLOAD_ONGOING = 3;
	
	public static final String SUBSCRIPTION_UPDATE_BROADCAST = "com.axelby.podax.SUBSCRIPTION_UPDATE_BROADCAST";
	
	public static final String EXTRA_PODCAST_ID = "com.axelby.podax.podcastId";

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
}
