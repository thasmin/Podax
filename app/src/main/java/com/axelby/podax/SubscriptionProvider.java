package com.axelby.podax;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.database.DatabaseUtilsCompat;

import com.axelby.podax.model.DBAdapter;
import com.axelby.podax.model.SubscriptionEditor;
import com.axelby.podax.model.Subscriptions;

import java.util.HashMap;

public class SubscriptionProvider extends ContentProvider {
	private static final String AUTHORITY = "com.axelby.podax.subscriptionprovider";
	public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/subscriptions");

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
	public static final String COLUMN_SINGLE_USE = "singleUse";

	private static final int SUBSCRIPTIONS = 1;
	private static final int SUBSCRIPTION_ID = 2;
	private static final int PODCASTS = 3;
	private static final int SUBSCRIPTIONS_SEARCH = 4;
	private static final int FROM_GPODDER = 5;

	private static final UriMatcher _uriMatcher;

	public static HashMap<String, String> getColumnMap() {
		return _columnMap;
	}

	private static final HashMap<String, String> _columnMap;

	static {
		_uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		_uriMatcher.addURI(AUTHORITY, "subscriptions", SUBSCRIPTIONS);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/#", SUBSCRIPTION_ID);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/#/podcasts", PODCASTS);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/search", SUBSCRIPTIONS_SEARCH);
		_uriMatcher.addURI(AUTHORITY, "subscriptions/from_gpodder", FROM_GPODDER);

		_columnMap = new HashMap<>();
		_columnMap.put(COLUMN_ID, "subscriptions._id");
		_columnMap.put(COLUMN_TITLE, "subscriptions.title");
		_columnMap.put(COLUMN_URL, "subscriptions.url");
		_columnMap.put(COLUMN_LAST_MODIFIED, "lastModified");
		_columnMap.put(COLUMN_LAST_UPDATE, "lastUpdate");
		_columnMap.put(COLUMN_ETAG, "eTag");
		_columnMap.put(COLUMN_THUMBNAIL, "thumbnail");
		_columnMap.put(COLUMN_TITLE_OVERRIDE, "subscriptions.titleOverride");
		_columnMap.put(COLUMN_PLAYLIST_NEW, "queueNew");
		_columnMap.put(COLUMN_EXPIRATION, "expirationDays");
		_columnMap.put(COLUMN_DESCRIPTION, "subscriptions.description");
		_columnMap.put(COLUMN_SINGLE_USE, "singleUse");
	}

	public static Uri getContentUri(long id) {
		return ContentUris.withAppendedId(URI, id);
	}

	private DBAdapter _dbAdapter;

    public static long addNewSubscription(Context context, String url) {
		long id = SubscriptionEditor.create(url).setSingleUse(false).commit();
        UpdateService.updateSubscription(context, id);
		return id;
    }

    public static long addSingleUseSubscription(Context context, String url) {
		long id = SubscriptionEditor.create(url)
			.setSingleUse(true)
			.setPlaylistNew(false)
			.commit();
		UpdateService.updateSubscription(context, id);
		return id;
    }

    @Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(@NonNull Uri uri) {
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
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
		// the url is not allowed to be changed -- insert a new one instead
		if (values.containsKey(COLUMN_URL))
			return 0;

		switch (_uriMatcher.match(uri)) {
			case SUBSCRIPTIONS:
				break;
			case SUBSCRIPTION_ID:
				where = DatabaseUtilsCompat.concatenateWhere(where, COLUMN_ID + " = " + uri.getLastPathSegment());
				break;
			default:
				throw new IllegalArgumentException("Unknown URI");
		}

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		int count = db.update("subscriptions", values, where, whereArgs);
		if (getContext() != null) {
			if (!URI.equals(uri))
				getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(URI, null);

			// tell every listener every subscription that changed
			Cursor c = db.query("subscriptions", null, where, whereArgs, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					Subscriptions.notifyChange(new SubscriptionCursor(c));
				c.close();
			}
		}

		// update the full text search virtual table
		if (hasFTSValues(values))
			db.update("fts_subscriptions", extractFTSValues(values), where, whereArgs);

		return count;
	}

	private boolean hasFTSValues(ContentValues values) {
		return values.containsKey(COLUMN_TITLE) || values.containsKey(COLUMN_TITLE_OVERRIDE) || values.containsKey(COLUMN_DESCRIPTION);
	}

	private ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(3);
		if (values.containsKey(COLUMN_TITLE))
			ftsValues.put(COLUMN_TITLE, values.getAsString(COLUMN_TITLE));
		if (values.containsKey(COLUMN_TITLE_OVERRIDE))
			ftsValues.put(COLUMN_TITLE_OVERRIDE, values.getAsString(COLUMN_TITLE_OVERRIDE));
		if (values.containsKey(COLUMN_DESCRIPTION))
			ftsValues.put(COLUMN_DESCRIPTION, values.getAsString(COLUMN_DESCRIPTION));
		return ftsValues;
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
		return 0;
	}

}
