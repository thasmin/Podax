package com.axelby.podax;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Vector;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class UpdateService extends IntentService {
	public static void updateSubscriptions(Context context) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void updateSubscription(Context context, int subscriptionId) {
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

	public UpdateService() {
		super("Podax_UpdateService");
	}

	Handler _handler = new Handler();

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		if (intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false) && !Helper.ensureWifi(this)) {
			_handler.post(new Runnable() {
				public void run() {
					Toast.makeText(UpdateService.this,
							R.string.update_request_no_wifi,
							Toast.LENGTH_SHORT).show();
				}
			});
			return;
		}

		if (action.equals(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS)) {
			// when updating all subscriptions, send the list to the podax server
			sendSubscriptionsToPodaxServer();

			String[] projection = new String[] { SubscriptionProvider.COLUMN_ID };
			Cursor c = getContentResolver().query(SubscriptionProvider.URI, projection, null, null, null);
			while (c.moveToNext())
				startService(createUpdateSubscriptionIntent(this, c.getInt(0)));
			c.close();
		} else if (action.equals(Constants.ACTION_REFRESH_SUBSCRIPTION)) {
			int subscriptionId = intent.getIntExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (subscriptionId == -1)
				return;
			new SubscriptionUpdater(this).update(subscriptionId);
		} else if (action.equals(Constants.ACTION_DOWNLOAD_PODCASTS)) {
			verifyDownloadedFiles();

			String[] projection = { PodcastProvider.COLUMN_ID };
			Cursor c = getContentResolver().query(PodcastProvider.QUEUE_URI, projection, null, null, null);
			while (c.moveToNext())
				startService(createDownloadPodcastIntent(this, c.getInt(0)));
			c.close();
		} else if (action.equals(Constants.ACTION_DOWNLOAD_PODCAST)) {
			int podcastId = intent.getIntExtra(Constants.EXTRA_PODCAST_ID, -1);
			if (podcastId == -1)
				return;
			new PodcastDownloader(this).download(podcastId);
		}
	}

	private static Intent createUpdateSubscriptionIntent(Context context, int subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_REFRESH_SUBSCRIPTION);
		intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		return intent;
	}

	private static Intent createDownloadPodcastIntent(Context context, int subscriptionId) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_PODCAST);
		intent.putExtra(Constants.EXTRA_PODCAST_ID, subscriptionId);
		return intent;
	}

	public void sendSubscriptionsToPodaxServer() {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("usageDataPref", true) == false)
			return;

		// errors are acceptible
		try {
			URL podaxServer = new URL("http://www.axelby.com/podax.php");
			HttpURLConnection conn = (HttpURLConnection)podaxServer.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.connect();

			String[] projection = new String[] {
					SubscriptionProvider.COLUMN_URL,
			};
			Cursor c = getContentResolver().query(SubscriptionProvider.URI, projection, null, null, null);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write("inst=");
			wr.write(Installation.id(this));
			while (c.moveToNext()) {
				SubscriptionCursor subscription = new SubscriptionCursor(c);
				String url = subscription.getUrl();
				wr.write("&sub[");
				wr.write(String.valueOf(c.getPosition()));
				wr.write("]=");
				wr.write(URLEncoder.encode(url, "UTF-8"));
			}
			wr.flush();

			c.close();

			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			Vector<String> response = new Vector<String>();
			String line;
			while ((line = rd.readLine()) != null)
				response.add(line);
			if (response.size() > 1 || !response.get(0).equals("OK")) {
				Log.w("Podax", "Podax server error");
				Log.w("Podax", "------------------");
				for (String s : response)
					Log.w("Podax", s);
			}
		} catch (Exception ex) {
		}
	}

	// make sure all media files in the folder are for existing podcasts
	private void verifyDownloadedFiles() {
		Vector<String> validMediaFilenames = new Vector<String>();
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
		Cursor c = getContentResolver().query(queueUri, projection, null, null, null);
		while (c.moveToNext())
			validMediaFilenames.add(new PodcastCursor(c).getFilename());
		c.close();

		File dir = new File(PodcastCursor.getStoragePath());
		File[] files = dir.listFiles();
		for (File f : files) {
			// make sure the file is a media file
			String extension = PodcastCursor.getExtension(f.getName());
			// list of file extensions of media files considered to delete (sorted!)
			String[] mediaExtensions = new String[] { "m4a", "mp3", "ogg", "wma", };
			if (Arrays.binarySearch(mediaExtensions, extension) < 0)
				continue;
			if (!validMediaFilenames.contains(f.getAbsolutePath())) {
				Log.w("Podax", "deleting file " + f.getName());
				f.delete();
			}
		}
	}

}
