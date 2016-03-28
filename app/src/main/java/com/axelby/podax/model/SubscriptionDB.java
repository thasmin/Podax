package com.axelby.podax.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionDB {
	private DBAdapter _dbAdapter;

	public SubscriptionDB(DBAdapter dbAdapter) {
		_dbAdapter = dbAdapter;
	}

	public long insert(ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate url
		String url = values.getAsString(SubscriptionProvider.COLUMN_URL);
		Subscriptions.getFor(SubscriptionProvider.COLUMN_URL, url);
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
		ftsValues.put(SubscriptionProvider.COLUMN_ID, id);
		db.insert("fts_subscriptions", null, ftsValues);

		return id;
	}

	public void update(long subscriptionId, ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		db.update("subscriptions", values, "_id = ?", new String[] { String.valueOf(subscriptionId) });
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
			if (cursor.moveToNext())
				data = SubscriptionData.from(new SubscriptionCursor(cursor));
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
		String fieldName = SubscriptionProvider.getColumnMap().get(field);
		String selection = fieldName + " = ?";
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
			subs.add(SubscriptionData.from(new SubscriptionCursor(cursor)));
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

	private static ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(3);
		if (values.containsKey(SubscriptionProvider.COLUMN_TITLE))
			ftsValues.put(SubscriptionProvider.COLUMN_TITLE, values.getAsString(SubscriptionProvider.COLUMN_TITLE));
		if (values.containsKey(SubscriptionProvider.COLUMN_TITLE_OVERRIDE))
			ftsValues.put(SubscriptionProvider.COLUMN_TITLE_OVERRIDE, values.getAsString(SubscriptionProvider.COLUMN_TITLE_OVERRIDE));
		if (values.containsKey(SubscriptionProvider.COLUMN_DESCRIPTION))
			ftsValues.put(SubscriptionProvider.COLUMN_DESCRIPTION, values.getAsString(SubscriptionProvider.COLUMN_DESCRIPTION));
		return ftsValues;
	}
}
