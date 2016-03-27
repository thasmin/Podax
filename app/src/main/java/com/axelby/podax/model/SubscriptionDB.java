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

	@NonNull
	private List<SubscriptionData> getList(String selection, String[] selectionArgs) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor cursor = db.query("subscriptions", null, selection, selectionArgs, null, null, null);

		int idIndex = cursor.getColumnIndex("_id");

		ArrayList<SubscriptionData> subs = new ArrayList<>(cursor.getCount());
		while (cursor.moveToNext()) {
			SubscriptionData.evictFromCache(cursor.getLong(idIndex));
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
