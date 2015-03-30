package com.axelby.podax;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class UpdateService extends IntentService {
	private final Handler _uiHandler = new Handler();

	public UpdateService() {
		super("UpdateService");
	}

	private static long _updatingSubscriptionId = -1;
	public static long getUpdatingSubscriptionId() { return _updatingSubscriptionId; }

	public static void updateSubscriptions(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, Uri subscriptionUri) {
		long subscriptionId = ContentUris.parseId(subscriptionUri);
		Intent intent = createUpdateSubscriptionIntent(context, subscriptionId);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void downloadEpisode(Context context, long episodeId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODE);
		intent.putExtra(Constants.EXTRA_EPISODE_ID, episodeId);
		context.startService(intent);
	}

	public static void downloadEpisodes(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODES);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void downloadEpisodesSilently(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODES);
		context.startService(intent);
	}

	private static Intent createUpdateSubscriptionIntent(Context context, long subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_SUBSCRIPTION);
		intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		return intent;
	}

	private static Intent createDownloadEpisodeIntent(Context context, long episodeId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODE);
		intent.putExtra(Constants.EXTRA_EPISODE_ID, episodeId);
		return intent;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onHandleIntent(Intent intent) {
		handleIntent(intent);
	}

	void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		if (intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false) && Helper.isInvalidNetworkState(this)) {
			_uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(UpdateService.this,
							R.string.update_request_no_wifi,
							Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}

		switch (action) {
			case Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS: {
				String[] projection = new String[]{SubscriptionProvider.COLUMN_ID};
				String selection = SubscriptionProvider.COLUMN_SINGLE_USE + " = 0";
				Cursor c = getContentResolver().query(SubscriptionProvider.URI, projection, selection, null, null);
				if (c != null) {
					while (c.moveToNext())
						handleIntent(createUpdateSubscriptionIntent(this, c.getLong(0)));
					c.close();
				}
				break;
			}
			case Constants.ACTION_REFRESH_SUBSCRIPTION:
				long subscriptionId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
				_updatingSubscriptionId = subscriptionId;
				if (subscriptionId == -1)
					return;

				LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
				Intent localIntent = new Intent(Constants.ACTION_UPDATE_SUBSCRIPTION, SubscriptionProvider.getContentUri(subscriptionId));
				localBroadcastManager.sendBroadcast(localIntent);

				new SubscriptionUpdater(this).update(subscriptionId);

				_updatingSubscriptionId = -1;
				localIntent = new Intent(Constants.ACTION_UPDATE_SUBSCRIPTION, SubscriptionProvider.getContentUri(-1));
				localBroadcastManager.sendBroadcast(localIntent);
				break;
			case Constants.ACTION_DOWNLOAD_EPISODES: {
				verifyDownloadedFiles();
				expireDownloadedFiles();

				String[] projection = {EpisodeProvider.COLUMN_ID};
				Cursor c = getContentResolver().query(EpisodeProvider.PLAYLIST_URI, projection, null, null, null);
				if (c != null) {
					while (c.moveToNext())
						handleIntent(createDownloadEpisodeIntent(this, c.getLong(0)));
					c.close();
				}
				break;
			}
			case Constants.ACTION_DOWNLOAD_EPISODE:
				long episodeId = intent.getLongExtra(Constants.EXTRA_EPISODE_ID, -1L);
				if (episodeId == -1)
					return;
				float maxEpisodes = PreferenceManager.getDefaultSharedPreferences(this).getFloat("queueMaxNumPodcasts", 10000);
				if (getPlaylistNumDownloadedItems() >= maxEpisodes)
					return;
				EpisodeDownloader.download(this, episodeId);
				break;
		}

		removeNotification();
	}

	// make sure all media files in the folder are for existing episodes
	private void verifyDownloadedFiles() {
		ArrayList<String> validMediaFilenames = new ArrayList<>();
		String[] projection = new String[]{
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_MEDIA_URL,
		};
		Uri playlistUri = Uri.withAppendedPath(EpisodeProvider.URI, "playlist");
		if (playlistUri == null)
			return;
		Cursor c = getContentResolver().query(playlistUri, projection, null, null, null);
		if (c == null)
			return;
		while (c.moveToNext())
			validMediaFilenames.add(new EpisodeCursor(c).getFilename(this));
		c.close();

		File dir = new File(EpisodeCursor.getStoragePath(this));
		File[] files = dir.listFiles();
		// this is possible if the directory does not exist
		if (files == null)
			return;
		for (File f : files) {
			// make sure the file is a media file
			String extension = EpisodeCursor.getExtension(f.getName());
			String[] mediaExtensions = new String[]{"mp3", "ogg", "wma", "m4a",};
			if (Arrays.binarySearch(mediaExtensions, extension) < 0)
				continue;
			if (!validMediaFilenames.contains(f.getAbsolutePath())) {
				Log.w("Podax", "deleting file " + f.getName());
				f.delete();
			}
		}
	}

	private void expireDownloadedFiles() {
		String[] projection = new String[]{
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_MEDIA_URL,
		};
		Cursor c = getContentResolver().query(EpisodeProvider.EXPIRED_URI, projection, null, null, null);
		if (c == null)
			return;
		while (c.moveToNext())
			new EpisodeCursor(c).removeFromPlaylist(this);
		c.close();
	}

	private int getPlaylistNumDownloadedItems() {
		String[] projection = {
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
				EpisodeProvider.COLUMN_MEDIA_URL,
				EpisodeProvider.COLUMN_FILE_SIZE,
		};
		Cursor c = getContentResolver().query(EpisodeProvider.PLAYLIST_URI, projection, null, null, null);
		if (c == null)
			return 0;
		int ret = 0;

		while (c.moveToNext()) {
			EpisodeCursor episode = new EpisodeCursor(c);
			if (episode.isDownloaded(this))
				ret++;
		}
		c.close();

		return ret;
	}

	private void removeNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_UPDATE);
	}
}
