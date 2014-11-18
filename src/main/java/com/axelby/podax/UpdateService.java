package com.axelby.podax;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class UpdateService extends IntentService {
	Handler _uiHandler = new Handler();

	public UpdateService() {
		super("UpdateService");
	}

	public static void updateSubscriptions(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, long subscriptionId) {
		Intent intent = createUpdateSubscriptionIntent(context, subscriptionId);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, Uri subscriptionUri) {
		Integer subscriptionId = Integer.valueOf(subscriptionUri.getLastPathSegment());
		Intent intent = createUpdateSubscriptionIntent(context, subscriptionId);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
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

	private static Intent createDownloadEpisodeIntent(Context context, long subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODE);
		intent.putExtra(Constants.EXTRA_EPISODE_ID, subscriptionId);
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

	public void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		if (intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false) && !Helper.ensureWifiPref(this)) {
			_uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(UpdateService.this,
							R.string.update_request_no_wifi,
							Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}

		if (action.equals(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS)) {
			String[] projection = new String[]{SubscriptionProvider.COLUMN_ID};
			Cursor c = getContentResolver().query(SubscriptionProvider.URI, projection, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					handleIntent(createUpdateSubscriptionIntent(this, c.getLong(0)));
				c.close();
			}
		} else if (action.equals(Constants.ACTION_REFRESH_SUBSCRIPTION)) {
			long subscriptionId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (subscriptionId == -1)
				return;
			new SubscriptionUpdater(this).update(subscriptionId);
		} else if (action.equals(Constants.ACTION_DOWNLOAD_EPISODES)) {
			verifyDownloadedFiles();
			expireDownloadedFiles();

			String[] projection = {EpisodeProvider.COLUMN_ID};
			Cursor c = getContentResolver().query(EpisodeProvider.PLAYLIST_URI, projection, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					handleIntent(createDownloadEpisodeIntent(this, c.getLong(0)));
				c.close();
			}
		} else if (action.equals(Constants.ACTION_DOWNLOAD_EPISODE)) {
			long episodeId = intent.getLongExtra(Constants.EXTRA_EPISODE_ID, -1L);
			if (episodeId == -1)
				return;
			float maxEpisodes = PreferenceManager.getDefaultSharedPreferences(this).getFloat("queueMaxNumPodcasts", 10000);
			if (getPlaylistNumDownloadedItems() >= maxEpisodes)
				return;
			EpisodeDownloader.download(this, episodeId);
		}

		removeNotification();
	}

	// make sure all media files in the folder are for existing episodes
	private void verifyDownloadedFiles() {
		ArrayList<String> validMediaFilenames = new ArrayList<String>();
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
