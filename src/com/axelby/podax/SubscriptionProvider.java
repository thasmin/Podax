package com.axelby.podax;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SubscriptionProvider extends ContentProvider {
	public static String AUTHORITY = "com.axelby.podax.subscriptionprovider";
	public static Uri URI = Uri.parse("content://" + AUTHORITY + "/subscriptions");
	public static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/vnd.axelby.subscription";
	public static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/vnd.axelby.subscription";
	public static final String PODCAST_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/vnd.axelby.podcast";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_LAST_MODIFIED = "lastModified";
	public static final String COLUMN_LAST_UPDATE = "lastUpdate";
	public static final String COLUMN_ETAG = "eTag";
	public static final String COLUMN_THUMBNAIL = "thumbnail";

	static final int SUBSCRIPTIONS = 1;
	static final int SUBSCRIPTION_ID = 2;
	static final int PODCASTS = 3;

	static UriMatcher _uriMatcher;
	static HashMap<String, String> _columnMap;

	static {
		_uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		_uriMatcher.addURI(AUTHORITY, "subscriptions", SUBSCRIPTIONS);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/#", SUBSCRIPTION_ID);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/#/podcasts", PODCASTS);

		_columnMap = new HashMap<String, String>();
		_columnMap.put(COLUMN_ID, "_id");
		_columnMap.put(COLUMN_TITLE, "title");
		_columnMap.put(COLUMN_URL, "url");
		_columnMap.put(COLUMN_LAST_MODIFIED, "lastModified");
		_columnMap.put(COLUMN_LAST_UPDATE, "id");
		_columnMap.put(COLUMN_ETAG, "eTag");
		_columnMap.put(COLUMN_THUMBNAIL, "thumbnail");
	}

	DBAdapter _dbAdapter;

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (_uriMatcher.match(uri)) {
		case SUBSCRIPTIONS:
			return DIR_TYPE;
		case SUBSCRIPTION_ID:
			return ITEM_TYPE;
		case PODCASTS:
			return PODCAST_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int uriMatch = _uriMatcher.match(uri);

		if (uriMatch == PODCASTS) {
			return getContext().getContentResolver().query(PodcastProvider.URI,
					projection, "subscriptionId = ?",
					new String[] { uri.getPathSegments().get(1) },
					PodcastProvider.COLUMN_PUB_DATE + " DESC");
		}

		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setProjectionMap(_columnMap);
		sqlBuilder.setTables("subscriptions");

		switch (uriMatch) {
		case SUBSCRIPTIONS:
			if (sortOrder == null)
				sortOrder = "title IS NULL, title";
			break;
		case SUBSCRIPTION_ID:
			sqlBuilder.appendWhere("_id = " + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		Cursor c = sqlBuilder.query(_dbAdapter.getRawDB(), projection,
				selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		switch (_uriMatcher.match(uri)) {
		case SUBSCRIPTION_ID:
			String extraWhere = COLUMN_ID + " = " + uri.getLastPathSegment();
			if (where != null)
				where = extraWhere + " AND " + where;
			else
				where = extraWhere;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		int count = 0;
		count += _dbAdapter.getRawDB().update("subscriptions", values, where,
				whereArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		getContext().getContentResolver().notifyChange(URI, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (values.size() == 0)
			return null;
		switch (_uriMatcher.match(uri)) {
		case SUBSCRIPTIONS:
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		long id = _dbAdapter.getRawDB().insert("subscriptions", null, values);
		getContext().getContentResolver().notifyChange(URI, null);
		return ContentUris.withAppendedId(URI, id);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		switch (_uriMatcher.match(uri)) {
		case SUBSCRIPTIONS:
			break;
		case SUBSCRIPTION_ID:
			String extraWhere = COLUMN_ID + " = " + uri.getLastPathSegment();
			if (where != null)
				where = extraWhere + " AND " + where;
			else
				where = extraWhere;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		getContext().getContentResolver().notifyChange(URI, null);
		return _dbAdapter.getRawDB().delete("subscriptions", where, whereArgs);
	}

}
