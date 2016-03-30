package com.axelby.podax;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.axelby.podax.model.DBAdapter;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;

import java.util.Arrays;
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
		return 0;
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
		return 0;
	}

	public static void restart(long episodeId) {
		new EpisodeEditor(episodeId).setLastPosition(0).commit();
	}

	public static void movePositionTo(long episodeId, int position) {
		new EpisodeEditor(episodeId).setLastPosition(position).commit();
	}

	public static void movePositionBy(long episodeId, int delta) {
		EpisodeData ep = EpisodeData.create(episodeId);
		if (ep == null)
			return;
		int position = ep.getLastPosition();
		int duration = ep.getDuration();

		int newPosition = position + delta * 1000;
		if (newPosition < 0)
			newPosition = 0;
		if (duration != 0 && newPosition > duration)
			newPosition = duration;

		movePositionTo(episodeId, newPosition);
	}

	public static void skipToEnd(long episodeId) {
		PlaylistManager.markEpisodeComplete(episodeId);
	}
}
