package com.axelby.podax;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;

import com.axelby.podax.R.drawable;
import com.axelby.podax.ui.MainActivity;
import com.axelby.podax.ui.PodcastDetailActivity;
import com.axelby.riasel.Feed;
import com.axelby.riasel.FeedItem;
import com.axelby.riasel.FeedParser;

public class SubscriptionUpdater {
	private Context _context;

	public SubscriptionUpdater(Context context) {
		_context = context;
	}
	
	public void update(long subscriptionId) {
		Cursor cursor = null;
		try {
			if (!Helper.ensureWifi(_context))
				return;

			Uri subscriptionUri = ContentUris.withAppendedId(SubscriptionProvider.URI, subscriptionId);
			String[] projection = new String[] {
					SubscriptionProvider.COLUMN_ID,
					SubscriptionProvider.COLUMN_TITLE,
					SubscriptionProvider.COLUMN_URL,
					SubscriptionProvider.COLUMN_ETAG,
					SubscriptionProvider.COLUMN_LAST_MODIFIED,
			};

			ContentValues subscriptionValues = new ContentValues();

			cursor = _context.getContentResolver().query(subscriptionUri, projection,
					SubscriptionProvider.COLUMN_ID + " = ?",
					new String[] { String.valueOf(subscriptionId) }, null);
			if (!cursor.moveToNext())
				return;
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);

			showNotification(subscription);

			URL url = new URL(subscription.getUrl());
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			if (subscription.getETag() != null)
				connection.setRequestProperty("If-None-Match", subscription.getETag());
			if (subscription.getLastModified() != null && subscription.getLastModified().getTime() > 0) {
				SimpleDateFormat imsFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
				connection.setRequestProperty("If-Modified-Since", imsFormat.format(subscription.getLastModified()));
			}

			int code = connection.getResponseCode();
			// only valid response code is 200
			// 304 (content not modified) is OK too
			if (code != 200) {
				return;
			}

			String eTag = connection.getHeaderField("ETag");
			if (eTag != null) {
				subscriptionValues.put(SubscriptionProvider.COLUMN_ETAG, eTag);
				if (eTag.equals(subscription.getETag()))
					return;
			}

			String encoding = connection.getContentEncoding();
			if (encoding == null) {
				String contentType = connection.getContentType();
				if (contentType != null && contentType.indexOf(";") > -1) {
					encoding = contentType.split(";")[1].trim().substring("charset=".length());
				}
			}

			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(connection.getInputStream(), encoding);
				Feed feed = FeedParser.parseFeed(parser);
				if (feed == null)
					return;

				subscriptionValues.putAll(feed.getContentValues());
				changeKeyString(subscriptionValues, "lastBuildDate", SubscriptionProvider.COLUMN_LAST_UPDATE);
				for (FeedItem item : feed.getItems()) {
					if (item.getMediaURL() == null || item.getMediaURL().length() == 0)
						continue;

					ContentValues podcastValues = item.getContentValues();
					podcastValues.put(PodcastProvider.COLUMN_SUBSCRIPTION_ID, subscriptionId);

					// translate Riasel keys to old Podax keys
					changeKeyString(podcastValues, "mediaURL", PodcastProvider.COLUMN_MEDIA_URL);
					changeKeyString(podcastValues, "mediaSize", PodcastProvider.COLUMN_FILE_SIZE);
					if (changeKeyLong(podcastValues, "publicationDate", PodcastProvider.COLUMN_PUB_DATE))
						podcastValues.put(PodcastProvider.COLUMN_PUB_DATE, podcastValues.getAsLong(PodcastProvider.COLUMN_PUB_DATE) / 1000);

					if (podcastValues.containsKey(PodcastProvider.COLUMN_MEDIA_URL)) {
						try {
							_context.getContentResolver().insert(PodcastProvider.URI, podcastValues);
						} catch (IllegalArgumentException e) {
							Log.w("Podax", "error while inserting podcast: " + e.getMessage());
						}
					}
				}
				if (feed.getThumbnail() != null)
					downloadThumbnail(subscriptionId, feed.getThumbnail());
			} catch (XmlPullParserException e) {
				// not much we can do about this
				Log.w("Podax", "error in subscription xml: " + e.getMessage());
				showUpdateErrorNotification(subscription, _context.getString(R.string.rss_not_valid));
			}

			// finish grabbing subscription values and update
			subscriptionValues.put(SubscriptionProvider.COLUMN_LAST_UPDATE, new Date().getTime() / 1000);
			_context.getContentResolver().update(subscriptionUri, subscriptionValues, null, null);

			writeSubscriptionOPML();
		} catch (Exception e) {
			Log.w("Podax", "error while updating: " + e.getMessage());
		} finally {
			if (cursor != null)
				cursor.close();
			UpdateService.downloadPodcastsSilently(_context);
		}
	}

	private boolean changeKeyString(ContentValues values, String oldKey, String newKey) {
		if (values.containsKey(oldKey)) {
			values.put(newKey, values.getAsString(oldKey));
			values.remove(oldKey);
			return true;
		}
		return false;
	};

	private boolean changeKeyLong(ContentValues values, String oldKey, String newKey) {
		if (values.containsKey(oldKey)) {
			values.put(newKey, values.getAsLong(oldKey));
			values.remove(oldKey);
			return true;
		}
		return false;
	};

	private void showUpdateErrorNotification(SubscriptionCursor subscription, String reason) {
		Intent notificationIntent = MainActivity.getSubscriptionIntent(_context);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(_context)
			.setSmallIcon(drawable.icon)
			.setTicker("Error Updating Subscription")
			.setWhen(System.currentTimeMillis())
			.setContentTitle("Error updating " + subscription.getTitle())
			.setContentText(reason)
			.setContentIntent(contentIntent)
			.setOngoing(false)
			.getNotification();

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(ns);
		notificationManager.notify(Constants.SUBSCRIPTION_UPDATE_ERROR, notification);
	}
	

	protected void writeSubscriptionOPML() {
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

			String[] projection = {
					SubscriptionProvider.COLUMN_TITLE,
					SubscriptionProvider.COLUMN_URL,
			};
			Cursor c = _context.getContentResolver().query(SubscriptionProvider.URI, projection , null, null, SubscriptionProvider.COLUMN_TITLE);
			while (c.moveToNext()) {
				SubscriptionCursor sub = new SubscriptionCursor(c);

				serializer.startTag(null, "outline");
				serializer.attribute(null, "type", "rss");
				serializer.attribute(null, "title", sub.getTitle());
				serializer.attribute(null, "xmlUrl", sub.getUrl());
				serializer.endTag(null, "outline");
			}
			c.close();

			serializer.endTag(null, "body");
			serializer.endTag(null, "opml");

			serializer.endDocument();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadThumbnail(long subscriptionId, String thumbnailUrl) {
		File thumbnailFile = new File(SubscriptionProvider.getThumbnailFilename(subscriptionId));
		if (thumbnailFile.exists())
			return;

		InputStream thumbIn;
		try {
			thumbIn = new URL(thumbnailUrl).openStream();
			FileOutputStream thumbOut = new FileOutputStream(thumbnailFile.getAbsolutePath());
			byte[] buffer = new byte[1024];
			int bufferLength = 0;
			while ( (bufferLength = thumbIn.read(buffer)) > 0 )
				thumbOut.write(buffer, 0, bufferLength);
			thumbOut.close();
			thumbIn.close();
		} catch (IOException e) {
			if (thumbnailFile.exists())
				thumbnailFile.delete();
		}
	}

	void showNotification(SubscriptionCursor subscription) {
		Intent notificationIntent = new Intent(_context, PodcastDetailActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(_context, 0, notificationIntent, 0);

		Notification notification = new NotificationCompat.Builder(_context)
			.setSmallIcon(drawable.icon)
			.setWhen(System.currentTimeMillis())
			.setContentTitle("Updating " + subscription.getTitle())
			.setContentIntent(contentIntent)
			.getNotification();

		NotificationManager notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Constants.NOTIFICATION_UPDATE, notification);
	}
}
