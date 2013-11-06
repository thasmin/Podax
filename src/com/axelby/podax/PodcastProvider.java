package com.axelby.podax;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class PodcastProvider extends ContentProvider {
	public static String AUTHORITY = "com.axelby.podax.podcastprovider";
	public static Uri URI = Uri.parse("content://" + AUTHORITY + "/podcasts");
	public static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.axelby.podcast";
	public static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.axelby.podcast";
	public static final Uri QUEUE_URI = Uri.withAppendedPath(PodcastProvider.URI, "queue");
	public static final Uri SEARCH_URI = Uri.withAppendedPath(PodcastProvider.URI, "search");
	public static final Uri EXPIRED_URI = Uri.withAppendedPath(PodcastProvider.URI, "expired");
	public static final Uri ACTIVE_PODCAST_URI = Uri.parse("content://" + AUTHORITY + "/active");
	public static final Uri PLAYER_UPDATE_URI = Uri.parse("content://" + AUTHORITY + "/player_update");

	private final static int PODCASTS = 1;
	private final static int PODCASTS_QUEUE = 2;
	private final static int PODCAST_ID = 3;
	private final static int PODCASTS_TO_DOWNLOAD = 4;
	private final static int PODCAST_ACTIVE = 5;
	private final static int PODCASTS_SEARCH = 6;
	private final static int PODCASTS_EXPIRED = 7;
	private final static int PODCAST_PLAYER_UPDATE = 8;

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SUBSCRIPTION_ID = "subscriptionId";
	public static final String COLUMN_SUBSCRIPTION_TITLE = "subscriptionTitle";
	public static final String COLUMN_SUBSCRIPTION_THUMBNAIL = "subscriptionThumbnail";
	public static final String COLUMN_QUEUE_POSITION = "queuePosition";
	public static final String COLUMN_MEDIA_URL = "mediaUrl";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUB_DATE = "pubDate";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_FILE_SIZE = "fileSize";
	public static final String COLUMN_LAST_POSITION = "lastPosition";
	public static final String COLUMN_DURATION = "duration";
	public static final String COLUMN_DOWNLOAD_ID = "downloadId";
	public static final String COLUMN_PAYMENT = "payment";

	static final String PREF_ACTIVE = "active";

	static UriMatcher uriMatcher;
	static HashMap<String, String> _columnMap;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "podcasts", PODCASTS);
		uriMatcher.addURI(AUTHORITY, "podcasts/queue", PODCASTS_QUEUE);
		uriMatcher.addURI(AUTHORITY, "podcasts/#", PODCAST_ID);
		uriMatcher.addURI(AUTHORITY, "podcasts/to_download", PODCASTS_TO_DOWNLOAD);
		uriMatcher.addURI(AUTHORITY, "podcasts/search", PODCASTS_SEARCH);
		uriMatcher.addURI(AUTHORITY, "podcasts/expired", PODCASTS_EXPIRED);
		uriMatcher.addURI(AUTHORITY, "active", PODCAST_ACTIVE);
		uriMatcher.addURI(AUTHORITY, "player_update", PODCAST_PLAYER_UPDATE);

		_columnMap = new HashMap<String, String>();
		_columnMap.put(COLUMN_ID, "podcasts._id AS _id");
		_columnMap.put(COLUMN_TITLE, "podcasts.title AS title");
		_columnMap.put(COLUMN_SUBSCRIPTION_ID, "subscriptionId");
		_columnMap.put(COLUMN_SUBSCRIPTION_TITLE, "subscriptions.title AS subscriptionTitle");
		_columnMap.put(COLUMN_SUBSCRIPTION_THUMBNAIL, "subscriptions.thumbnail as subscriptionThumbnail");
		_columnMap.put(COLUMN_QUEUE_POSITION, "queuePosition");
		_columnMap.put(COLUMN_MEDIA_URL, "mediaUrl");
		_columnMap.put(COLUMN_LINK, "link");
		_columnMap.put(COLUMN_PUB_DATE, "pubDate");
		_columnMap.put(COLUMN_DESCRIPTION, "description");
		_columnMap.put(COLUMN_FILE_SIZE, "fileSize");
		_columnMap.put(COLUMN_LAST_POSITION, "lastPosition");
		_columnMap.put(COLUMN_DURATION, "duration");
		_columnMap.put(COLUMN_DOWNLOAD_ID, "downloadId");
		_columnMap.put(COLUMN_PAYMENT, "payment");

	}

	public static Uri getContentUri(long id) {
		return ContentUris.withAppendedId(URI, id);
	}

	DBAdapter _dbAdapter;

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case PODCASTS:
		case PODCASTS_QUEUE:
		case PODCASTS_TO_DOWNLOAD:
			return DIR_TYPE;
		case PODCAST_ID:
		case PODCAST_ACTIVE:
			return ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setProjectionMap(_columnMap);
		if (projection != null) {
			List<String> projectionList = Arrays.asList(projection);
			if (projectionList.contains(COLUMN_SUBSCRIPTION_TITLE) || projectionList.contains(COLUMN_SUBSCRIPTION_THUMBNAIL))
				sqlBuilder.setTables("podcasts JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id");
			else
				sqlBuilder.setTables("podcasts");
		} else {
			sqlBuilder.setTables("podcasts JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id");
		}

		switch (uriMatcher.match(uri)) {
		case PODCASTS:
			break;
		case PODCASTS_QUEUE:
			sqlBuilder.appendWhere("queuePosition IS NOT NULL");
			if (sortOrder == null)
				sortOrder = "queuePosition";
			break;
		case PODCAST_ID:
			sqlBuilder.appendWhere("podcasts._id = " + uri.getLastPathSegment());
			break;
		case PODCASTS_TO_DOWNLOAD:
			sqlBuilder.appendWhere("podcasts._id IN (" + getNeedsDownloadIds() + ")");
			if (sortOrder == null)
				sortOrder = "queuePosition";
			_dbAdapter.getWritableDatabase().execSQL("update podcasts set queueposition = (select count(*) from podcasts p2 where p2.queueposition is not null and p2.queueposition < podcasts.queueposition) where podcasts.queueposition is not null");
			break;
		case PODCAST_ACTIVE:
			SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
			if (prefs.contains(PREF_ACTIVE)) {
				long activeId = prefs.getLong(PREF_ACTIVE, -1);
				// make sure the active podcast wasn't deleted
				Cursor c = _dbAdapter.getReadableDatabase().query("podcasts", new String[] { "_id" }, "_id = ?", new String[] { String.valueOf(activeId) }, null, null, null);
				if (c.moveToFirst())
					sqlBuilder.appendWhere("podcasts._id = " + activeId);
				else {
					prefs.edit().remove(PREF_ACTIVE).commit();
					sqlBuilder.appendWhere("podcasts._id = " + getFirstDownloadedId());
				}
				c.close();
			} else {
				sqlBuilder.appendWhere("podcasts._id = " + getFirstDownloadedId());
			}
			break;
		case PODCASTS_SEARCH:
			sqlBuilder.appendWhere("LOWER(podcasts.title) LIKE ?");
			if (!selectionArgs[0].startsWith("%"))
				selectionArgs[0] = "%" + selectionArgs[0] + "%";
			if (sortOrder == null)
				sortOrder = "pubDate DESC";
			break;
		case PODCASTS_EXPIRED:
			String inWhere = "SELECT podcasts._id FROM podcasts " +
					"JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id " +
					"WHERE expirationDays IS NOT NULL AND queuePosition IS NOT NULL AND " +
					"date(pubDate, 'unixepoch', expirationDays || ' days') < date('now')";
			sqlBuilder.appendWhere("podcasts._id IN (" + inWhere + ")");
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	private long getFirstDownloadedId() {
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_FILE_SIZE,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Cursor c = query(QUEUE_URI, projection, null, null, null);
		long podcastId = -1;
		try {
			while (c.moveToNext()) {
				PodcastCursor podcast = new PodcastCursor(c);
				if (podcast.isDownloaded(getContext())) {
					podcastId = podcast.getId();
					break;
				}
			}
		} finally {
			c.close();
		}
		return podcastId;
	}

	private String getNeedsDownloadIds() {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		SQLiteQueryBuilder queueBuilder = new SQLiteQueryBuilder();
		queueBuilder.setTables("podcasts");
		queueBuilder.appendWhere("queuePosition IS NOT NULL");
		Cursor queue = queueBuilder.query(db,
				new String[] { "_id, mediaUrl, fileSize" }, null, null, null,
				null, "queuePosition");

		String queueIds = "";
		while (queue.moveToNext()) {
			PodcastCursor podcast = new PodcastCursor(queue);
			if (!podcast.isDownloaded(getContext()))
				queueIds = queueIds + queue.getLong(0) + ",";
		}
		queue.close();

		if (queueIds.length() > 0)
			queueIds = queueIds.substring(0, queueIds.length() - 1);
		return queueIds;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		int count = 0;

		long podcastId;
		SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
		Long activePodcastId = prefs.getLong(PREF_ACTIVE, -1);
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		int uriMatch = uriMatcher.match(uri);

		// process the player update separately
		if (uriMatch == PODCAST_PLAYER_UPDATE) {
			if (activePodcastId == -1)
				return 0;
			Cursor c = db.query("podcasts", new String[] { "lastPosition" },
					"_id = ?", new String[] { String.valueOf(activePodcastId) }, null, null, null);
			c.moveToFirst();
			int oldPosition = c.getInt(0);
			c.close();
			int newPosition = values.getAsInteger(COLUMN_LAST_POSITION);
			// reject changes if it's not a normal update
			if (newPosition < oldPosition || newPosition - oldPosition > 3000)
				return 0;

			db.update("podcasts", values, "_id = ?", new String[] { String.valueOf(activePodcastId) });
			getContext().getContentResolver().notifyChange(ACTIVE_PODCAST_URI, null);
			getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(URI, activePodcastId), null);
			ActivePodcastReceiver.NotifyExternal(getContext());
			return 1;
		}

		switch (uriMatch) {
		case PODCAST_ID:
			podcastId = ContentUris.parseId(uri);
			break;
		case PODCAST_ACTIVE:
			if (values.containsKey(COLUMN_ID)) {
				activePodcastId = values.getAsLong(COLUMN_ID);
				Editor editor = prefs.edit();
				if (activePodcastId != null)
					editor.putLong(PREF_ACTIVE, values.getAsLong(COLUMN_ID));
				else
					editor.remove(PREF_ACTIVE);
				editor.commit();

				// if we're clearing the active podcast or updating just the ID, don't go to the DB
				if (activePodcastId == null || values.size() == 1) {
					getContext().getContentResolver().notifyChange(ACTIVE_PODCAST_URI, null);
					return 0;
				}
			}
			
			// if we don't have an active podcast, don't update it
			if (activePodcastId == -1)
				return 0;

			podcastId = activePodcastId;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		String extraWhere = COLUMN_ID + " = " + podcastId;
		if (where != null)
			where = extraWhere + " AND " + where;
		else
			where = extraWhere;

		// subscription title and thumbnail is not in the table
		values.remove(COLUMN_SUBSCRIPTION_TITLE);
		values.remove(COLUMN_SUBSCRIPTION_THUMBNAIL);

		// update queuePosition separately
		if (values.containsKey(COLUMN_QUEUE_POSITION)) {
			// get the new position
			Integer newPosition = values.getAsInteger(COLUMN_QUEUE_POSITION);
			values.remove(COLUMN_QUEUE_POSITION);

			// no way to get changed record count until
			// SQLiteStatement.executeUpdateDelete in API level 11
			updateQueuePosition(podcastId, newPosition);

			// if this was the active podcast and it's no longer in the queue, pick the first downloaded in the queue
			if (activePodcastId == podcastId && newPosition == null) {
				prefs.edit().remove(PREF_ACTIVE).commit();
				activePodcastId = podcastId; // make sure the active podcast notification is sent
			}

			// if there is no active podcast, the active podcast may have changed
			if (activePodcastId == -1)
				activePodcastId = podcastId;
		}

		if (values.size() > 0)
			count += db.update("podcasts", values, where, whereArgs);
		getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(URI, podcastId), null);
		if (values.containsKey(COLUMN_FILE_SIZE))
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(URI, "to_download"), null);
		if (podcastId == activePodcastId) {
			getContext().getContentResolver().notifyChange(ACTIVE_PODCAST_URI, null);
			ActivePodcastReceiver.NotifyExternal(getContext());
		}
		// if the current podcast has updated the position but it's not from the player, tell the player to update
		if (podcastId == activePodcastId && uriMatch != PODCAST_PLAYER_UPDATE && values.containsKey(COLUMN_LAST_POSITION))
			getContext().getContentResolver().notifyChange(PLAYER_UPDATE_URI, null);

		return count;
	}

	public void updateQueuePosition(long podcastId, Integer newPosition) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// get the old position
		Cursor c = db.query("podcasts", new String[] { "queuePosition" },
				"_id = ?", new String[] { String.valueOf(podcastId) }, null, null, null);
		c.moveToFirst();
		Integer oldPosition = null;
		if (!c.isNull(0))
			oldPosition = c.getInt(0);
		c.close();

		// no need to remove from queue if it's not in queue
		if (oldPosition == null && newPosition == null)
			return;

		if (oldPosition == null && newPosition != null) {
			// new at 3: 1 2 3 4 5 do: 3++ 4++ 5++
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition + 1 "
					+ "WHERE queuePosition >= ?", new Object[] { newPosition });

			// download the newly added podcast
			UpdateService.downloadPodcastsSilently(getContext());
		} else if (oldPosition != null && newPosition == null) {
			// remove 3: 1 2 3 4 5 do: 4-- 5--
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition - 1 "
					+ "WHERE queuePosition > ?", new Object[] { oldPosition });

			// delete the podcast's file
			deleteDownload(getContext(), Long.valueOf(podcastId));
		} else if (oldPosition != newPosition) {
			// moving up: 1 2 3 4 5 2 -> 4: 3-- 4-- 2->4
			if (oldPosition < newPosition)
				db.execSQL(
						"UPDATE podcasts SET queuePosition = queuePosition - 1 "
								+ "WHERE queuePosition > ? AND queuePosition <= ?",
						new Object[] { oldPosition, newPosition });
			// moving down: 1 2 3 4 5 4 -> 2: 2++ 3++ 4->2
			if (newPosition < oldPosition)
				db.execSQL(
						"UPDATE podcasts SET queuePosition = queuePosition + 1 "
								+ "WHERE queuePosition >= ? AND queuePosition < ?",
						new Object[] { newPosition, oldPosition });
		}


		// if new position is max_value, put the podcast at the end
		if (newPosition != null && newPosition == Integer.MAX_VALUE) {
			Cursor max = db.rawQuery(
					"SELECT COALESCE(MAX(queuePosition) + 1, 0) FROM podcasts",
					null);
			max.moveToFirst();
			newPosition = max.getInt(0);
			max.close();
		}

		// update specified podcast
		db.execSQL("UPDATE podcasts SET queuePosition = ? WHERE _id = ?",
				new Object[] { newPosition, podcastId });
		getContext().getContentResolver().notifyChange(QUEUE_URI, null);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		if (!(uriMatcher.match(uri) == PODCASTS))
			throw new IllegalArgumentException("Illegal URI for insert");
		if (values.get(COLUMN_MEDIA_URL) == null)
			throw new IllegalArgumentException("mediaUrl is required field for podcast");

		Cursor mediaUrlCursor = db.rawQuery(
				"SELECT _id FROM podcasts WHERE mediaUrl = ?",
				new String[] { values.getAsString(COLUMN_MEDIA_URL) });
		Long podcastId = null;
		if (mediaUrlCursor.moveToNext())
			podcastId = mediaUrlCursor.getLong(0);
		mediaUrlCursor.close();

		if (podcastId != null) {
			if (values.containsKey(COLUMN_MEDIA_URL) && values.containsKey(COLUMN_FILE_SIZE)) {
				String file = PodcastCursor.getStoragePath(getContext()) +
						String.valueOf(podcastId) + "." +
						PodcastCursor.getExtension(values.getAsString(COLUMN_MEDIA_URL));
				// possible bug: file size shrinks for some reason -- don't use new one
				if (new File(file).length() > values.getAsInteger(COLUMN_FILE_SIZE))
					values.remove(COLUMN_FILE_SIZE);
			}
			db.update("podcasts", values, COLUMN_ID + " = ?", new String[] { String.valueOf(podcastId) });
		} else {
			podcastId = db.insert("podcasts", null, values);

			// find out if we should download new podcasts
			Cursor queueNewCursor = db.query("subscriptions",
					new String[] { SubscriptionProvider.COLUMN_QUEUE_NEW },
					"_id = ?",
					new String[] { String.valueOf(values.getAsLong(COLUMN_SUBSCRIPTION_ID)) },
					null, null, null);
			queueNewCursor.moveToFirst();
			boolean queueNew = queueNewCursor.getInt(0) != 0;
			queueNewCursor.close();

			// if the new podcast is less than 5 days old , and add it to the queue
			if (queueNew && values.containsKey(COLUMN_PUB_DATE)) {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, -5);
				if (new Date(values.getAsLong(COLUMN_PUB_DATE) * 1000L).after(c.getTime())) {
					updateQueuePosition(podcastId, Integer.MAX_VALUE);
				}
			}
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return PodcastProvider.getContentUri(podcastId);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		switch (uriMatcher.match(uri)) {
		case PODCASTS:
			break;
		case PODCAST_ID:
			String extraWhere = COLUMN_ID + " = " + uri.getLastPathSegment();
			if (where != null)
				where = extraWhere + " AND " + where;
			else
				where = extraWhere;
			break;
		default:
			throw new IllegalArgumentException("Illegal URI for delete");
		}
		
		// find out what we're deleting and delete downloaded files
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		String[] columns = new String[] { COLUMN_ID };
		String podcastsWhere = "queuePosition IS NOT NULL";
		if (where != null)
			podcastsWhere = where + " AND " + podcastsWhere;
		Cursor c = db.query("podcasts", columns, podcastsWhere, whereArgs, null, null, null);
		while (c.moveToNext()) {
			updateQueuePosition(c.getLong(0), null);
			deleteDownload(getContext(), c.getLong(0));
		}
		c.close();
		
		int count = db.delete("podcasts", where, whereArgs);
		if (!uri.equals(URI))
			getContext().getContentResolver().notifyChange(URI, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	public static void deleteDownload(Context context, final long podcastId) {
		File storage = new File(PodcastCursor.getStoragePath(context));
		File[] files = storage.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().startsWith(String.valueOf(podcastId)) &&
						pathname.getPath().endsWith(".mp3");
			}
		});
		for (File f : files)
			f.delete();
	}

	public static void restart(Context context, long podcastId) {
		restart(context, PodcastProvider.getContentUri(podcastId));
	}

	public static void restart(Context context, Uri uri) {
		ContentValues values = new ContentValues(1);
		values.put(PodcastProvider.COLUMN_LAST_POSITION, 0);
		context.getContentResolver().update(uri, values, null, null);
	}

	public static void movePositionTo(Context context, long podcastId, int position) {
		movePositionTo(context, PodcastProvider.getContentUri(podcastId), position);
	}

	public static void movePositionTo(Context context, Uri uri, int position) {
		ContentValues values = new ContentValues(1);
		values.put(PodcastProvider.COLUMN_LAST_POSITION, position);
		context.getContentResolver().update(uri, values, null, null);
	}

	public static void movePositionBy(Context context, long podcastId, int delta) {
		movePositionBy(context, PodcastProvider.getContentUri(podcastId), delta);
	}

	public static void movePositionBy(Context context, Uri uri, int delta) {
		String[] projection = new String[] { PodcastProvider.COLUMN_LAST_POSITION, PodcastProvider.COLUMN_DURATION };
		Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
		if (!c.moveToFirst()) {
			c.close();
			return;
		}
		int position = c.getInt(0);
		int duration = c.getInt(1);
		c.close();

		int newPosition = position + delta * 1000;
		if (newPosition < 0)
			newPosition = 0;
		if (duration != 0 && newPosition > duration)
			newPosition = duration;

		movePositionTo(context, uri, newPosition);
	}

	public static void skipToEnd(Context context, long podcastId) {
		skipToEnd(context, PodcastProvider.getContentUri(podcastId));
	}

	public static void skipToEnd(Context context, Uri uri) {
		String[] projection = new String[] { PodcastProvider.COLUMN_DURATION };
		Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
		if (!c.moveToFirst()) {
			c.close();
			return;
		}
		int duration = c.getInt(0);
		if (duration == 0)
			duration = new PodcastCursor(c).determineDuration(context);
		c.close();

		ContentValues values = new ContentValues(2);
		values.put(PodcastProvider.COLUMN_LAST_POSITION, duration);
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer)null);
		context.getContentResolver().update(uri, values, null, null);
	}
}
