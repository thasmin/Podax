package com.axelby.podax;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class Episodes {
	private static Observable<EpisodeData> queryToObservable(Context context,
			 Uri uri, String selection, String[] selectionArgs, String sortOrder) {
		return Observable.create(subscriber -> {
			Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				while (cursor.moveToNext())
					subscriber.onNext(EpisodeData.from(new EpisodeCursor(cursor)));
				cursor.close();
			}
			subscriber.onCompleted();
		});
	}

	private static Observable<List<EpisodeData>> queryToListObservable(Context context,
		   Uri uri, String selection, String[] selectionArgs, String sortOrder) {
		return Observable.create(subscriber -> {
			Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, sortOrder);
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

	public static Observable<EpisodeData> getObservable(Context context, long episodeId) {
		return Episodes.getEpisodeWatcher(episodeId)
			.startWith(EpisodeData.create(context, episodeId))
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<List<EpisodeData>> getForSubscriptionId(Context context, long subscriptionId) {
		String selection = EpisodeProvider.COLUMN_SUBSCRIPTION_ID + "=?";
		String[] selectionArgs = { String.valueOf(subscriptionId) };
		return queryToListObservable(context, EpisodeProvider.URI, selection, selectionArgs, "pubDate DESC");
	}

	// not attached to a subject because not attached to a timer
	public static Observable<EpisodeData> getExpired(Context context) {
		return queryToObservable(context, EpisodeProvider.EXPIRED_URI, null, null, null);
	}

	public static Observable<EpisodeData> getLatestActivity(Context context) {
		return queryToObservable(context, EpisodeProvider.LATEST_ACTIVITY_URI, null, null, EpisodeProvider.COLUMN_PUB_DATE + " DESC");
	}

	public static boolean isLastActivityAfter(Context context, long when) {
		Cursor c = context.getContentResolver().query(EpisodeProvider.LATEST_ACTIVITY_URI,
				null, EpisodeProvider.COLUMN_PUB_DATE + ">?",
				new String[] { String.valueOf(when) }, null);
		if (c == null)
			return true;
		boolean isAfter = c.getCount() > 0;
		c.close();
		return isAfter;
	}

	private static BehaviorSubject<List<EpisodeData>> _finishedSubject = BehaviorSubject.create();
	public static void notifyFinishedChange(Context context) {
		Cursor c = context.getContentResolver().query(EpisodeProvider.FINISHED_URI, null, null, null, null);
		if (c == null)
			return;

		List<EpisodeData> finished = new ArrayList<>(c.getCount());
		while (c.moveToNext())
			finished.add(EpisodeData.from(new EpisodeCursor(c)));
		c.close();

		_finishedSubject.onNext(finished);
	}
	public static Observable<List<EpisodeData>> getFinished(Context context) {
		if (!_finishedSubject.hasValue())
			notifyFinishedChange(context);
		return _finishedSubject;
	}

	private static BehaviorSubject<List<EpisodeData>> _playlistSubject = BehaviorSubject.create();
	public static void notifyPlaylistChange(Context context) {
		Cursor c = context.getContentResolver().query(EpisodeProvider.PLAYLIST_URI, null, null, null, null);
		if (c == null)
			return;

		List<EpisodeData> playlist = new ArrayList<>(c.getCount());
		while (c.moveToNext())
			playlist.add(EpisodeData.from(new EpisodeCursor(c)));
		c.close();

		_playlistSubject.onNext(playlist);
	}
	public static Observable<List<EpisodeData>> getPlaylist(Context context) {
		if (!_playlistSubject.hasValue())
			notifyPlaylistChange(context);
		return _playlistSubject;
	}

	private static PublishSubject<EpisodeCursor> _changeSubject = PublishSubject.create();
	public static void notifyChange(EpisodeCursor c) {
		_changeSubject.onNext(c);
	}

	private static Observable<EpisodeData> _changeWatcher = _changeSubject.map(EpisodeData::from);
	public static Observable<EpisodeData> getEpisodeWatcher() {
		return _changeWatcher.observeOn(AndroidSchedulers.mainThread());
	}
	public static Observable<EpisodeData> getEpisodeWatcher(long id) {
		return _changeWatcher
			.filter(d -> d.getId() == id)
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<EpisodeData> getNewForSubscriptionIds(Context context, List<Long> subIds) {
		if (subIds.size() == 0)
			return Observable.empty();

		String selection = EpisodeProvider.COLUMN_SUBSCRIPTION_ID + " IN (" + TextUtils.join(",", subIds) + ")" +
			" AND " + EpisodeProvider.COLUMN_PUB_DATE + " > " + (LocalDate.now().minusDays(7).toDate().getTime() / 1000);
		return queryToObservable(context, EpisodeProvider.URI, selection, null, null);
	}
}
