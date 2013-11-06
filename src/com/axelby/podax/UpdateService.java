package com.axelby.podax;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Vector;

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

	public static void downloadPodcasts(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_PODCASTS);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void downloadPodcastsSilently(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_PODCASTS);
		context.startService(intent);
	}

	private static Intent createUpdateSubscriptionIntent(Context context, long subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_SUBSCRIPTION);
		intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		return intent;
	}

	private static Intent createDownloadPodcastIntent(Context context, long subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_PODCAST);
		intent.putExtra(Constants.EXTRA_PODCAST_ID, subscriptionId);
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

		if (intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false) && !Helper.ensureWifi(this)) {
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
			while (c.moveToNext())
				handleIntent(createUpdateSubscriptionIntent(this, c.getLong(0)));
			c.close();
		} else if (action.equals(Constants.ACTION_REFRESH_SUBSCRIPTION)) {
			long subscriptionId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (subscriptionId == -1)
				return;
			new SubscriptionUpdater(this).update(subscriptionId);
		} else if (action.equals(Constants.ACTION_DOWNLOAD_PODCASTS)) {
			verifyDownloadedFiles();
			expireDownloadedFiles();

			String[] projection = {PodcastProvider.COLUMN_ID};
			Cursor c = getContentResolver().query(PodcastProvider.QUEUE_URI, projection, null, null, null);
			while (c.moveToNext())
				handleIntent(createDownloadPodcastIntent(this, c.getLong(0)));
			c.close();
		} else if (action.equals(Constants.ACTION_DOWNLOAD_PODCAST)) {
			long podcastId = intent.getLongExtra(Constants.EXTRA_PODCAST_ID, -1L);
			if (podcastId == -1)
				return;
			PodcastDownloader.download(this, podcastId);
		}

		removeNotification();
	}

	// make sure all media files in the folder are for existing podcasts
	private void verifyDownloadedFiles() {
		Vector<String> validMediaFilenames = new Vector<String>();
		String[] projection = new String[]{
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
		Cursor c = getContentResolver().query(queueUri, projection, null, null, null);
		while (c.moveToNext())
			validMediaFilenames.add(new PodcastCursor(c).getFilename(this));
		c.close();

		File dir = new File(PodcastCursor.getStoragePath(this));
		File[] files = dir.listFiles();
		// this is possible if the directory does not exist
		if (files == null)
			return;
		for (File f : files) {
			// make sure the file is a media file
			String extension = PodcastCursor.getExtension(f.getName());
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
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Cursor c = getContentResolver().query(PodcastProvider.EXPIRED_URI, projection, null, null, null);
		while (c.moveToNext())
			new PodcastCursor(c).removeFromQueue(this);
		c.close();
	}

	private void removeNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_UPDATE);
	}
}
