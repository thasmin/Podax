package com.axelby.podax.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;

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

	SubscriptionDB(DBAdapter dbAdapter) {
		_dbAdapter = dbAdapter;
	}

	/* -------
	   watcher
	   ------- */

	// subscription id is old id, if null means inserted
	// data is new data, if null means deleted
	public class SubscriptionChange {
		private final Long _subscriptionId;
		private final SubscriptionData _newData;

		public SubscriptionChange(Long subscriptionId, SubscriptionData newData) {
			_subscriptionId = subscriptionId;
			_newData = newData;
		}

		public Long getId() { return _subscriptionId; }
		public SubscriptionData getNewData() { return _newData; }
	}

	private PublishSubject<SubscriptionChange> _changeSubject = PublishSubject.create();
	public void notifyChange(Long subscriptionId, SubscriptionData sub) {
		_changeSubject.onNext(new SubscriptionChange(subscriptionId, sub));
	}

	public Observable<SubscriptionChange> watchAll() {
		return _changeSubject;
	}

	public Observable<SubscriptionData> watch(long id) {
		if (id < 0)
			return Observable.empty();

		return _changeSubject
			.filter(d -> d.getId() == id)
			.map(SubscriptionChange::getNewData)
			.startWith(SubscriptionData.create(id));
	}

	public void evictCache() {
		SubscriptionData.evictCache();
	}

	/* -------------
	   db operations
	   ------------- */

	public long insert(ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate url
		String url = values.getAsString(COLUMN_URL);
		List<SubscriptionData> existing = getFor(COLUMN_URL, url);
		if (existing.size() > 0)
			return existing.get(0).getId();

		// insert subscription
		long id = db.insert("subscriptions", null, values);
		values.put(COLUMN_ID, id);

		// insert into full text search
		ContentValues ftsValues = extractFTSValues(values);
		ftsValues.put(COLUMN_ID, id);
		db.insert("fts_subscriptions", null, ftsValues);

		notifyChange(null, get(id));

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

		notifyChange(subscriptionId, get(subscriptionId));
	}

	public void delete(long subscriptionId) {
		SubscriptionData sub = SubscriptionData.create(subscriptionId);
		if (sub != null)
			PodaxDB.gPodder.remove(sub.getUrl());
		doDelete(subscriptionId);
	}

	public void deleteViaGPodder(String url) {
		SubscriptionData sub = getForRSSUrl(url);
		if (sub != null)
			doDelete(sub.getId());
	}

	private void doDelete(long subscriptionId) {
		Observable.from(PodaxDB.episodes.getForSubscriptionId(subscriptionId))
			.subscribe(
				s -> PodaxDB.episodes.delete(s.getId()),
				e -> Log.e("Subscriptions", "unable to retrieve episodes to delete", e)
			);

		SubscriptionData.evictThumbnails(subscriptionId);

		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		db.delete("subscriptions", "_id = ?", new String[] { String.valueOf(subscriptionId) });
		db.delete("fts_subscriptions", "_id = ?", new String[] { String.valueOf(subscriptionId) });

		notifyChange(subscriptionId, null);
	}

	/* -------
	   getters
	   ------- */

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

	public SubscriptionData getForRSSUrl(String rssUrl) {
		List<SubscriptionData> subs = PodaxDB.subscriptions.getFor(COLUMN_URL, rssUrl);
		if (subs.size() == 0)
			return null;
		return subs.get(0);
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

	private static boolean hasFTSValues(ContentValues values) {
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
