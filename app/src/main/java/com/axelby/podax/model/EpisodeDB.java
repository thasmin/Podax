package com.axelby.podax.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.axelby.podax.ActiveEpisodeReceiver;
import com.axelby.podax.Constants;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PlaylistManager;
import com.axelby.podax.Stats;
import com.axelby.podax.Storage;

import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDate;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class EpisodeDB {
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SUBSCRIPTION_ID = "subscriptionId";
	public static final String COLUMN_SUBSCRIPTION_TITLE = "subscriptionTitle";
	public static final String COLUMN_SUBSCRIPTION_URL = "subscriptionUrl";
	public static final String COLUMN_PLAYLIST_POSITION = "queuePosition";
	public static final String COLUMN_MEDIA_URL = "mediaUrl";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUB_DATE = "pubDate";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_FILE_SIZE = "fileSize";
	public static final String COLUMN_LAST_POSITION = "lastPosition";
	public static final String COLUMN_DURATION = "duration";
	public static final String COLUMN_NEEDS_GPODDER_UPDATE = "needsGpodderUpdate";
	public static final String COLUMN_GPODDER_UPDATE_TIMESTAMP = "gpodderUpdateTimestamp";
	public static final String COLUMN_PAYMENT = "payment";
	public static final String COLUMN_FINISHED_TIME = "finishedTime";

	private static Context _context;

	public static void setContext(@NonNull Context context) {
		_context = context;
	}

	public static void restart(long episodeId) {
		new EpisodeEditor(episodeId).setLastPosition(0).commit();
	}

	public static void movePositionTo(long episodeId, int position) {
		new EpisodeEditor(episodeId).setLastPosition(position).commit();
	}

	public static void movePositionBy(long episodeId, int delta) {
		EpisodeData ep = EpisodeData.create(episodeId);
		if (ep == null)
			return;
		int position = ep.getLastPosition();
		int duration = ep.getDuration();

		int newPosition = position + delta * 1000;
		if (newPosition < 0)
			newPosition = 0;
		if (duration != 0 && newPosition > duration)
			newPosition = duration;

		movePositionTo(episodeId, newPosition);
	}

	public static void skipToEnd(long episodeId) {
		PlaylistManager.markEpisodeComplete(episodeId);
	}

	/* ----------
	   operations
	   ---------- */

	private final DBAdapter _dbAdapter;

	EpisodeDB(DBAdapter dbAdapter) {
		_dbAdapter = dbAdapter;
	}

	void update(long episodeId, ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// tell gpodder the new position
		if (values.containsKey(COLUMN_LAST_POSITION)) {
			values.put(COLUMN_NEEDS_GPODDER_UPDATE, Constants.GPODDER_UPDATE_POSITION);
			values.put(COLUMN_GPODDER_UPDATE_TIMESTAMP, new Date().getTime());
		}

		// don't try to update subscription values
		values.remove(COLUMN_SUBSCRIPTION_TITLE);
		values.remove(COLUMN_SUBSCRIPTION_URL);

		boolean isChangingPlaylistPosition = values.containsKey(COLUMN_PLAYLIST_POSITION);
		Integer playlistPosition = values.getAsInteger(COLUMN_PLAYLIST_POSITION);
		boolean isRemovingFromPlaylist = isChangingPlaylistPosition && playlistPosition != null;

		// shift the playlist if this is moving to the middle
		if (isRemovingFromPlaylist && playlistPosition != Integer.MAX_VALUE)
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition + 1 WHERE queuePosition IS NOT NULL AND queuePosition >= ?",
				new String[]{String.valueOf(playlistPosition)}
			);

		// if this was the active episode and it's no longer in the playlist, don't restart on this episode
		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeEpisodeId = prefs.getLong("active", -1);
		if (activeEpisodeId == episodeId && isRemovingFromPlaylist) {
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
			PlayerStatus.update(_context);
			ActiveEpisodeReceiver.notifyExternal(_context);
		}
		// playlist notification
		if (isChangingPlaylistPosition)
			notifyPlaylistChange();
		// finished notification
		if (values.containsKey(COLUMN_FINISHED_TIME))
			notifyFinishedChange();
		// regular notification
		notifyChange(EpisodeData.create(episodeId));
	}

	public void setActiveEpisode(long episodeId) {
		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		prefs.edit().putLong("active", episodeId).apply();

		PlayerStatus.update(_context);
		ActiveEpisodeReceiver.notifyExternal(_context);
		notifyChange(EpisodeData.create(episodeId));
	}

	public void updatePlayerPosition(int position) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeEpisodeId = prefs.getLong("active", -1);
		if (activeEpisodeId == -1)
			return;

		// saved the watched time to the stats
		Cursor lastPositionCursor = db.rawQuery(
			"SELECT " + COLUMN_LAST_POSITION + " FROM podcasts WHERE _id = ?",
			new String[] { String.valueOf(activeEpisodeId) }
		);
		if (lastPositionCursor == null || !lastPositionCursor.moveToFirst())
			return;
		Stats.addListenTime(_context, (position - lastPositionCursor.getInt(0)) / 1000.0f);
		lastPositionCursor.close();

		ContentValues values = new ContentValues();
		values.put(COLUMN_LAST_POSITION, position);
		db.update("podcasts", values, "_id = ?", new String[] { String.valueOf(activeEpisodeId) });

		// celebrate!
		PlayerStatus.updateFromPlayer(_context);
		ActiveEpisodeReceiver.notifyExternal(_context);
		notifyChange(EpisodeData.create(activeEpisodeId));
	}

	public long insert(ContentValues values) {
		SQLiteDatabase db = _dbAdapter.getWritableDatabase();

		// don't duplicate media url
		String url = values.getAsString(COLUMN_MEDIA_URL);
		List<EpisodeData> existing = getList(COLUMN_MEDIA_URL + " = ?", new String[] { url });
		if (existing.size() > 0)
			return existing.get(0).getId();

		// if the new episode is less than 5 days old for the right subscriptions, add it to the playlist
		SubscriptionData sub = SubscriptionData.create(values.getAsLong(COLUMN_SUBSCRIPTION_ID));
		if (sub != null) {
			if (sub.areNewEpisodesAddedToPlaylist()
					&& !sub.isSingleUse()
					&& values.containsKey(COLUMN_PUB_DATE)) {
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, -5);
				if (new Date(values.getAsLong(COLUMN_PUB_DATE) * 1000L).after(c.getTime())) {
					values.put(COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
				}
			}
		}

		long id = db.insert("podcasts", null, values);

		ContentValues ftsValues = extractFTSValues(values);
		ftsValues.put(COLUMN_ID, id);
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
		File storage = new File(Storage.getPodcastStoragePath(_context));
		File[] files = storage.listFiles(pathname -> {
			return pathname.getName().startsWith(String.valueOf(episodeId) + ".");
		});
		for (File f : files)
			f.delete();
	}

	public long getActiveEpisodeId() {
		// first check shared pref, then db
		final String PREF_ACTIVE = "active";
		SharedPreferences prefs = _context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		long activeId = prefs.getLong(PREF_ACTIVE, -1);
		if (activeId != -1)
			return activeId;

		List<EpisodeData> eps = PodaxDB.episodes.getDownloaded();
		if (eps.size() == 0)
			return -1;
		return eps.get(0).getId();
	}

	public EpisodeData getActive() {
		long activeEpisodeId = getActiveEpisodeId();
		if (activeEpisodeId == -1)
			return null;
		return EpisodeData.create(activeEpisodeId);
	}

	public static Observable<EpisodeData> getObservable(long episodeId) {
		return EpisodeDB.getEpisodeWatcher(episodeId)
			.subscribeOn(Schedulers.io())
			.startWith(EpisodeData.create(episodeId))
			.observeOn(AndroidSchedulers.mainThread());
	}

	public List<EpisodeData> getAll() {
		return getList(null, null);
	}

	public EpisodeData get(long episodeId) {
		String selection = "_id = ?";
		String[] selectionArgs = new String[] {String.valueOf(episodeId)};
		return getSingle(selection, selectionArgs);
	}

	public List<EpisodeData> getFor(@NonNull String field, @NonNull int value) {
		String selection = field + " = ?";
		String[] selectionArgs = new String[] { String.valueOf(value) };
		return getList(selection, selectionArgs);
	}

	public List<EpisodeData> getFor(@NonNull String field, @NonNull String value) {
		String selection = field + " = ?";
		String[] selectionArgs = new String[] { value };
		return getList(selection, selectionArgs, null);
	}

	public EpisodeData getForMediaUrl(String url) {
		return getSingle("mediaUrl = ?", new String[] { url });
	}

	@Nullable
	private EpisodeData getSingle(String selection, String[] selectionArgs) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor c = db.query("podcasts_view", null, selection, selectionArgs, null, null, null);
		if (c == null || !c.moveToNext())
			return null;
		EpisodeData ep = EpisodeData.from(c);
		c.close();
		return ep;
	}

	@NonNull
	private List<EpisodeData> getList(String selection, String[] selectionArgs) {
		return getList(selection, selectionArgs, null, null);
	}

	@NonNull
	private List<EpisodeData> getList(String selection, String[] selectionArgs, String orderBy) {
		return getList(selection, selectionArgs, orderBy, null);
	}

	@NonNull
	private List<EpisodeData> getList(String selection, String[] selectionArgs, String orderBy, Integer limit) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor cursor;
		if (limit != null)
			cursor = db.query("podcasts_view", null, selection, selectionArgs, null, null, orderBy, String.valueOf(limit));
		else
			cursor = db.query("podcasts_view", null, selection, selectionArgs, null, null, orderBy);
		List<EpisodeData> eps = getList(cursor);
		cursor.close();
		return eps;
	}

	@NonNull
	private List<EpisodeData> getList(Cursor cursor) {
		ArrayList<EpisodeData> eps = new ArrayList<>(cursor.getCount());
		while (cursor.moveToNext())
			eps.add(EpisodeData.from(cursor));

		cursor.close();

		return eps;
	}

	private static <T> List<T> filter(Collection<T> list, Func1<T, Boolean> predicate) {
		ArrayList<T> results = new ArrayList<>();
		for (T t : list)
			if (predicate.call(t))
				results.add(t);
		return results;
	}

	public List<EpisodeData> getDownloaded() {
		List<EpisodeData> potential = getList(COLUMN_FILE_SIZE + " > 0", null, "queuePosition");
		return filter(potential, ep -> ep.isDownloaded(_context));
	}

	public List<EpisodeData> getNeedsDownload() {
		List<EpisodeData> potential = getList("queuePosition IS NOT NULL", null, "queuePosition");
		return filter(potential, ep -> !ep.isDownloaded(_context));
	}

	public List<EpisodeData> getForSubscriptionId(long subscriptionId) {
		return getList("subscriptionId = ?", new String[]{ String.valueOf(subscriptionId) }, "pubDate DESC");
	}

	// not attached to a subject because not attached to a timer
	public List<EpisodeData> getExpired() {
		String inWhere = "SELECT podcasts._id FROM podcasts " +
				"JOIN subscriptions ON podcasts.subscriptionId = subscriptions._id " +
				"WHERE expirationDays IS NOT NULL AND queuePosition IS NOT NULL AND " +
				"date(pubDate, 'unixepoch', expirationDays || ' days') <= date('now')";
		String selection = "podcasts_view._id IN (" + inWhere + ")";
		return getList(selection, null);
	}

	public List<EpisodeData> getLatestActivity() {
		String sql = "SELECT p.* FROM podcasts_view p JOIN subscriptions s ON p.subscriptionId = s._id " +
			"WHERE s.singleUse = 0";
		return getList(_dbAdapter.getReadableDatabase().rawQuery(sql, null));
	}

	public List<EpisodeData> search(String query) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		// TODO: use an in clause to avoid getting duplicate table names
		String tables = ("podcasts_view JOIN fts_podcasts on podcasts._id = fts_podcasts._id");
		String selection = "fts_podcasts MATCH ?";
		String orderBy = "pubDate DESC";
		Cursor c = db.query(tables, null, selection, new String[] { query }, null, null, orderBy);
		return getList(c);
	}

	public boolean isLastActivityAfter(long when) {
		SQLiteDatabase db = _dbAdapter.getReadableDatabase();
		Cursor c = db.query("podcasts", null, COLUMN_PUB_DATE + ">?",
				new String[] { String.valueOf(when) }, null, null, null);
		if (c == null)
			return true;
		boolean isAfter = c.getCount() > 0;
		c.close();
		return isAfter;
	}

	private BehaviorSubject<List<EpisodeData>> _finishedSubject = BehaviorSubject.create();
	private void notifyFinishedChange() {
		_finishedSubject.onNext(getList("finishedTime IS NOT NULL", null, "finishedTime DESC"));
	}
	public List<EpisodeData> getFinished() {
		return getList("finishedTime IS NOT NULL", null, "finishedTime DESC");
	}
	public Observable<List<EpisodeData>> watchFinished() {
		if (!_finishedSubject.hasValue())
			notifyFinishedChange();
		return _finishedSubject;
	}

	private BehaviorSubject<List<EpisodeData>> _playlistSubject = BehaviorSubject.create();
	private void notifyPlaylistChange() {
		_playlistSubject.onNext(getList("queuePosition IS NOT NULL", null, "queuePosition"));
	}
	public List<EpisodeData> getPlaylist() {
		return getList("queuePosition IS NOT NULL", null, "queuePosition");
	}
	public Observable<List<EpisodeData>> watchPlaylist() {
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

	public List<EpisodeData> getNewForSubscriptionIds(List<Long> subIds) {
		if (subIds.size() == 0)
			return new ArrayList<>();

		String selection = COLUMN_SUBSCRIPTION_ID + " IN (" + TextUtils.join(",", subIds) + ")" +
			" AND " + COLUMN_PUB_DATE + " > " + (LocalDate.now().minusDays(7).toDate().getTime() / 1000);
		return getList(selection, null, null);
	}

	public void evictCache() {
		_changeSubject = PublishSubject.create();
		_finishedSubject = BehaviorSubject.create();
		_playlistSubject = BehaviorSubject.create();
		EpisodeData.evictCache();
	}

	private static boolean hasFTSValues(ContentValues values) {
		return values.containsKey(COLUMN_TITLE) || values.containsKey(COLUMN_DESCRIPTION);
	}

	private static ContentValues extractFTSValues(ContentValues values) {
		ContentValues ftsValues = new ContentValues(2);
		ftsValues.put(COLUMN_TITLE, values.getAsString(COLUMN_TITLE));
		ftsValues.put(COLUMN_DESCRIPTION, values.getAsString(COLUMN_DESCRIPTION));
		return ftsValues;
	}
}
