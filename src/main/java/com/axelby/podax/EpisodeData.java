package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class EpisodeData {

	// TODO: change Date to LocalDateTime
	private final long _id;
	private final String _title;
	private final long _subscriptionId;
	private final String _subscriptionTitle;
	private final String _subscriptionUrl;
	private final Integer _playlistPosition;
	private final String _mediaUrl;
	private final Integer _fileSize;
	private final String _description;
	private final String _link;
	private final int _lastPosition;
	private final int _duration;
	private final Date _pubDate;
	private final Date _gpodderUpdateTimestamp;
	private final String _payment;
	private final Date _finishedDate;

	private String _filename = null;

	public EpisodeData(EpisodeCursor ep) {
		_id = ep.getId();
		_title = ep.getTitle();
		_subscriptionId = ep.getSubscriptionId();
		_subscriptionTitle = ep.getSubscriptionTitle();
		_subscriptionUrl = ep.getSubscriptionUrl();
		_playlistPosition = ep.getPlaylistPosition();
		_mediaUrl = ep.getMediaUrl();
		_fileSize = ep.getFileSize();
		_description = ep.getDescription();
		_link = ep.getLink();
		_lastPosition = ep.getLastPosition();
		_duration = ep.getDuration();
		_pubDate = ep.getPubDate();
		_gpodderUpdateTimestamp = ep.getGPodderUpdateTimestamp();
		_payment = ep.getPaymentUrl();
		_finishedDate = ep.getFinishedDate();
	}

	public static EpisodeData create(Context context, long episodeId) {
		if (episodeId < 0)
			return null;

		EpisodeCursor ep = EpisodeCursor.getCursor(context, episodeId);
		if (ep == null)
			return null;

		EpisodeData d = new EpisodeData(ep);
		ep.closeCursor();
		return d;
	}

	public long getId() {
		return _id;
	}

	public String getTitle() {
		return _title;
	}

	public long getSubscriptionId() {
		return _subscriptionId;
	}

	public String getSubscriptionTitle() {
		return _subscriptionTitle;
	}

	public String getSubscriptionUrl() {
		return _subscriptionUrl;
	}

	public Integer getPlaylistPosition() {
		return _playlistPosition;
	}

	public String getMediaUrl() {
		return _mediaUrl;
	}

	public Integer getFileSize() {
		return _fileSize;
	}

	public String getDescription() {
		return _description;
	}

	public String getLink() {
		return _link;
	}

	public int getLastPosition() {
		return _lastPosition;
	}

	public int getDuration() {
		return _duration;
	}

	public Date getPubDate() {
		return _pubDate;
	}

	public Date getGpodderUpdateTimestamp() {
		return _gpodderUpdateTimestamp;
	}

	public String getPaymentUrl() {
		return _payment;
	}

	public Date getFinishedDate() {
		return _finishedDate;
	}

	public String getFilename(Context context) {
		if (_filename == null)
			_filename = String.format("%s%s.%s",
				EpisodeCursor.getPodcastStoragePath(context),
				String.valueOf(getId()),
				EpisodeCursor.getExtension(getMediaUrl())
			);

		return _filename;
	}

	public boolean isDownloaded(Context context) {
		if (getFileSize() == null)
			return false;
		File file = new File(getFilename(context));
		return file.exists() && file.length() == getFileSize() && getFileSize() != 0;
	}

	private Uri getContentUri() {
		return ContentUris.withAppendedId(EpisodeProvider.URI, getId());
	}

	/* -------
	   actions
	   ------- */

	public void removeFromPlaylist(Context context) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void addToPlaylist(Context context) {
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		context.getContentResolver().update(getContentUri(), values, null, null);
	}

	/* ------------
	   data binding
	   ------------ */

	public void show(View view) {
		View thumbnail = view.findViewById(R.id.thumbnail);
		View title = view.findViewById(R.id.title);
		AppFlow.get(view.getContext()).displayEpisode(_id, thumbnail, title);
	}

	public void play(View view) {
		PlayerService.play(view.getContext(), _id);
	}

	public void removeFromPlaylist(View view) {
		removeFromPlaylist(view.getContext());
	}

	public void togglePlaylist(View view) {
		if (getPlaylistPosition() == null)
			addToPlaylist(view.getContext());
		else
			removeFromPlaylist(view.getContext());
	}

	public boolean hasDuration() {
		return getDuration() != 0;
	}

	public String getDurationAsWords(Context context) {
		return Helper.getVerboseTimeString(context, getDuration() / 1000.0f, false) + " long";
	}

	public String getDownloadStatus(Context context) {
		String episodeFilename = getFilename(context);
		float downloaded = new File(episodeFilename).length();
		if (getFileSize() == downloaded)
			return context.getString(R.string.downloaded);
		else if (EpisodeDownloadService.isDownloading(episodeFilename))
			return context.getString(R.string.now_downloading);
		else
			return context.getString(R.string.not_downloaded);
	}

	public int getDownloadStatusColor(Context context) {
		String filename = getFilename(context);
		float downloaded = new File(filename).length();
		if (getFileSize() == downloaded || EpisodeDownloadService.isDownloading(filename))
			return android.R.color.holo_green_dark;
		else
			return android.R.color.holo_red_dark;
	}

	public String getReleaseDate(Context context) {
		return context.getString(R.string.released_on) + " " + DateFormat.getInstance().format(getPubDate());
	}

	/* --
	   rx
	   -- */

	private static Observable<EpisodeData> queryToObservable(Context context,
			 Uri uri, String selection, String[] selectionArgs, String sortOrder) {
		return Observable.create(subscriber -> {
			Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				while (cursor.moveToNext())
					subscriber.onNext(new EpisodeData(new EpisodeCursor(cursor)));
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
				list.add(new EpisodeData(new EpisodeCursor(cursor)));
			cursor.close();

			subscriber.onNext(list);
			subscriber.onCompleted();
		});
	}

	public static Observable<EpisodeData> getObservable(Context context, long episodeId) {
		return EpisodeData.getEpisodeWatcher(episodeId)
			.startWith(EpisodeData.create(context, episodeId))
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<List<EpisodeData>> getForSubscriptionId(Context context, String[] selectionArgs) {
		String selection = EpisodeProvider.COLUMN_SUBSCRIPTION_ID + "=?";
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
			finished.add(new EpisodeData(new EpisodeCursor(c)));
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
			playlist.add(new EpisodeData(new EpisodeCursor(c)));
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

	private static Observable<EpisodeData> _changeWatcher = _changeSubject.map(EpisodeData::new);
	public static Observable<EpisodeData> getEpisodeWatcher() {
		return _changeWatcher.observeOn(AndroidSchedulers.mainThread());
	}
	public static Observable<EpisodeData> getEpisodeWatcher(long id) {
		return _changeWatcher
			.filter(d -> d.getId() == id)
			.observeOn(AndroidSchedulers.mainThread());
	}
}
