package com.axelby.podax;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Vector;

import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class UpdateService extends Service {
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

	private static final class UpdateServiceHandler extends Handler {
		private WeakReference<UpdateService> _service;

		public UpdateServiceHandler(UpdateService service, Looper looper) {
			super(looper);
			_service = new WeakReference<UpdateService>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			_service.get().handleIntent((Intent)msg.obj);

			try {
				if (!hasMessages(0)) {
					Thread.sleep(1000);
					// make sure service hasn't been garbage collected
					if (_service.get() == null)
						return;
					if (!hasMessages(0)) {
						_service.get().removeNotification();
						_service.get().stopSelf();
					}
				}
			} catch (InterruptedException e) {
				Log.e("Podax", "interrupted before stopping updateservice", e);
			}
		}
	}
	private volatile UpdateServiceHandler _handler;
	private volatile HandlerThread _handlerThread;

	private static UpdateService _singleton = null;
	public static boolean isRunning() {
		return _singleton != null;
	}

	Handler _uiHandler = new Handler();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		_handlerThread = new HandlerThread("UpdateService");
		_handlerThread.start();
		_handler = new UpdateServiceHandler(this, _handlerThread.getLooper());
	}

	@Override
	public void onDestroy() {
		_handlerThread.getLooper().quit();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false) && !Helper.ensureWifi(this)) {
			_uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(UpdateService.this,
							R.string.update_request_no_wifi,
							Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			Message msg = _handler.obtainMessage(0, intent.clone());
			_handler.sendMessage(msg);
		}

		return START_NOT_STICKY;
	}

	protected void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		if (action.equals(Constants.ACTION_REFRESH_ALL_SUBSCRIPTIONS)) {
			// when updating all subscriptions, send the list to the podax server
			sendSubscriptionsToPodaxServer();

			// sync with gpodder if it's installed and there's a linked account
			AccountManager am = AccountManager.get(this);
			if (Helper.isGPodderInstalled(this) && am.getAccountsByType("com.axelby.gpodder.account").length > 0)
				GPodderReceiver.syncWithProvider(this);

			String[] projection = new String[] { SubscriptionProvider.COLUMN_ID };
			Cursor c = getContentResolver().query(SubscriptionProvider.URI, projection, null, null, null);
			while (c.moveToNext())
				startService(createUpdateSubscriptionIntent(this, c.getLong(0)));
			c.close();
		} else if (action.equals(Constants.ACTION_REFRESH_SUBSCRIPTION)) {
			long subscriptionId = intent.getLongExtra(Constants.EXTRA_SUBSCRIPTION_ID, -1);
			if (subscriptionId == -1)
				return;
			new SubscriptionUpdater(this).update(subscriptionId);
		} else if (action.equals(Constants.ACTION_DOWNLOAD_PODCASTS)) {
			verifyDownloadedFiles();
			expireDownloadedFiles();

			String[] projection = { PodcastProvider.COLUMN_ID };
			Cursor c = getContentResolver().query(PodcastProvider.QUEUE_URI, projection, null, null, null);
			while (c.moveToNext())
				startService(createDownloadPodcastIntent(this, c.getLong(0)));
			c.close();
		} else if (action.equals(Constants.ACTION_DOWNLOAD_PODCAST)) {
			long podcastId = intent.getLongExtra(Constants.EXTRA_PODCAST_ID, -1L);
			if (podcastId == -1)
				return;
			new PodcastDownloader(this).download(podcastId);
		}
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
		// this is possible if the directory does not exist
		if (files == null)
			return;
		for (File f : files) {
			// make sure the file is a media file
			String extension = PodcastCursor.getExtension(f.getName());
			String[] mediaExtensions = new String[] { "mp3", "ogg", "wma", "m4a", };
			if (Arrays.binarySearch(mediaExtensions, extension) < 0)
				continue;
			if (!validMediaFilenames.contains(f.getAbsolutePath())) {
				Log.w("Podax", "deleting file " + f.getName());
				f.delete();
			}
		}
	}

	private void expireDownloadedFiles() {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Cursor c = getContentResolver().query(PodcastProvider.EXPIRED_URI, projection, null, null, null);
		while (c.moveToNext()) {
			PodcastCursor p = new PodcastCursor(c);
			new File(p.getFilename()).delete();
			p.removeFromQueue(this);
		}
		c.close();
	}

	private void removeNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(Constants.NOTIFICATION_UPDATE);
	}
}
