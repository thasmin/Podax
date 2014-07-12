package com.axelby.podax;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SubscriptionProvider extends ContentProvider {
	public static String AUTHORITY = "com.axelby.podax.subscriptionprovider";
	public static Uri URI = Uri.parse("content://" + AUTHORITY + "/subscriptions");
	public static final Uri SEARCH_URI = Uri.withAppendedPath(URI, "search");
	public static final Uri FROM_GPODDER_URI = Uri.withAppendedPath(URI, "from_gpodder");

	public static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.axelby.subscription";
	public static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.axelby.subscription";
	public static final String PODCAST_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.axelby.podcast";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_URL = "url";
	public static final String COLUMN_LAST_MODIFIED = "lastModified";
	public static final String COLUMN_LAST_UPDATE = "lastUpdate";
	public static final String COLUMN_ETAG = "eTag";
	public static final String COLUMN_THUMBNAIL = "thumbnail";
	public static final String COLUMN_TITLE_OVERRIDE = "titleOverride";
	public static final String COLUMN_PLAYLIST_NEW = "queueNew";
	public static final String COLUMN_EXPIRATION = "expirationDays";
    public static final String COLUMN_DESCRIPTION = "description";

	private static final int SUBSCRIPTIONS = 1;
	private static final int SUBSCRIPTION_ID = 2;
	private static final int PODCASTS = 3;
	private static final int SUBSCRIPTIONS_SEARCH = 4;
	private static final int FROM_GPODDER = 5;

	static UriMatcher _uriMatcher;
	static HashMap<String, String> _columnMap;

	static {
		_uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		_uriMatcher.addURI(AUTHORITY, "subscriptions", SUBSCRIPTIONS);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/#", SUBSCRIPTION_ID);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/#/podcasts", PODCASTS);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/search", SUBSCRIPTIONS_SEARCH);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/from_gpodder", FROM_GPODDER);

		_columnMap = new HashMap<String, String>();
		_columnMap.put(COLUMN_ID, "_id");
		_columnMap.put(COLUMN_TITLE, "title");
		_columnMap.put(COLUMN_URL, "url");
		_columnMap.put(COLUMN_LAST_MODIFIED, "lastModified");
		_columnMap.put(COLUMN_LAST_UPDATE, "lastUpdate");
		_columnMap.put(COLUMN_ETAG, "eTag");
		_columnMap.put(COLUMN_THUMBNAIL, "thumbnail");
		_columnMap.put(COLUMN_TITLE_OVERRIDE, "titleOverride");
		_columnMap.put(COLUMN_PLAYLIST_NEW, "queueNew");
		_columnMap.put(COLUMN_EXPIRATION, "expirationDays");
        _columnMap.put(COLUMN_DESCRIPTION, "description");
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
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		int uriMatch = _uriMatcher.match(uri);

		if (uriMatch == PODCASTS) {
			return getContext().getContentResolver().query(EpisodeProvider.URI,
					projection, "subscriptionId = ?",
					new String[]{uri.getPathSegments().get(1)},
					EpisodeProvider.COLUMN_PUB_DATE + " DESC");
		}

		// make sure that title_override is in the query set if title is in there
		if (projection != null) {
			boolean hasTitle = false, hasOverride = false;
			for (String p : projection) {
				if (p.equals(COLUMN_TITLE))
					hasTitle = true;
				if (p.equals(COLUMN_TITLE_OVERRIDE))
					hasOverride = true;
			}
			if (hasTitle && !hasOverride) {
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(projection));
				list.add(COLUMN_TITLE_OVERRIDE);
				projection = list.toArray(new String[list.size()]);
			}
		}

		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setProjectionMap(_columnMap);
		sqlBuilder.setTables("subscriptions");

		switch (uriMatch) {
			case SUBSCRIPTIONS:
				if (sortOrder == null)
					sortOrder = "title IS NULL, COALESCE(titleOverride, title)";
				break;
			case SUBSCRIPTION_ID:
				sqlBuilder.appendWhere("_id = " + uri.getLastPathSegment());
				break;
			case SUBSCRIPTIONS_SEARCH:
				sqlBuilder.appendWhere("LOWER(title) LIKE ? OR LOWER(titleOverride) LIKE ?");
				if (!selectionArgs[0].startsWith("%"))
					selectionArgs[0] = "%" + selectionArgs[0] + "%";
				selectionArgs = new String[]{selectionArgs[0], selectionArgs[0]};
				if (sortOrder == null)
					sortOrder = "COALESCE(titleOverride, title)";
				break;
			default:
				throw new IllegalArgumentException("Unknown URI");
		}

		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		// the url is not allowed to be changed -- insert a new one instead
		if (values.containsKey(COLUMN_URL))
			return 0;

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

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		int count = db.update("subscriptions", values, where, whereArgs);
		if (!URI.equals(uri))
			getContext().getContentResolver().notifyChange(uri, null);
		getContext().getContentResolver().notifyChange(URI, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// url is required
		if (!values.containsKey(COLUMN_URL))
			return null;

		boolean from_gpodder = false;

		switch (_uriMatcher.match(uri)) {
			case SUBSCRIPTIONS:
				break;
			case FROM_GPODDER:
				from_gpodder = true;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI");
		}

		String url = values.getAsString(COLUMN_URL);
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate a url
		Cursor c = db.rawQuery("SELECT _id FROM subscriptions WHERE url = ?", new String[]{url});
		if (c.moveToNext()) {
			long oldId = c.getLong(0);
			c.close();
			return ContentUris.withAppendedId(URI, oldId);
		}
		c.close();

		long id = db.insert("subscriptions", null, values);

		// add during next gpodder sync
		if (!from_gpodder)
			getContext().getContentResolver().insert(GPodderProvider.URI, GPodderProvider.makeValuesToAdd(url));

		getContext().getContentResolver().notifyChange(URI, null);
		return ContentUris.withAppendedId(URI, id);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		ContentResolver contentResolver = getContext().getContentResolver();

		boolean from_gpodder = false;

		switch (_uriMatcher.match(uri)) {
			case SUBSCRIPTIONS:
				break;
			case FROM_GPODDER:
				from_gpodder = true;
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

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// go through subscriptions about to be deleted and remove podcasts
		Cursor c = db.query("subscriptions", new String[]{COLUMN_ID}, where, whereArgs, null, null, null);
		ArrayList<String> subIds = new ArrayList<String>();
		String in = "";
		while (c.moveToNext()) {
			in += ",?";
			subIds.add(String.valueOf(c.getInt(0)));
		}
		c.close();
		if (!in.equals("")) {
			in = "(" + in.substring(1) + ")";
			contentResolver.delete(EpisodeProvider.URI, "subscriptionId IN " + in, subIds.toArray(new String[subIds.size()]));
		}

		// remove during next gpodder sync
		if (!from_gpodder) {
			c = db.query("subscriptions", new String[]{COLUMN_URL}, where, whereArgs, null, null, null);
			while (c.moveToNext()) {
				String url = c.getString(0);
				getContext().getContentResolver().insert(GPodderProvider.URI, GPodderProvider.makeValuesToRemove(url));
			}
			c.close();
		}

		// delete subscription
		int count = db.delete("subscriptions", where, whereArgs);

		contentResolver.notifyChange(URI, null);
		return count;
	}

}
