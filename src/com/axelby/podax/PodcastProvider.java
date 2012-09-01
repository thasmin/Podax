package com.axelby.podax;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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
	public static final Uri ACTIVE_PODCAST_URI = Uri.withAppendedPath(PodcastProvider.URI, "active");
	public static final Uri QUEUE_URI = Uri.withAppendedPath(PodcastProvider.URI, "queue");
	public static final Uri SEARCH_URI = Uri.withAppendedPath(PodcastProvider.URI, "search");

	private final static int PODCASTS = 1;
	private final static int PODCASTS_QUEUE = 2;
	private final static int PODCAST_ID = 3;
	private final static int PODCASTS_TO_DOWNLOAD = 4;
	private final static int PODCASTS_ACTIVE = 5;
	private final static int PODCASTS_SEARCH = 6;

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SUBSCRIPTION_ID = "subscriptionId";
	public static final String COLUMN_SUBSCRIPTION_TITLE = "subscriptionTitle";
	public static final String COLUMN_QUEUE_POSITION = "queuePosition";
	public static final String COLUMN_MEDIA_URL = "mediaUrl";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUB_DATE = "pubDate";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_FILE_SIZE = "fileSize";
	public static final String COLUMN_LAST_POSITION = "lastPosition";
	public static final String COLUMN_DURATION = "duration";

	static final String PREF_ACTIVE = "active";

	static UriMatcher uriMatcher;
	static HashMap<String, String> _columnMap;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "podcasts", PODCASTS);
		uriMatcher.addURI(AUTHORITY, "podcasts/queue", PODCASTS_QUEUE);
		uriMatcher.addURI(AUTHORITY, "podcasts/#", PODCAST_ID);
		uriMatcher.addURI(AUTHORITY, "podcasts/to_download", PODCASTS_TO_DOWNLOAD);
		uriMatcher.addURI(AUTHORITY, "podcasts/active", PODCASTS_ACTIVE);
		uriMatcher.addURI(AUTHORITY, "podcasts/search", PODCASTS_SEARCH);

		_columnMap = new HashMap<String, String>();
		_columnMap.put(COLUMN_ID, "podcasts._id AS _id");
		_columnMap.put(COLUMN_TITLE, "podcasts.title AS title");
		_columnMap.put(COLUMN_SUBSCRIPTION_ID, "subscriptionId");
		_columnMap.put(COLUMN_SUBSCRIPTION_TITLE, "subscriptions.title AS subscriptionTitle");
		_columnMap.put(COLUMN_QUEUE_POSITION, "queuePosition");
		_columnMap.put(COLUMN_MEDIA_URL, "mediaUrl");
		_columnMap.put(COLUMN_LINK, "link");
		_columnMap.put(COLUMN_PUB_DATE, "pubDate");
		_columnMap.put(COLUMN_DESCRIPTION, "description");
		_columnMap.put(COLUMN_FILE_SIZE, "fileSize");
		_columnMap.put(COLUMN_LAST_POSITION, "lastPosition");
		_columnMap.put(COLUMN_DURATION, "duration");

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
		case PODCASTS_ACTIVE:
			return ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setProjectionMap(_columnMap);
		if (Arrays.asList(projection).contains(COLUMN_SUBSCRIPTION_TITLE))
			sqlBuilder.setTables("podcasts JOIN subscriptions on podcasts.subscriptionId = subscriptions._id");
		else
			sqlBuilder.setTables("podcasts");

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
		case PODCASTS_ACTIVE:
			SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
			if (prefs.contains(PREF_ACTIVE))
				sqlBuilder.appendWhere("podcasts._id = " + prefs.getLong(PREF_ACTIVE, -1));
			else
				sqlBuilder.appendWhere("podcasts._id = " + getFirstDownloadedId());
			break;
		case PODCASTS_SEARCH:
			sqlBuilder.appendWhere("LOWER(podcasts.title) LIKE ?");
			if (!selectionArgs[0].startsWith("%"))
				selectionArgs[0] = "%" + selectionArgs[0] + "%";
			if (sortOrder == null)
				sortOrder = "pubDate DESC";
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
		String[] innerProjection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_FILE_SIZE,
				PodcastProvider.COLUMN_MEDIA_URL,
		};
		Cursor c = query(QUEUE_URI, innerProjection, null, null, null);
		long podcastId = -1;
		try {
			while (c.moveToNext()) {
				PodcastCursor podcast = new PodcastCursor(c);
				if (podcast.isDownloaded()) {
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
			if (!podcast.isDownloaded())
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

		String podcastId;
		SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
		Long activePodcastId = prefs.getLong(PREF_ACTIVE, -1);
		switch (uriMatcher.match(uri)) {
		case PODCAST_ID:
			podcastId = uri.getLastPathSegment();
			break;
		case PODCASTS_ACTIVE:
			if (values.containsKey(COLUMN_ID)) {
				activePodcastId = values.getAsLong(COLUMN_ID);
				Editor editor = prefs.edit();
				if (activePodcastId != null)
					editor.putLong(PREF_ACTIVE, values.getAsLong(COLUMN_ID));
				else
					editor.remove(PREF_ACTIVE);
				editor.commit();

				// tell everyone that there's a new active podcast
				Helper.updateWidgets(getContext());

				// if we're clearing the active podcast or updating just the ID, don't go to the DB
				if (activePodcastId == null || values.size() == 1)
					return 0;
			}
			
			// if we don't have an active podcast, don't update it
			if (activePodcastId == -1)
				return 0;

			podcastId = String.valueOf(activePodcastId);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		String extraWhere = COLUMN_ID + " = " + podcastId;
		if (where != null)
			where = extraWhere + " AND " + where;
		else
			where = extraWhere;

		// subscription title is not in the table
		values.remove(COLUMN_SUBSCRIPTION_TITLE);

		// update queuePosition separately
		if (values.containsKey(COLUMN_QUEUE_POSITION)) {
			// get the new position
			Integer newPosition = values.getAsInteger(COLUMN_QUEUE_POSITION);
			values.remove(COLUMN_QUEUE_POSITION);

			// no way to get changed record count until
			// SQLiteStatement.executeUpdateDelete in API level 11
			updateQueuePosition(podcastId, newPosition);
		}

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		if (values.size() > 0)
			count += db.update("podcasts", values, where, whereArgs);
		getContext().getContentResolver().notifyChange(Uri.withAppendedPath(URI, podcastId), null);
		if (values.containsKey(COLUMN_FILE_SIZE))
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(URI, "to_download"), null);
		if (Long.valueOf(podcastId).equals(activePodcastId))
			getContext().getContentResolver().notifyChange(ACTIVE_PODCAST_URI, null);
		return count;
	}

	public void updateQueuePosition(String podcastId, Integer newPosition) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// get the old position
		Cursor c = db.query("podcasts", new String[] { "queuePosition" },
				"_id = ?", new String[] { podcastId }, null, null, null);
		c.moveToFirst();
		Integer oldPosition = null;
		if (!c.isNull(0))
			oldPosition = c.getInt(0);
		c.close();

		// no need to remove from queue if it's not in queue
		if (oldPosition == null && newPosition == null)
			return;

		PodaxLog.log(getContext(), "podcast id " + podcastId + " moving from queue position " + oldPosition + " to " + newPosition);

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
			deleteDownload(Long.valueOf(podcastId));
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
		getContext().getContentResolver().notifyChange(Uri.withAppendedPath(URI, "queue"), null);

		// figure out if the active podcast changed
		// explicitly set as active or there is no active and first in queue changed
		SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activePodcastId = prefs.getLong(PREF_ACTIVE, -1);
		if (String.valueOf(activePodcastId).equals(podcastId)) {
			prefs.edit().remove(PREF_ACTIVE).commit();
			Helper.updateWidgets(getContext());
		} else if (activePodcastId == -1 &&
				((oldPosition != null && oldPosition == 0) ||
				  newPosition != null && newPosition == 0)) {
			Helper.updateWidgets(getContext());
		}
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
				String file = PodcastCursor.getStoragePath() +
						String.valueOf(podcastId) + "." +
						PodcastCursor.getExtension(values.getAsString(COLUMN_MEDIA_URL));
				// possible bug: file size shrinks for some reason -- don't use new one
				if (new File(file).length() > values.getAsInteger(COLUMN_FILE_SIZE)) {
					PodaxLog.log(getContext(), "podcast id " + podcastId + ": new file size (" + values.getAsInteger(COLUMN_FILE_SIZE) + " is less than existing file" + new File(file).length());
					values.remove(COLUMN_FILE_SIZE);
				}
			}
			db.update("podcasts", values, COLUMN_ID + " = ?",
					new String[] { String.valueOf(podcastId) });
		} else {
			podcastId = db.insert("podcasts", null, values);
			// if the new podcast is less than 5 days old, add it to the queue
			if (values.containsKey(COLUMN_PUB_DATE)) {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, -5);
				if (new Date(values.getAsLong(COLUMN_PUB_DATE) * 1000L).after(c.getTime())) {
					updateQueuePosition(String.valueOf(podcastId), Integer.MAX_VALUE);
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
			updateQueuePosition(String.valueOf(c.getLong(0)), null);
			deleteDownload(c.getLong(0));
		}
		c.close();
		
		int count = db.delete("podcasts", where, whereArgs);
		if (!uri.equals(URI))
			getContext().getContentResolver().notifyChange(URI, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	public static void deleteDownload(final long podcastId) {
		File storage = new File(PodcastCursor.getStoragePath());
		File[] files = storage.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().startsWith(String.valueOf(podcastId)) &&
						pathname.getPath().endsWith(".mp3");
			}
		});
		for (File f : files)
			f.delete();
	}
}
