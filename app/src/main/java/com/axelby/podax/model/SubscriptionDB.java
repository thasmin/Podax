package com.axelby.podax.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class SubscriptionDB {

	private DBAdapter _dbAdapter;

	public SubscriptionDB(DBAdapter dbAdapter) {
		_dbAdapter = dbAdapter;
	}

	public long insert(ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate url
		String url = values.getAsString(Subscriptions.COLUMN_URL);
		Subscriptions.getFor(Subscriptions.COLUMN_URL, url);
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
		ftsValues.put(Subscriptions.COLUMN_ID, id);
		db.insert("fts_subscriptions", null, ftsValues);

		return id;
	}

	public void update(long subscriptionId, ContentValues values) {
		// url is not allowed to be changed - make a new subscription
		if (values.containsKey(Subscriptions.COLUMN_URL))
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
				data = SubscriptionData.from(cursor);
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
		while (cursor.moveToNext()) {
			long id = cursor.getLong(cursor.getColumnIndex("_id"));
			subs.add(get(id));
		}
		cursor.close();
		return subs;
	}

	@NonNull
	private List<SubscriptionData> getList(String selection, String[] selectionArgs) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		String sortOrder = "subscriptions.title IS NULL, COALESCE(subscriptions.titleOverride, subscriptions.title)";
		Cursor cursor = db.query("subscriptions", null, selection, selectionArgs, null, null, sortOrder);

		ArrayList<SubscriptionData> subs = new ArrayList<>(cursor.getCount());
		while (cursor.moveToNext())
			subs.add(SubscriptionData.from(cursor));

		cursor.close();

		return subs;
	}

	private boolean hasFTSValues(ContentValues values) {
		return values.containsKey(Subscriptions.COLUMN_TITLE)
			|| values.containsKey(Subscriptions.COLUMN_TITLE_OVERRIDE)
			|| values.containsKey(Subscriptions.COLUMN_DESCRIPTION);
	}

	private static ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(3);
		if (values.containsKey(Subscriptions.COLUMN_TITLE))
			ftsValues.put(Subscriptions.COLUMN_TITLE, values.getAsString(Subscriptions.COLUMN_TITLE));
		if (values.containsKey(Subscriptions.COLUMN_TITLE_OVERRIDE))
			ftsValues.put(Subscriptions.COLUMN_TITLE_OVERRIDE, values.getAsString(Subscriptions.COLUMN_TITLE_OVERRIDE));
		if (values.containsKey(Subscriptions.COLUMN_DESCRIPTION))
			ftsValues.put(Subscriptions.COLUMN_DESCRIPTION, values.getAsString(Subscriptions.COLUMN_DESCRIPTION));
		return ftsValues;
	}
}
