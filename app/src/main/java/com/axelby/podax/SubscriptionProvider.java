package com.axelby.podax;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

public class SubscriptionProvider extends ContentProvider {
	private static final String AUTHORITY = "com.axelby.podax.subscriptionprovider";
	public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/subscriptions");

	public static Uri getContentUri(long id) {
		return ContentUris.withAppendedId(URI, id);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		return null;
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
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

}
