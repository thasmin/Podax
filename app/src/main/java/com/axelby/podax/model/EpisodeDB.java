package com.axelby.podax.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.axelby.podax.ActiveEpisodeReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.Stats;

import org.joda.time.LocalDate;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class EpisodeDB {
	private static Context _context;

	public static void setContext(@NonNull Context context) {
		_context = context;
	}

	private final DBAdapter _dbAdapter;

	EpisodeDB(DBAdapter dbAdapter) {
		_dbAdapter = dbAdapter;
	}

	public void update(long episodeId, ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// tell gpodder the new position
		if (values.containsKey(EpisodeProvider.COLUMN_LAST_POSITION)) {
			values.put(EpisodeProvider.COLUMN_NEEDS_GPODDER_UPDATE, Constants.GPODDER_UPDATE_POSITION);
			values.put(EpisodeProvider.COLUMN_GPODDER_UPDATE_TIMESTAMP, new Date().getTime());
		}

		// don't try to update subscription values
		values.remove(EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE);
		values.remove(EpisodeProvider.COLUMN_SUBSCRIPTION_URL);

		Integer playlistPosition = values.getAsInteger(EpisodeProvider.COLUMN_PLAYLIST_POSITION);
		// shift the playlist if this is moving to the middle
		if (values.containsKey(EpisodeProvider.COLUMN_PLAYLIST_POSITION) && playlistPosition != null && playlistPosition != Integer.MAX_VALUE)
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition + 1 WHERE queuePosition IS NOT NULL AND queuePosition >= ?",
				new String[]{String.valueOf(playlistPosition)}
			);

		// if this was the active episode and it's no longer in the playlist, don't restart on this episode
		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeEpisodeId = prefs.getLong("active", -1);
		if (activeEpisodeId == episodeId && playlistPosition == null) {
			prefs.edit().remove("active").apply();
			activeEpisodeId = -1;
		}

		// do updates
		db.update("podcasts", values, "_id = ?", new String[] { String.valueOf(episodeId) });
		if (hasFTSValues(values))
			db.update("fts_podcasts", extractFTSValues(values), "_id = ?", new String[] { String.valueOf(episodeId) });
		ensureMonotonicQueue(db);

		EpisodeData.evictFromCache(episodeId);

		// active episode notification
		if (activeEpisodeId == episodeId) {
			PlayerStatus.notify(_context);
			ActiveEpisodeReceiver.notifyExternal(_context);
		}
		// playlist notification
		if (values.containsKey(EpisodeProvider.COLUMN_PLAYLIST_POSITION))
			EpisodeDB.notifyPlaylistChange();
		// finished notification
		if (values.containsKey(EpisodeProvider.COLUMN_FINISHED_TIME))
			EpisodeDB.notifyFinishedChange();
		// regular notification
		notifyChange(EpisodeData.create(episodeId));
	}

	public void resetGPodderUpdates() {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();
		ContentValues values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_NEEDS_GPODDER_UPDATE, Constants.GPODDER_UPDATE_NONE);
		db.update("podcasts", values, null, null);
	}

	public void setActiveEpisode(long episodeId) {
		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		prefs.edit().putLong("active", episodeId).apply();

		PlayerStatus.notify(_context);
		ActiveEpisodeReceiver.notifyExternal(_context);
		notifyChange(EpisodeData.create(episodeId));
	}

	public void updateActiveEpisode(ContentValues values) {
		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeEpisodeId = prefs.getLong("active", -1);
		if (activeEpisodeId == -1)
			return;
		update(activeEpisodeId, values);
	}

	public void updateActiveEpisodePosition(int position) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeEpisodeId = prefs.getLong("active", -1);
		if (activeEpisodeId == -1)
			return;

		// saved the watched time to the stats
		Cursor lastPositionCursor = db.rawQuery(
			"SELECT " + EpisodeProvider.COLUMN_LAST_POSITION + " FROM podcasts WHERE _id = ?",
			new String[] { String.valueOf(activeEpisodeId) }
		);
		if (lastPositionCursor == null || !lastPositionCursor.moveToFirst())
			return;
		Stats.addListenTime(_context, (position - lastPositionCursor.getInt(0)) / 1000.0f);
		lastPositionCursor.close();

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_LAST_POSITION, position);
		db.update("podcasts", values, "_id = ?", new String[] { String.valueOf(activeEpisodeId) });

		// celebrate!
		PlayerStatus.notify(_context);
		ActiveEpisodeReceiver.notifyExternal(_context);
		notifyChange(EpisodeData.create(activeEpisodeId));
	}

	public long insert(ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate media url
		String url = values.getAsString(EpisodeProvider.COLUMN_MEDIA_URL);
		List<EpisodeData> existing = getList(EpisodeProvider.COLUMN_MEDIA_URL + " = ?", new String[] { url });
		if (existing.size() > 0)
			return existing.get(0).getId();

		// if the new episode is less than 5 days old for the right subscriptions, add it to the playlist
		SubscriptionData sub = SubscriptionData.create(values.getAsLong(EpisodeProvider.COLUMN_SUBSCRIPTION_ID));
		if (sub != null) {
			if (sub.areNewEpisodesAddedToPlaylist()
					&& !sub.isSingleUse()
					&& values.containsKey(EpisodeProvider.COLUMN_PUB_DATE)) {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, -5);
				if (new Date(values.getAsLong(EpisodeProvider.COLUMN_PUB_DATE) * 1000L).after(c.getTime())) {
					values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
				}
			}
		}

		long id = db.insert("podcasts", null, values);

		ContentValues ftsValues = extractFTSValues(values);
		ftsValues.put(EpisodeProvider.COLUMN_ID, id);
		db.insert("fts_podcasts", null, ftsValues);

		ensureMonotonicQueue(db);

		return id;
	}

	public void delete(long episodeId) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// act on the podcasts about to be deleted
		db.delete("podcasts", "_id = ?", new String[] { String.valueOf(episodeId) });
		db.delete("fts_podcasts", "_id = ?", new String[] { String.valueOf(episodeId) });
		deleteFiles(episodeId);

		ensureMonotonicQueue(db);

		// TODO: let everyone know
	}

	private void ensureMonotonicQueue(SQLiteDatabase db) {
		// keep the queue order monotonic
		String monotonicSQL = "UPDATE podcasts SET queuePosition = " +
			"(SELECT COUNT(queuePosition) FROM podcasts sub WHERE queuePosition IS NOT NULL AND sub.queuePosition < podcasts.queuePosition) " +
			"WHERE queuePosition IS NOT NULL";
		db.execSQL(monotonicSQL);
	}

	private static void deleteFiles(long episodeId) {
		File storage = new File(EpisodeCursor.getPodcastStoragePath(_context));
		File[] files = storage.listFiles(pathname -> {
			return pathname.getName().startsWith(String.valueOf(episodeId) + ".");
		});
		for (File f : files)
			f.delete();
	}

	private static Observable<EpisodeData> queryToObservable(Uri uri, String selection, String[] selectionArgs, String sortOrder) {
		return Observable.create(subscriber -> {
			Cursor cursor = _context.getContentResolver().query(uri, null, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				while (cursor.moveToNext())
					subscriber.onNext(EpisodeData.from(new EpisodeCursor(cursor)));
				cursor.close();
			}
			subscriber.onCompleted();
		});
	}

	private static Observable<List<EpisodeData>> queryToListObservable(Uri uri, String selection, String[] selectionArgs, String sortOrder) {
		return Observable.create(subscriber -> {
			Cursor cursor = _context.getContentResolver().query(uri, null, selection, selectionArgs, sortOrder);
			if (cursor == null) {
				subscriber.onCompleted();
				return;
			}

			ArrayList<EpisodeData> list = new ArrayList<>(cursor.getCount());
			while (cursor.moveToNext())
				list.add(EpisodeData.from(new EpisodeCursor(cursor)));
			cursor.close();

			subscriber.onNext(list);
			subscriber.onCompleted();
		});
	}

	public static Observable<EpisodeData> getObservable(long episodeId) {
		return EpisodeDB.getEpisodeWatcher(episodeId)
			.subscribeOn(Schedulers.io())
			.startWith(EpisodeData.create(episodeId))
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<List<EpisodeData>> getAll() {
		return queryToListObservable(EpisodeProvider.URI, null, null, null);
	}

	public EpisodeData get(long episodeId) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		String selection = "_id = ?";
		String[] selectionArgs = new String[] {String.valueOf(episodeId)};
		Cursor c = db.query("podcasts_view", null, selection, selectionArgs, null, null, null);
		if (c == null || !c.moveToNext())
			return null;
		EpisodeData ep = EpisodeData.from(new EpisodeCursor(c));
		c.close();
		return ep;
	}

	public static Observable<EpisodeData> getFor(String field, int value) {
		String fieldName = EpisodeProvider.getColumnMap().get(field);
		String selection = fieldName + " = ?";
		String[] selectionArgs = new String[] { String.valueOf(value) };
		return queryToObservable(EpisodeProvider.URI, selection, selectionArgs, null);
	}

	public static Observable<EpisodeData> getFor(String field, String value) {
		String fieldName = EpisodeProvider.getColumnMap().get(field);
		String selection = fieldName + " = ?";
		String[] selectionArgs = new String[] { value };
		return queryToObservable(EpisodeProvider.URI, selection, selectionArgs, null);
	}

	public EpisodeData getForMediaUrl(String url) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		String selection = "mediaUrl = ?";
		String[] selectionArgs = new String[] { url };
		Cursor c = db.query("podcasts_view", null, selection, selectionArgs, null, null, null);
		if (c == null || !c.moveToNext())
			return null;
		EpisodeData ep = EpisodeData.from(new EpisodeCursor(c));
		c.close();
		return ep;
	}

	@NonNull
	private List<EpisodeData> getList(String selection, String[] selectionArgs) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor cursor = db.query("podcasts_view", null, selection, selectionArgs, null, null, null);

		ArrayList<EpisodeData> eps = new ArrayList<>(cursor.getCount());
		while (cursor.moveToNext())
			eps.add(EpisodeData.from(new EpisodeCursor(cursor)));

		cursor.close();

		return eps;
	}

	public static Observable<EpisodeData> getDownloaded() {
		return queryToObservable(EpisodeProvider.URI, EpisodeProvider.COLUMN_FILE_SIZE + " > 0", null, null)
			.filter(ep -> ep.isDownloaded(_context));
	}

	public static Observable<EpisodeData> getNeedsDownload() {
		return queryToObservable(EpisodeProvider.PLAYLIST_URI, null, null, null)
			.filter(ep -> !ep.isDownloaded(_context));
	}

	public static Observable<List<EpisodeData>> getForSubscriptionId(long subscriptionId) {
		String selection = EpisodeProvider.COLUMN_SUBSCRIPTION_ID + "=?";
		String[] selectionArgs = { String.valueOf(subscriptionId) };
		return queryToListObservable(EpisodeProvider.URI, selection, selectionArgs, "pubDate DESC");
	}

	// not attached to a subject because not attached to a timer
	public static Observable<EpisodeData> getExpired() {
		return queryToObservable(EpisodeProvider.EXPIRED_URI, null, null, null);
	}

	public static Observable<EpisodeData> getLatestActivity() {
		return queryToObservable(EpisodeProvider.LATEST_ACTIVITY_URI, null, null, EpisodeProvider.COLUMN_PUB_DATE + " DESC");
	}

	public static Observable<List<EpisodeData>> search(String query) {
		return queryToListObservable(EpisodeProvider.SEARCH_URI, null, new String[] { query }, null);
	}

	public static boolean isLastActivityAfter(long when) {
		Cursor c = _context.getContentResolver().query(EpisodeProvider.LATEST_ACTIVITY_URI,
				null, EpisodeProvider.COLUMN_PUB_DATE + ">?",
				new String[] { String.valueOf(when) }, null);
		if (c == null)
			return true;
		boolean isAfter = c.getCount() > 0;
		c.close();
		return isAfter;
	}

	private static BehaviorSubject<List<EpisodeData>> _finishedSubject = BehaviorSubject.create();
	public static void notifyFinishedChange() {
		Cursor c = _context.getContentResolver().query(EpisodeProvider.FINISHED_URI, null, null, null, null);
		if (c == null)
			return;

		List<EpisodeData> finished = new ArrayList<>(c.getCount());
		while (c.moveToNext())
			finished.add(EpisodeData.from(new EpisodeCursor(c)));
		c.close();

		_finishedSubject.onNext(finished);
	}
	public static Observable<List<EpisodeData>> getFinished() {
		if (!_finishedSubject.hasValue())
			notifyFinishedChange();
		return _finishedSubject;
	}

	private static BehaviorSubject<List<EpisodeData>> _playlistSubject = BehaviorSubject.create();
	public static void notifyPlaylistChange() {
		Cursor c = _context.getContentResolver().query(EpisodeProvider.PLAYLIST_URI, null, null, null, null);
		if (c == null)
			return;

		List<EpisodeData> playlist = new ArrayList<>(c.getCount());
		while (c.moveToNext())
			playlist.add(EpisodeData.from(new EpisodeCursor(c)));
		c.close();

		_playlistSubject.onNext(playlist);
	}
	public static Observable<List<EpisodeData>> getPlaylist() {
		if (!_playlistSubject.hasValue())
			notifyPlaylistChange();
		return _playlistSubject;
	}

	private static PublishSubject<EpisodeData> _changeSubject = PublishSubject.create();
	public static void notifyChange(EpisodeData ep) {
		EpisodeData data = EpisodeData.cacheSwap(ep);
		_changeSubject.onNext(data);
	}

	public static Observable<EpisodeData> getEpisodeWatcher() {
		return _changeSubject.observeOn(AndroidSchedulers.mainThread());
	}
	public static Observable<EpisodeData> getEpisodeWatcher(long id) {
		return _changeSubject
			.filter(d -> d.getId() == id)
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<EpisodeData> getNewForSubscriptionIds(List<Long> subIds) {
		if (subIds.size() == 0)
			return Observable.empty();

		String selection = EpisodeProvider.COLUMN_SUBSCRIPTION_ID + " IN (" + TextUtils.join(",", subIds) + ")" +
			" AND " + EpisodeProvider.COLUMN_PUB_DATE + " > " + (LocalDate.now().minusDays(7).toDate().getTime() / 1000);
		return queryToObservable(EpisodeProvider.URI, selection, null, null);
	}

	public static void evictCache() {
		_changeSubject = PublishSubject.create();
		_finishedSubject = BehaviorSubject.create();
		_playlistSubject = BehaviorSubject.create();
		EpisodeData.evictCache();
	}

	private static boolean hasFTSValues(ContentValues values) {
		return values.containsKey(EpisodeProvider.COLUMN_TITLE) || values.containsKey(EpisodeProvider.COLUMN_DESCRIPTION);
	}

	private static ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(2);
		ftsValues.put(EpisodeProvider.COLUMN_TITLE, values.getAsString(EpisodeProvider.COLUMN_TITLE));
		ftsValues.put(EpisodeProvider.COLUMN_DESCRIPTION, values.getAsString(EpisodeProvider.COLUMN_DESCRIPTION));
		return ftsValues;
	}
}
