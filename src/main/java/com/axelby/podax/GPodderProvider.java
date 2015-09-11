package com.axelby.podax;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import javax.annotation.Nonnull;

public class GPodderProvider extends ContentProvider {
	public static final String AUTHORITY = "com.axelby.podax.gpodder_sync";
	public static final Uri URI = Uri.parse("content://" + AUTHORITY);
	private static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.axelby.podax.gpoddersync";

	private DBAdapter _dbAdapter;

	public static final Uri TO_REMOVE_URI = Uri.withAppendedPath(URI, "to_remove");
	public static final Uri TO_ADD_URI = Uri.withAppendedPath(URI, "to_add");

	private static final int ALL = 0;
	private static final int TO_ADD = 1;
	private static final int TO_REMOVE = 2;
	private static final int DEVICE = 3;

	private static final UriMatcher _uriMatcher;

	static {
		_uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		_uriMatcher.addURI(AUTHORITY, "", ALL);
		_uriMatcher.addURI(AUTHORITY, "to_add", TO_ADD);
		_uriMatcher.addURI(AUTHORITY, "to_remove", TO_REMOVE);
		_uriMatcher.addURI(AUTHORITY, "device", DEVICE);
	}

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(this.getContext());
		return true;
	}

	@Override
	public String getType(@Nonnull Uri uri) {
		return DIR_TYPE;
	}

	@Override
	public Cursor query(@Nonnull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();

		if (selection == null)
			selection = "1=1";
		switch (_uriMatcher.match(uri)) {
			case ALL:
				return db.query("gpodder_sync", projection, selection, selectionArgs, null, null, sortOrder);
			case TO_ADD:
				return db.query("gpodder_sync", projection, selection + " AND to_add = 1", selectionArgs, null, null, sortOrder);
			case TO_REMOVE:
				return db.query("gpodder_sync", projection, selection + " AND to_remove = 1", selectionArgs, null, null, sortOrder);
			case DEVICE:
				return db.query("gpodder_device", projection, selection, selectionArgs, null, null, sortOrder);
			default:
				return null;
		}
	}

	// not a valid operation
	@Override
	public int update(@Nonnull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	public static ContentValues makeValuesToAdd(String url) {
		ContentValues values = new ContentValues(2);
		values.put("url", url);
		values.put("to_add", 1);
		return values;
	}

	public static ContentValues makeValuesToRemove(String url) {
		ContentValues values = new ContentValues(2);
		values.put("url", url);
		values.put("to_remove", 1);
		return values;
	}

	@Override
	public Uri insert(@Nonnull Uri uri, ContentValues values) {
		if (!uri.equals(URI) || !values.containsKey("url"))
			return null;
		if (!values.containsKey("to_add") && !values.containsKey("to_remove"))
			return null;

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		long id = db.insert("gpodder_sync", null, values);
		return ContentUris.withAppendedId(URI, id);
	}

	@Override
	public int delete(@Nonnull Uri uri, String selection, String[] selectionArgs) {
		if (!uri.equals(URI))
			return 0;

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		return db.delete("gpodder_sync", selection, selectionArgs);
	}
}
