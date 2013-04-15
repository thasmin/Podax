package com.axelby.gpodder;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class Provider extends ContentProvider {
	public static String AUTHORITY = "com.axelby.gpodder.podcasts";
	public static Uri URI = Uri.parse("content://" + AUTHORITY);
	public static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.axelby.gpodder.podcast";
	public static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.axelby.gpodder.podcast";

	public static final String URL = "url";
	private DBAdapter _dbAdapter;

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(this.getContext());
		return true;
	}
	
	// if uri is the authority, all podcasts will be return
	// otherwise, only match will be returned
	@Override
	public String getType(Uri uri) {
		return uri.equals(URI) ? DIR_TYPE : ITEM_TYPE;
	}

	// if uri is the authority, all podcasts will be return
	// otherwise, only match will be returned
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();

		String columns = projection != null ? projection[0] : "ROWID as _id, url";
		Cursor c;
		if (uri.equals(URI))
			c = db.rawQuery("SELECT " + columns + " FROM " +
					"(SELECT url FROM subscriptions UNION select url from pending_add)" +
					"WHERE url NOT IN (SELECT url FROM pending_remove)", null);
		else
			c = db.query("subscriptions", new String[] { "url" }, "url = ?", new String[] { uri.getPath() }, null, null, null);
		return c;
	}

	// not a valid operation
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	// remove it from the pending_remove table
	// if it's not in the subscriptions table, add it to the pending_add table
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (!uri.equals(URI) || !values.containsKey(URL))
			return null;
		String url = values.getAsString(URL);
		Log.d("gpodder", "adding " + url);
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		db.delete("pending_remove", "url = ?", new String[] { url });
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM subscriptions WHERE url = ?", new String[] { url });
		c.moveToFirst();
		if (c.getLong(0) == 0)
			db.insert("pending_add", null, values);
		c.close();
		return Uri.withAppendedPath(URI, url);
	}

	// remove it from the pending_add table
	// if it's in the subscriptions table, add it to the pending_remove table
	// selection is ignored, selection args[0] is matched
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (!uri.equals(URI))
			return 0;

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		if (selectionArgs != null && selectionArgs.length > 0) {
			for (String url : selectionArgs) {
				Log.d("gpodder", "removing " + url);
				db.delete("pending_add", "url = ?", new String[] { url });
				db.delete("subscriptions", "url = ?", new String[] { url });
				db.insert("pending_remove", null, makeUrlValues(url));
			}
			return selectionArgs.length;
		} else {
			int count = db.delete("pending_add", "1", null);
			db.delete("pending_remove", null, null);
			Cursor c = db.query("subscriptions", new String[] { "url" }, null, null, null, null, null);
			while (c.moveToNext()) {
				++count;
				db.insert("pending_remove", null, makeUrlValues(c.getString(0)));
			}
			c.close();
			return count;
		}
	}

	private ContentValues makeUrlValues(String url) {
		ContentValues values = new ContentValues();
		values.put("url", url);
		return values;
	}

	public void fakeSync() {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		Cursor c = db.query("pending_remove", new String[] { "url" }, null, null, null, null, null);
		while (c.moveToNext())
			db.delete("subscriptions", "url = ?", new String[] { c.getString(0) });
		c.close();
		db.delete("pending_remove", null, null);

		c = db.query("pending_add", new String[] { "url" }, null, null, null, null, null);
		while (c.moveToNext())
			db.insert("subscriptions", null, makeUrlValues(c.getString(0)));
		c.close();
		db.delete("pending_add", null, null);
	}
}
