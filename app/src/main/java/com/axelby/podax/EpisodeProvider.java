package com.axelby.podax;

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
import android.support.annotation.NonNull;

import com.axelby.podax.model.DBAdapter;
import com.axelby.podax.model.Episodes;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class EpisodeProvider extends ContentProvider {
	private static final String AUTHORITY = "com.axelby.podax.podcastprovider";
	public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/episodes");
	private static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.axelby.podcast";
	private static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.axelby.podcast";
	public static final Uri PLAYLIST_URI = Uri.withAppendedPath(EpisodeProvider.URI, "playlist");
	public static final Uri TO_DOWNLOAD_URI = Uri.withAppendedPath(EpisodeProvider.URI, "to_download");
	public static final Uri SEARCH_URI = Uri.withAppendedPath(EpisodeProvider.URI, "search");
	public static final Uri EXPIRED_URI = Uri.withAppendedPath(EpisodeProvider.URI, "expired");
	public static final Uri ACTIVE_EPISODE_URI = Uri.parse("content://" + AUTHORITY + "/active");
	public static final Uri PLAYER_UPDATE_URI = Uri.parse("content://" + AUTHORITY + "/player_update");
	public static final Uri NEED_GPODDER_UPDATE_URI = Uri.withAppendedPath(EpisodeProvider.URI, "need_gpodder_update");
	public static final Uri LATEST_ACTIVITY_URI = Uri.withAppendedPath(EpisodeProvider.URI, "latest_activity");
	public static final Uri FINISHED_URI = Uri.withAppendedPath(EpisodeProvider.URI, "finished");

	private final static int EPISODES = 1;
	private final static int EPISODES_PLAYLIST = 2;
	private final static int EPISODE_ID = 3;
	private final static int EPISODES_TO_DOWNLOAD = 4;
	private final static int EPISODE_ACTIVE = 5;
	private final static int EPISODES_SEARCH = 6;
	private final static int EPISODES_EXPIRED = 7;
	private final static int EPISODE_PLAYER_UPDATE = 8;
	private final static int EPISODES_NEED_GPODDER_UPDATE = 9;
	private final static int EPISODES_LATEST_ACTIVITY = 10;
	private final static int EPISODES_FINISHED = 11;

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SUBSCRIPTION_ID = "subscriptionId";
	public static final String COLUMN_SUBSCRIPTION_TITLE = "subscriptionTitle";
	public static final String COLUMN_SUBSCRIPTION_URL = "subscriptionUrl";
	public static final String COLUMN_PLAYLIST_POSITION = "queuePosition";
	public static final String COLUMN_MEDIA_URL = "mediaUrl";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUB_DATE = "pubDate";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_FILE_SIZE = "fileSize";
	public static final String COLUMN_LAST_POSITION = "lastPosition";
	public static final String COLUMN_DURATION = "duration";
	public static final String COLUMN_NEEDS_GPODDER_UPDATE = "needsGpodderUpdate";
	public static final String COLUMN_GPODDER_UPDATE_TIMESTAMP = "gpodderUpdateTimestamp";
	public static final String COLUMN_PAYMENT = "payment";
	public static final String COLUMN_FINISHED_TIME = "finishedTime";

	private static final String PREF_ACTIVE = "active";

	private static final UriMatcher uriMatcher;

	public static HashMap<String, String> getColumnMap() {
		return _columnMap;
	}

	private static final HashMap<String, String> _columnMap;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "episodes", EPISODES);
		uriMatcher.addURI(AUTHORITY, "episodes/playlist", EPISODES_PLAYLIST);
		uriMatcher.addURI(AUTHORITY, "episodes/#", EPISODE_ID);
		uriMatcher.addURI(AUTHORITY, "episodes/to_download", EPISODES_TO_DOWNLOAD);
		uriMatcher.addURI(AUTHORITY, "episodes/search", EPISODES_SEARCH);
		uriMatcher.addURI(AUTHORITY, "episodes/expired", EPISODES_EXPIRED);
		uriMatcher.addURI(AUTHORITY, "active", EPISODE_ACTIVE);
		uriMatcher.addURI(AUTHORITY, "player_update", EPISODE_PLAYER_UPDATE);
		uriMatcher.addURI(AUTHORITY, "episodes/need_gpodder_update", EPISODES_NEED_GPODDER_UPDATE);
		uriMatcher.addURI(AUTHORITY, "episodes/latest_activity", EPISODES_LATEST_ACTIVITY);
		uriMatcher.addURI(AUTHORITY, "episodes/finished", EPISODES_FINISHED);

		_columnMap = new HashMap<>();
		_columnMap.put(COLUMN_ID, "podcasts._id AS _id");
		_columnMap.put(COLUMN_TITLE, "podcasts.title AS title");
		_columnMap.put(COLUMN_SUBSCRIPTION_ID, "subscriptionId");
		_columnMap.put(COLUMN_SUBSCRIPTION_TITLE, "subscriptions.title AS subscriptionTitle");
		_columnMap.put(COLUMN_SUBSCRIPTION_URL, "subscriptions.url as subscriptionUrl");
		_columnMap.put(COLUMN_PLAYLIST_POSITION, "queuePosition");
		_columnMap.put(COLUMN_MEDIA_URL, "mediaUrl");
		_columnMap.put(COLUMN_LINK, "link");
		_columnMap.put(COLUMN_PUB_DATE, "pubDate");
		_columnMap.put(COLUMN_DESCRIPTION, "podcasts.description AS description");
		_columnMap.put(COLUMN_FILE_SIZE, "fileSize");
		_columnMap.put(COLUMN_LAST_POSITION, "lastPosition");
		_columnMap.put(COLUMN_DURATION, "duration");
		_columnMap.put(COLUMN_NEEDS_GPODDER_UPDATE, "needsGpodderUpdate");
		_columnMap.put(COLUMN_GPODDER_UPDATE_TIMESTAMP, "gpodderUpdateTimestamp");
		_columnMap.put(COLUMN_PAYMENT, "payment");
		_columnMap.put(COLUMN_FINISHED_TIME, "finishedTime");
	}

	public static Uri getContentUri(long id) {
		return ContentUris.withAppendedId(URI, id);
	}

	private DBAdapter _dbAdapter;

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		switch (uriMatcher.match(uri)) {
			case EPISODES:
			case EPISODES_PLAYLIST:
			case EPISODES_TO_DOWNLOAD:
				return DIR_TYPE;
			case EPISODE_ID:
			case EPISODE_ACTIVE:
				return ITEM_TYPE;
			default:
				throw new IllegalArgumentException("Unknown URI");
		}
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setProjectionMap(_columnMap);
		if (projection != null) {
			List<String> projectionList = Arrays.asList(projection);
			if (projectionList.contains(COLUMN_SUBSCRIPTION_TITLE)
					|| projectionList.contains(COLUMN_SUBSCRIPTION_URL))
				sqlBuilder.setTables("podcasts JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id");
			else
				sqlBuilder.setTables("podcasts");
		} else {
			projection = _columnMap.values().toArray(new String[_columnMap.size()]);
			sqlBuilder.setTables("podcasts JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id");
		}

		String limit = null;
		switch (uriMatcher.match(uri)) {
			case EPISODES:
				break;
			case EPISODES_PLAYLIST:
				sqlBuilder.appendWhere("queuePosition IS NOT NULL");
				if (sortOrder == null)
					sortOrder = "queuePosition";
				break;
			case EPISODE_ID:
				sqlBuilder.appendWhere("podcasts._id = " + uri.getLastPathSegment());
				break;
			case EPISODES_TO_DOWNLOAD:
				sqlBuilder.appendWhere("podcasts._id IN (" + getNeedsDownloadIds() + ")");
				if (sortOrder == null)
					sortOrder = "queuePosition";
				_dbAdapter.getWritableDatabase().execSQL("update podcasts set queueposition = (select count(*) from podcasts p2 where p2.queueposition is not null and p2.queueposition < podcasts.queueposition) where podcasts.queueposition is not null");
				break;
			case EPISODE_ACTIVE:
				if (getContext() != null) {
					SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
					if (prefs.contains(PREF_ACTIVE)) {
						long activeId = prefs.getLong(PREF_ACTIVE, -1);
						// make sure the active episode wasn't deleted
						Cursor c = _dbAdapter.getReadableDatabase().query("podcasts", new String[]{"_id"}, "_id = ?", new String[]{String.valueOf(activeId)}, null, null, null);
						if (c.moveToFirst())
							sqlBuilder.appendWhere("podcasts._id = " + activeId);
						else {
							prefs.edit().remove(PREF_ACTIVE).apply();
							sqlBuilder.appendWhere("podcasts._id = " + getFirstDownloadedId(getContext()));
						}
						c.close();
					} else {
						sqlBuilder.appendWhere("podcasts._id = " + getFirstDownloadedId(getContext()));
					}
				}
				break;
			case EPISODES_SEARCH:
				sqlBuilder.setTables(sqlBuilder.getTables() + " JOIN fts_podcasts on podcasts._id = fts_podcasts._id");
				selection = "fts_podcasts MATCH ?";
				if (sortOrder == null)
					sortOrder = "pubDate DESC";
				break;
			case EPISODES_EXPIRED:
				String inWhere = "SELECT podcasts._id FROM podcasts " +
						"JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id " +
						"WHERE expirationDays IS NOT NULL AND queuePosition IS NOT NULL AND " +
						"date(pubDate, 'unixepoch', expirationDays || ' days') <= date('now')";
				sqlBuilder.appendWhere("podcasts._id IN (" + inWhere + ")");
				break;
			case EPISODES_NEED_GPODDER_UPDATE:
				sqlBuilder.appendWhere("podcasts.needsGpodderUpdate != 0");
				break;
			case EPISODES_LATEST_ACTIVITY:
				sqlBuilder.appendWhere("subscriptions.singleUse = 0");
				sortOrder = "pubDate DESC";
				limit = "10";
				break;
			case EPISODES_FINISHED:
				sortOrder = "finishedTime DESC";
				sqlBuilder.appendWhere("finishedTime IS NOT NULL");
				break;
			default:
				throw new IllegalArgumentException("Unknown URI");
		}

		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
		if (c != null && getContext() != null)
			c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	private static long getFirstDownloadedId(Context context) {
		String[] projection = {
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_FILE_SIZE,
				EpisodeProvider.COLUMN_MEDIA_URL,
		};
		Cursor c = context.getContentResolver().query(PLAYLIST_URI, projection, null, null, null);
		if (c == null)
			return -1;

		long episodeId = -1;
		while (c.moveToNext()) {
			EpisodeCursor episode = new EpisodeCursor(c);
			if (episode.isDownloaded(context)) {
				episodeId = episode.getId();
				break;
			}
		}
		c.close();
		return episodeId;
	}

	private String getNeedsDownloadIds() {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		SQLiteQueryBuilder playlistBuilder = new SQLiteQueryBuilder();
		playlistBuilder.setTables("podcasts");
		playlistBuilder.appendWhere("queuePosition IS NOT NULL");
		Cursor playlist = playlistBuilder.query(db,
				new String[]{"_id, mediaUrl, fileSize"}, null, null, null,
				null, "queuePosition");
		String playlistIds = "";

		if (playlist != null) {
			while (playlist.moveToNext()) {
				EpisodeCursor episode = new EpisodeCursor(playlist);
				if (!episode.isDownloaded(getContext()))
					playlistIds = playlistIds + playlist.getLong(0) + ",";
			}
			playlist.close();
		}

		if (playlistIds.length() > 0)
			playlistIds = playlistIds.substring(0, playlistIds.length() - 1);
		return playlistIds;
	}

	public static long getActiveEpisodeId(Context context) {
		long epId = context.getSharedPreferences("internals", Context.MODE_PRIVATE).getLong(PREF_ACTIVE, -1);
		if (epId != -1)
			return epId;
		return getFirstDownloadedId(context);
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
		if (getContext() == null)
			return 0;

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		if (db == null)
			return 0;

		long episodeId;
		SharedPreferences prefs = getContext().getSharedPreferences("internals", Context.MODE_PRIVATE);
		Long activeEpisodeId = prefs.getLong(PREF_ACTIVE, -1);

		int uriMatch = uriMatcher.match(uri);
		if (uriMatch == EPISODES) {
			int count = db.update("podcasts", values, where, whereArgs);
			// only main uri is notified
			getContext().getContentResolver().notifyChange(URI, null);

			// tell every listener that every podcast changed
			Cursor c = db.query("podcasts", null, where, whereArgs, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					Episodes.notifyChange(new EpisodeCursor(c));
				c.close();
			}

			return count;
		}

		// tell gpodder the new position
		if (values.containsKey(COLUMN_LAST_POSITION)) {
			values.put(COLUMN_NEEDS_GPODDER_UPDATE, Constants.GPODDER_UPDATE_POSITION);
			values.put(COLUMN_GPODDER_UPDATE_TIMESTAMP, new Date().getTime());
		}

		// process the player update separately
		if (uriMatch == EPISODE_PLAYER_UPDATE) {
			if (activeEpisodeId == -1)
				return 0;

			// saved the watched time to the stats
			Cursor lastPositionCursor = db.rawQuery("SELECT " + COLUMN_LAST_POSITION + " FROM podcasts WHERE _id = ?", new String[] { String.valueOf(activeEpisodeId) });
			if (!lastPositionCursor.moveToFirst())
				return 0;
			Stats.addListenTime(getContext(), (values.getAsInteger(COLUMN_LAST_POSITION) - lastPositionCursor.getInt(0)) / 1000.0f);
			lastPositionCursor.close();

			db.update("podcasts", values, "_id = ?", new String[]{String.valueOf(activeEpisodeId)});
			getContext().getContentResolver().notifyChange(ACTIVE_EPISODE_URI, null);
			notifyActiveChange();
			getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(URI, activeEpisodeId), null);
			ActiveEpisodeReceiver.notifyExternal(getContext());

			notifyChange(activeEpisodeId);

			return 1;
		}

		switch (uriMatch) {
			case EPISODE_ID:
				episodeId = ContentUris.parseId(uri);
				break;
			case EPISODE_ACTIVE:
				if (values.containsKey(COLUMN_ID)) {
					activeEpisodeId = values.getAsLong(COLUMN_ID);
					Editor editor = prefs.edit();
					if (activeEpisodeId != null) {
						editor.putLong(PREF_ACTIVE, activeEpisodeId);
						notifyActiveChange();
					} else {
						editor.remove(PREF_ACTIVE);
						notifyActiveChange();
					}
					editor.apply();

					// if we're clearing the active podcast or updating just the ID, don't go to the DB
					if (activeEpisodeId == null || values.size() == 1) {
						getContext().getContentResolver().notifyChange(ACTIVE_EPISODE_URI, null);
						return 0;
					}
				}

				// if we don't have an active podcast, don't update it
				if (activeEpisodeId == -1)
					return 0;

				episodeId = activeEpisodeId;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI");
		}

		String extraWhere = COLUMN_ID + " = " + episodeId;
		if (where != null)
			where = extraWhere + " AND " + where;
		else
			where = extraWhere;

		// don't try to update subscription values
		values.remove(COLUMN_SUBSCRIPTION_TITLE);
		values.remove(COLUMN_SUBSCRIPTION_URL);

		// update queuePosition separately
		if (values.containsKey(COLUMN_PLAYLIST_POSITION)) {
			// get the new position
			Integer newPosition = values.getAsInteger(COLUMN_PLAYLIST_POSITION);
			values.remove(COLUMN_PLAYLIST_POSITION);

			// no way to get changed record count until
			// SQLiteStatement.executeUpdateDelete in API level 11
			updatePlaylistPosition(episodeId, newPosition);

			// if this was the active episode and it's no longer in the playlist or it was moved to the back
			// don't restart on this episode
			if (activeEpisodeId == episodeId && (newPosition == null || newPosition == Integer.MAX_VALUE)) {
				prefs.edit().remove(PREF_ACTIVE).apply();
				activeEpisodeId = episodeId; // make sure the active episode notification is sent
			}

			// if there is no episode podcast, the active episode may have changed
			if (activeEpisodeId == -1)
				activeEpisodeId = episodeId;
		}

		int count = 0;
		if (values.size() > 0)
			count += db.update("podcasts", values, where, whereArgs);
		getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(URI, episodeId), null);
		notifyChange(episodeId);
		if (values.containsKey(COLUMN_FILE_SIZE))
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(URI, "to_download"), null);
		if (episodeId == activeEpisodeId) {
			getContext().getContentResolver().notifyChange(ACTIVE_EPISODE_URI, null);
			notifyActiveChange();
			ActiveEpisodeReceiver.notifyExternal(getContext());
		}

		if (values.containsKey(COLUMN_FINISHED_TIME))
			Episodes.notifyFinishedChange();


		// if the current episode has updated the position but it's not from the player, tell the player to update
		if (episodeId == activeEpisodeId && values.containsKey(COLUMN_LAST_POSITION))
			getContext().getContentResolver().notifyChange(PLAYER_UPDATE_URI, null);

		// update the full text search virtual table
		if (hasFTSValues(values))
			db.update("fts_podcasts", extractFTSValues(values), where, whereArgs);

		return count;
	}

	private void notifyChange(long episodeId) {
		EpisodeCursor episodeCursor = EpisodeCursor.getCursor(getContext(), episodeId);
		if (episodeCursor != null) {
			Episodes.notifyChange(episodeCursor);
			episodeCursor.closeCursor();
		}
	}

	private void notifyActiveChange() {
		PlayerStatus.notify(getContext());
	}

	private boolean hasFTSValues(ContentValues values) {
		return values.containsKey(COLUMN_TITLE) || values.containsKey(COLUMN_DESCRIPTION);
	}

	private ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(2);
		if (values.containsKey(COLUMN_TITLE))
			ftsValues.put(COLUMN_TITLE, values.getAsString(COLUMN_TITLE));
		if (values.containsKey(COLUMN_DESCRIPTION))
			ftsValues.put(COLUMN_DESCRIPTION, values.getAsString(COLUMN_DESCRIPTION));
		return ftsValues;
	}

	void updatePlaylistPosition(long episodeId, Integer newPosition) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// get the old position
		Cursor c = db.query("podcasts", new String[]{"queuePosition"},
				"_id = ?", new String[]{String.valueOf(episodeId)}, null, null, null);
		c.moveToFirst();
		Integer oldPosition = null;
		if (!c.isNull(0))
			oldPosition = c.getInt(0);
		c.close();

		// no need to remove from playlist if it's not in playlist
		if (oldPosition == null && newPosition == null)
			return;

		if (oldPosition == null) { // newPosition != null
			// new at 3: 1 2 3 4 5 do: 3++ 4++ 5++
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition + 1 "
					+ "WHERE queuePosition >= ?", new Object[]{newPosition});

			// download the newly added episode
			EpisodeDownloadService.downloadEpisodesSilently(getContext());
		} else if (newPosition == null) { // oldPosition != null
			// remove 3: 1 2 3 4 5 do: 4-- 5--
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition - 1 "
					+ "WHERE queuePosition > ?", new Object[]{oldPosition});

			// delete the episode's file
			deleteFiles(getContext(), episodeId);
		} else if (!oldPosition.equals(newPosition)) {
			// moving up: 1 2 3 4 5 2 -> 4: 3-- 4-- 2->4
			if (oldPosition < newPosition)
				db.execSQL(
						"UPDATE podcasts SET queuePosition = queuePosition - 1 "
								+ "WHERE queuePosition > ? AND queuePosition <= ?",
						new Object[]{oldPosition, newPosition});
			// moving down: 1 2 3 4 5 4 -> 2: 2++ 3++ 4->2
			if (newPosition < oldPosition)
				db.execSQL(
						"UPDATE podcasts SET queuePosition = queuePosition + 1 "
								+ "WHERE queuePosition >= ? AND queuePosition < ?",
						new Object[]{newPosition, oldPosition});
		}


		// if new position is max_value, put the episode at the end
		if (newPosition != null && newPosition == Integer.MAX_VALUE) {
			Cursor max = db.rawQuery(
					"SELECT COALESCE(MAX(queuePosition) + 1, 0) FROM podcasts",
					null);
			max.moveToFirst();
			newPosition = max.getInt(0);
			max.close();
		}

		// update specified episode
		db.execSQL("UPDATE podcasts SET queuePosition = ? WHERE _id = ?",
			new Object[]{newPosition, episodeId});
		if (getContext() != null) {
			getContext().getContentResolver().notifyChange(PLAYLIST_URI, null);
			Episodes.notifyPlaylistChange();
		}
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		if (!(uriMatcher.match(uri) == EPISODES))
			throw new IllegalArgumentException("Illegal URI for insert");
		if (values.get(COLUMN_MEDIA_URL) == null)
			throw new IllegalArgumentException("mediaUrl is required field for episode");

		Cursor mediaUrlCursor = db.rawQuery(
				"SELECT _id FROM podcasts WHERE mediaUrl = ?",
				new String[]{values.getAsString(COLUMN_MEDIA_URL)});
		Long episodeId = null;
		if (mediaUrlCursor.moveToNext())
			episodeId = mediaUrlCursor.getLong(0);
		mediaUrlCursor.close();

		if (episodeId != null) {
			if (values.containsKey(COLUMN_MEDIA_URL) && values.containsKey(COLUMN_FILE_SIZE)) {
				String file = Storage.getStoragePath(getContext()) +
						String.valueOf(episodeId) + "." +
						EpisodeCursor.getExtension(values.getAsString(COLUMN_MEDIA_URL));
				// TODO: don't change filesize if file is downloaded
				if (new File(file).length() > values.getAsInteger(COLUMN_FILE_SIZE))
					values.remove(COLUMN_FILE_SIZE);
			}

			db.update("podcasts", values, COLUMN_ID + " = ?", new String[]{String.valueOf(episodeId)});
			// insert into the full text search virtual table
			if (hasFTSValues(values))
				db.update("fts_podcasts", extractFTSValues(values), COLUMN_ID + " = ?", new String[]{String.valueOf(episodeId)});
		} else {
			episodeId = db.insert("podcasts", null, values);

			ContentValues ftsValues = extractFTSValues(values);
			ftsValues.put(COLUMN_ID, episodeId);
			db.insert("fts_podcasts", null, ftsValues);

			// if the new episode is less than 5 days old for the right subscriptions, add it to the playlist
			SubscriptionCursor sub = SubscriptionCursor.getCursor(getContext(), values.getAsLong(COLUMN_SUBSCRIPTION_ID));
			if (sub != null) {
				if (sub.areNewEpisodesAddedToPlaylist()
						&& !sub.isSingleUse()
						&& values.containsKey(COLUMN_PUB_DATE)) {
					Calendar c = Calendar.getInstance();
					c.add(Calendar.DATE, -5);
					if (new Date(values.getAsLong(COLUMN_PUB_DATE) * 1000L).after(c.getTime())) {
						updatePlaylistPosition(episodeId, Integer.MAX_VALUE);
					}
				}
				sub.closeCursor();
			}
		}

		if (getContext() != null)
			getContext().getContentResolver().notifyChange(uri, null);
		return EpisodeProvider.getContentUri(episodeId);
	}

	@Override
	public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
		switch (uriMatcher.match(uri)) {
			case EPISODES:
				break;
			case EPISODE_ID:
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
		String[] columns = new String[]{COLUMN_ID};

		// act on the podcasts about to be deleted
		Cursor c = db.query("podcasts", columns, where, whereArgs, null, null, null);
		while (c.moveToNext()) {
			deleteFiles(getContext(), c.getLong(0));
			db.delete("fts_podcasts", "_id = ?", new String[] { Long.toString(c.getLong(0)) });
		}
		c.close();

		// keep the queue order monotonic
		String episodesWhere = "queuePosition IS NOT NULL";
		if (where != null)
			episodesWhere = where + " AND " + episodesWhere;
		c = db.query("podcasts", columns, episodesWhere, whereArgs, null, null, null);
		while (c.moveToNext())
			updatePlaylistPosition(c.getLong(0), null);
		c.close();

		if (getContext() != null) {
			if (!uri.equals(URI))
				getContext().getContentResolver().notifyChange(URI, null);
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return db.delete("podcasts", where, whereArgs);
	}

	private static void deleteFiles(Context context, final long episodeId) {
		File storage = new File(EpisodeCursor.getPodcastStoragePath(context));
		File[] files = storage.listFiles(pathname -> {
			return pathname.getName().startsWith(String.valueOf(episodeId) + ".");
		});
		for (File f : files)
			f.delete();
		new File(EpisodeCursor.getIndexFilename(context, episodeId)).delete();
	}

	public static void restart(Context context, long episodeId) {
		restart(context, EpisodeProvider.getContentUri(episodeId));
	}

	public static void restart(Context context, Uri uri) {
		ContentValues values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_LAST_POSITION, 0);
		context.getContentResolver().update(uri, values, null, null);
	}

	public static void movePositionTo(Context context, long episodeId, int position) {
		movePositionTo(context, EpisodeProvider.getContentUri(episodeId), position);
	}

	public static void movePositionTo(Context context, Uri uri, int position) {
		ContentValues values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_LAST_POSITION, position);
		context.getContentResolver().update(uri, values, null, null);
	}

	public static void movePositionBy(Context context, long episodeId, int delta) {
		movePositionBy(context, EpisodeProvider.getContentUri(episodeId), delta);
	}

	public static void movePositionBy(Context context, Uri uri, int delta) {
		String[] projection = new String[]{EpisodeProvider.COLUMN_LAST_POSITION, EpisodeProvider.COLUMN_DURATION};
		Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
		if (c == null)
			return;
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

	public static void skipToEnd(Context context, long episodeId) {
		skipToEnd(context, EpisodeProvider.getContentUri(episodeId));
	}

	public static void skipToEnd(Context context, Uri uri) {
		PlaylistManager.markEpisodeComplete(context, uri);
	}
}
