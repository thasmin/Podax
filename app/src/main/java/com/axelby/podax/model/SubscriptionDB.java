package com.axelby.podax.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.UpdateService;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionDB {
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

	private DBAdapter _dbAdapter;

	public SubscriptionDB(DBAdapter dbAdapter) {
		_dbAdapter = dbAdapter;
	}

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

	public long insert(ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate url
		String url = values.getAsString(COLUMN_URL);
		Subscriptions.getFor(COLUMN_URL, url);
		Cursor c = db.rawQuery("SELECT _id FROM subscriptions WHERE url = ?", new String[] { url });
		if (c.moveToNext()) {
			long oldId = c.getLong(0);
			c.close();
			return oldId;
		}
		c.close();

		// insert subscription
		long id = db.insert("subscriptions", null, values);

		// insert into full text search
		ContentValues ftsValues = extractFTSValues(values);
		ftsValues.put(COLUMN_ID, id);
		db.insert("fts_subscriptions", null, ftsValues);

		return id;
	}

	public void update(long subscriptionId, ContentValues values) {
		// url is not allowed to be changed - make a new subscription
		if (values.containsKey(COLUMN_URL))
			return;

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		db.update("subscriptions", values, "_id = ?", new String[] { String.valueOf(subscriptionId) });

		// update the full text search virtual table
		if (hasFTSValues(values))
			db.update("fts_subscriptions", extractFTSValues(values), "_id = ?", new String[] { String.valueOf(subscriptionId) });
	}

	public void delete(long subscriptionId) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		db.delete("subscriptions", "_id = ?", new String[] { String.valueOf(subscriptionId) });
		db.delete("fts_subscriptions", "_id = ?", new String[] { String.valueOf(subscriptionId) });
	}

	public SubscriptionData get(long subscriptionId) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor cursor = db.query("subscriptions", null, "_id = ?", new String[] { String.valueOf(subscriptionId) }, null, null, null);
		SubscriptionData data = null;
		if (cursor != null) {
			if (cursor.moveToNext()) {
				SubscriptionData.evictFromCache(subscriptionId);
				data = SubscriptionData.from(new SubscriptionCursor(cursor));
			}
			cursor.close();
		}
		return data;
	}

	public List<SubscriptionData> getAll() {
		return getList(null, null);
	}

	public List<SubscriptionData> getFor(String field, int value) {
		return getFor(field, String.valueOf(value));
	}

	public List<SubscriptionData> getFor(String field, String value) {
		String selection = field + " = ?";
		String[] selectionArgs = new String[] { value };
		return getList(selection, selectionArgs);
	}

	public List<SubscriptionData> search(String query) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		String table = "subscriptions s JOIN fts_subscriptions fts on s._id = fts._id";
		String selection = "singleUse = 0 AND fts_subscriptions MATCH ?";
		String sortOrder = "s.title IS NULL, COALESCE(s.titleOverride, s.title)";
		String sql = "SELECT s.* FROM " + table + " WHERE " + selection + " ORDER BY " + sortOrder;
		Cursor cursor = db.rawQuery(sql, new String[] { query });

		ArrayList<SubscriptionData> subs = new ArrayList<>(cursor.getCount());
		while (cursor.moveToNext())
			subs.add(get(new SubscriptionCursor(cursor).getId()));
		cursor.close();
		return subs;
	}

	@NonNull
	private List<SubscriptionData> getList(String selection, String[] selectionArgs) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		String sortOrder = "subscriptions.title IS NULL, COALESCE(subscriptions.titleOverride, subscriptions.title)";
		Cursor cursor = db.query("subscriptions", null, selection, selectionArgs, null, null, sortOrder);

		//int idIndex = cursor.getColumnIndex("_id");

		ArrayList<SubscriptionData> subs = new ArrayList<>(cursor.getCount());
		while (cursor.moveToNext()) {
			//SubscriptionData.evictFromCache(cursor.getLong(idIndex));
			subs.add(SubscriptionData.from(new SubscriptionCursor(cursor)));
		}

		cursor.close();

		return subs;
	}

	private boolean hasFTSValues(ContentValues values) {
		return values.containsKey(COLUMN_TITLE)
			|| values.containsKey(COLUMN_TITLE_OVERRIDE)
			|| values.containsKey(COLUMN_DESCRIPTION);
	}

	private static ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(3);
		if (values.containsKey(COLUMN_TITLE))
			ftsValues.put(COLUMN_TITLE, values.getAsString(COLUMN_TITLE));
		if (values.containsKey(COLUMN_TITLE_OVERRIDE))
			ftsValues.put(COLUMN_TITLE_OVERRIDE, values.getAsString(COLUMN_TITLE_OVERRIDE));
		if (values.containsKey(COLUMN_DESCRIPTION))
			ftsValues.put(COLUMN_DESCRIPTION, values.getAsString(COLUMN_DESCRIPTION));
		return ftsValues;
	}
}
