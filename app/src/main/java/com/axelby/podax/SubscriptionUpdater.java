package com.axelby.podax;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;
import com.axelby.podax.ui.MainActivity;
import com.axelby.riasel.FeedParser;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okio.BufferedSink;
import okio.Okio;
import rx.Observable;

class SubscriptionUpdater {
	private final Context _context;

	public SubscriptionUpdater(Context context) {
		_context = context;
	}

	public void update(long subscriptionId) {
		try {
			if (Helper.isInvalidNetworkState(_context))
				return;

			SubscriptionData subscription = SubscriptionData.create(subscriptionId);
			if (subscription == null)
				return;

			showNotification(subscription);

			URL url = new URL(subscription.getUrl());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			if (subscription.getETag() != null)
				connection.setRequestProperty("If-None-Match", subscription.getETag());
			if (subscription.getLastModified() != null && subscription.getLastModified().getTime() > 0) {
				SimpleDateFormat imsFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
				connection.setRequestProperty("If-Modified-Since", imsFormat.format(subscription.getLastModified()));
			}

			int code = connection.getResponseCode();
			// only valid response code is 200
			// 304 (content not modified) is OK too
			if (code != 200) {
				ensureThumbnail(subscriptionId, subscription.getThumbnail());
				return;
			}

			final String[] thumbnail = {null};
			SubscriptionEditor subscriptionEditor = new SubscriptionEditor(subscriptionId);

			String eTag = connection.getHeaderField("ETag");
			if (eTag != null) {
				if (eTag.equals(subscription.getETag())) {
					ensureThumbnail(subscriptionId, subscription.getThumbnail());
					return;
				}
				subscriptionEditor.setEtag(eTag);
			}

			String encoding = connection.getContentEncoding();
			if (encoding == null) {
				String contentType = connection.getContentType();
				if (contentType != null && contentType.contains(";")) {
					encoding = contentType.split(";")[1].trim().substring("charset=".length());
				}
			}

			InputStream inputStream = connection.getInputStream();
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(inputStream, encoding);

				FeedParser feedParser = new FeedParser();
				feedParser.setOnFeedInfoHandler((feedParser1, feed) -> {
					thumbnail[0] = feed.getThumbnail();
					subscriptionEditor
						.setRawTitle(feed.getTitle())
						.setThumbnail(feed.getThumbnail())
						.setDescription(feed.getDescription())
						.setLastUpdate(feed.getLastBuildDate())
						.setLastModified(feed.getPubDate());
				});
				feedParser.setOnFeedItemHandler((feedParser1, item) -> {
					// mediaURL is required
					if (TextUtils.isEmpty(item.getMediaURL()))
						return;

					// if already exists, stop processing
					// this is for podcasts that keep every episode in their rss feed
					boolean exists = PodaxDB.episodes.getForMediaUrl(item.getMediaURL()) != null;
					if (exists) {
						feedParser1.stopProcessing();
						return;
					}

					EpisodeEditor.fromNew(subscriptionId, item.getMediaURL())
						.setTitle(item.getTitle())
						.setLink(item.getLink())
						.setDescription(item.getDescription())
						.setPubDate(item.getPublicationDate())
						.setFileSize(item.getMediaSize())
						.setPayment(item.getPaymentURL())
						.commit();
				});

				feedParser.parseFeed(parser);

			} catch (XmlPullParserException e) {
				// not much we can do about this
				Log.w("Podax", "error in subscription xml: " + e.getMessage());
				showUpdateErrorNotification(subscription, _context.getString(R.string.rss_not_valid));
			} finally {
				if (inputStream != null)
					inputStream.close();
			}

			// finish grabbing subscription values and update
			subscriptionEditor.setLastUpdate(new Date()).commit();

			String oldThumbnail = subscription.getThumbnail();
			String newThumbnail = thumbnail[0];
			downloadThumbnailImage(subscriptionId, oldThumbnail, newThumbnail);

			writeSubscriptionOPML();
		} catch (Exception e) {
			Log.e("Podax", "error while updating", e);
		} finally {
			EpisodeDownloadService.downloadEpisodesSilently(_context);
		}
	}

	private void ensureThumbnail(long subscriptionId, String thumbnailUrl) {
		String filename = SubscriptionData.getThumbnailFilename(subscriptionId);
		if (new File(filename).exists())
			return;
		downloadThumbnail(subscriptionId, thumbnailUrl);
	}

	private void downloadThumbnailImage(long subscriptionId, String oldThumbnailUrl, String newThumbnailUrl) {
		// if the thumbnail was removed
		if (newThumbnailUrl == null && oldThumbnailUrl != null) {
			SubscriptionData.evictThumbnails(subscriptionId);
		}
		// there's no current thumbnail
		if (newThumbnailUrl == null)
			return;
		// thumbnail hasn't changed
		if (newThumbnailUrl.equals(oldThumbnailUrl)) {
			ensureThumbnail(subscriptionId, newThumbnailUrl);
			return;
		}
		// thumbnail exists
		if (oldThumbnailUrl != null) {
			SubscriptionData.evictThumbnails(subscriptionId);
		}

		downloadThumbnail(subscriptionId, newThumbnailUrl);
	}

	private void downloadThumbnail(long subscriptionId, @NonNull String thumbnailUrl) {
		try {
			Request request = new Request.Builder().url(thumbnailUrl).build();
			Response response = new OkHttpClient().newCall(request).execute();
			String filename = SubscriptionData.getThumbnailFilename(subscriptionId);
			BufferedSink sink = Okio.buffer(Okio.sink(new File(filename)));
			sink.writeAll(response.body().source());
			sink.close();
		} catch (IOException e) {
			Log.e("Podax", "ioexception downloading subscription bitmap: " + thumbnailUrl);
		}
	}

	private void showUpdateErrorNotification(SubscriptionData subscription, String reason) {
		Intent notificationIntent = new Intent(_context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(_context)
				.setSmallIcon(R.drawable.ic_stat_icon)
				.setTicker("Error Updating Subscription")
				.setWhen(System.currentTimeMillis())
				.setContentTitle("Error updating " + subscription.getTitle())
				.setContentText(reason)
				.setContentIntent(contentIntent)
				.setOngoing(false)
				.build();

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.notify(Constants.SUBSCRIPTION_UPDATE_ERROR, notification);
	}


	void writeSubscriptionOPML() {
		try {
			File file = new File(_context.getExternalFilesDir(null), "podax.opml");
			FileOutputStream output = new FileOutputStream(file);
			XmlSerializer serializer = Xml.newSerializer();

			serializer.setOutput(output, "UTF-8");
			serializer.startDocument("UTF-8", true);
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

			serializer.startTag(null, "opml");
			serializer.attribute(null, "version", "1.0");

			serializer.startTag(null, "head");
			serializer.startTag(null, "title");
			serializer.text("Podax Subscriptions");
			serializer.endTag(null, "title");
			serializer.endTag(null, "head");

			serializer.startTag(null, "body");

			Observable.from(PodaxDB.subscriptions.getAll())
				.subscribe(sub -> {
					try {
						serializer.startTag(null, "outline");
						serializer.attribute(null, "type", "rss");
						serializer.attribute(null, "title", sub.getTitle());
						serializer.attribute(null, "xmlUrl", sub.getUrl());
						serializer.endTag(null, "outline");
					} catch (IOException e) {
						Log.e("SubscriptionUpdater", "unable to write subscription in opml file", e);
					}
				},
				e -> Log.e("SubscriptionUpdater", "unable to write opml file", e)
			);

			serializer.endTag(null, "body");
			serializer.endTag(null, "opml");

			serializer.endDocument();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void showNotification(SubscriptionData subscription) {
		Intent notificationIntent = new Intent(_context, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(_context)
				.setSmallIcon(R.drawable.ic_stat_icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle("Updating " + subscription.getTitle())
				.setContentIntent(contentIntent)
				.build();

		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_UPDATE, notification);
	}
}
