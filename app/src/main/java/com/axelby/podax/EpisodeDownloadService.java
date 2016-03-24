package com.axelby.podax;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.Episodes;
import com.axelby.podax.ui.MainActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okio.BufferedSource;

public class EpisodeDownloadService extends Service {
	private final static ArrayList<String> _currentlyDownloading = new ArrayList<>(5);
	public static boolean isDownloading(String filename) { return _currentlyDownloading.contains(filename); }

	public static void downloadEpisode(Context context, long episodeId) {
		Intent intent = new Intent(context, EpisodeDownloadService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODE);
		intent.putExtra(Constants.EXTRA_EPISODE_ID, episodeId);
		context.startService(intent);
	}

	public static void downloadEpisodes(Context context) {
		Intent intent = new Intent(context, EpisodeDownloadService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODES);
		intent.putExtra(Constants.EXTRA_MANUAL_REFRESH, true);
		context.startService(intent);
	}

	public static void downloadEpisodesSilently(Context context) {
		Intent intent = new Intent(context, EpisodeDownloadService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODES);
		context.startService(intent);
	}

	public EpisodeDownloadService() { }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new Thread(() -> {
			handleIntent(intent);
		}, "EpisodeDownloadService" + startId).start();
		return START_NOT_STICKY;
	}

	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;

		if (intent.getBooleanExtra(Constants.EXTRA_MANUAL_REFRESH, false) && Helper.isInvalidNetworkState(this)) {
			new Handler().post(() -> Toast.makeText(EpisodeDownloadService.this,
				R.string.update_request_no_wifi,
				Toast.LENGTH_SHORT).show());
			return;
		}

		if (action.equals(Constants.ACTION_DOWNLOAD_EPISODE)) {
			long episodeId = intent.getLongExtra(Constants.EXTRA_EPISODE_ID, -1L);
			if (episodeId == -1)
				return;

			// make sure we don't download too many episodes
			float maxEpisodes = PreferenceManager.getDefaultSharedPreferences(this).getFloat("queueMaxNumPodcasts", 10000);
			Integer downloadedEpisodes = Episodes.getDownloaded(this).count().toBlocking().first();
			if (downloadedEpisodes >= maxEpisodes)
				return;

			download(this, episodeId);
		} else if (action.equals(Constants.ACTION_DOWNLOAD_EPISODES)) {
			verifyDownloadedFiles(this);
			expireDownloadedFiles();

			Episodes.getNeedsDownload(this).subscribe(
				ep -> handleIntent(createDownloadEpisodeIntent(this, ep.getId())),
				e -> Log.e("EpisodeDownloadService", "unable to get episodes that need to be downloaded", e)
			);
		}
	}

	private static Intent createDownloadEpisodeIntent(Context context, long episodeId) {
		Intent intent = new Intent(context, EpisodeDownloadService.class);
		intent.setAction(Constants.ACTION_DOWNLOAD_EPISODE);
		intent.putExtra(Constants.EXTRA_EPISODE_ID, episodeId);
		return intent;
	}

	// make sure all media files in the folder are for existing episodes
	public static void verifyDownloadedFiles(Context context) {
		List<String> validMediaFilenames = Episodes.getPlaylist(context)
			.first()
			.flatMapIterable(eps -> eps)
			.map(ep -> ep.getFilename(context))
			.toList()
			.toBlocking().first();

		File dir = new File(EpisodeCursor.getPodcastStoragePath(context));
		File[] files = dir.listFiles();
		// this is possible if the directory does not exist
		if (files == null)
			return;
		for (File f : files) {
			// make sure the file is a media file
			String extension = EpisodeCursor.getExtension(f.getName());
			String[] mediaExtensions = new String[]{"m4a", "mp3", "ogg", "wma",};
			if (Arrays.binarySearch(mediaExtensions, extension) < 0)
				continue;
			if (!validMediaFilenames.contains(f.getAbsolutePath())) {
				Log.w("Podax", "deleting file " + f.getName());
				f.delete();
			}
		}
	}

	private void expireDownloadedFiles() {
		Episodes.getExpired(this).subscribe(
			ep -> ep.removeFromPlaylist(this),
			e -> Log.e("EpisodeDownloadService", "unable to expire downloaded files")
		);
	}

	public void download(Context context, long episodeId) {
		EpisodeCursor episode = null;
		FileOutputStream outStream = null;
		File mediaFile = null;
		try {
			if (Helper.isInvalidNetworkState(context)) {
				Log.d("EpisodeDownloader", "not downloading for wifi pref");
				return;
			}

			episode = EpisodeCursor.getCursor(context, episodeId);
			if (episode == null) {
				Log.d("EpisodeDownloader", "episode cursor is null");
				return;
			}
			if (episode.isDownloaded(context))
				return;

			// don't do two downloads simultaneously
			if (isDownloading(episode.getFilename(context))) {
				Log.d("EpisodeDownloader", "episode is already being downloaded");
				return;
			}

			mediaFile = new File(episode.getFilename(context));
			_currentlyDownloading.add(mediaFile.getAbsolutePath());

			File indexFile = new File(episode.getIndexFilename(context));
			if (indexFile.exists())
				indexFile.delete();

			OkHttpClient client = new OkHttpClient();
			Request.Builder url = new Request.Builder().url(episode.getMediaUrl());
			if (mediaFile.exists() && mediaFile.length() > 0)
				url.addHeader("Range", "bytes=" + mediaFile.length() + "-");
			Response response = client.newCall(url.build()).execute();
			if (response.code() == 416) {
				// 416 means range is invalid
				mediaFile.delete();
				url.removeHeader("Range");
				response = client.newCall(url.build()).execute();
			}
			if (response.code() != 200 && response.code() != 206) {
				Log.d("EpisodeDownloader", "response not 200 or 206");
				return;
			}
			if (response.code() == 200 && mediaFile.exists()) {
				mediaFile.delete();
			}

			ResponseBody body = response.body();

			long notificationWhen = System.currentTimeMillis();
			long lastWhen = notificationWhen;
			showNotification(context, episode, 0, (int) body.contentLength(), notificationWhen);

			new EpisodeEditor(context, episode.getId())
				.setFileSize(body.contentLength())
				.commit();

			outStream = new FileOutputStream(mediaFile, true);
			BufferedSource source = body.source();
			int readSum = 0;
			byte[] b = new byte[100000];
			while (!source.exhausted()) {
				int read = source.read(b);
				readSum += read;
				outStream.write(b, 0, read);

				// update notification with progress
				if (System.currentTimeMillis() - lastWhen > 1000) {
					showNotification(context, episode, readSum, (int) body.contentLength(), notificationWhen);
					lastWhen = System.currentTimeMillis();
				}
			}

			episode.determineDuration(context);
		} catch (Exception e) {
			showErrorNotification(context, episode, e);
			Log.e("Podax", "error while downloading", e);
		} finally {
			if (mediaFile != null)
				_currentlyDownloading.remove(mediaFile.getAbsolutePath());

			if (episode != null) {
				hideNotification(context, episode);
				episode.closeCursor();
			}

			try {
				if (outStream != null)
					outStream.close();
			} catch (IOException e) {
				Log.e("EpisodeDownloader", "unable to close output stream", e);
			}
		}
	}

	private void showErrorNotification(Context context, EpisodeCursor podcast, Exception e) {
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_stat_icon)
				.setTicker("Error downloading " + podcast.getTitle())
				.setWhen(System.currentTimeMillis())
				.setContentTitle("Error Downloading Podcast Episode")
				.setContentText(e.getMessage())
				.setContentIntent(contentIntent)
				.setOngoing(true)
				.build();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_DOWNLOADING_BASE + (int) podcast.getId(), notification);
	}

	private void showNotification(Context context, EpisodeCursor podcast, int bytesRead, int fileSize, long when) {
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_stat_icon)
				.setTicker("Downloading " + podcast.getTitle())
				.setWhen(when)
				.setContentTitle("Downloading Podcast Episode")
				.setContentText(podcast.getTitle())
				.setContentIntent(contentIntent)
				.setOngoing(true)
				.setProgress(fileSize, bytesRead, false)
				.build();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_DOWNLOADING_BASE + (int) podcast.getId(), notification);
	}

	private void hideNotification(Context context, EpisodeCursor podcast) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(ns);
		notificationManager.cancel(Constants.NOTIFICATION_DOWNLOADING_BASE + (int) podcast.getId());
	}
}