package com.axelby.podax;

import android.content.Context;
import android.database.Cursor;

import java.io.File;
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

	public static Observable<EpisodeData> getObservable(Context context, long episodeId) {
		Observable<EpisodeData> ob = EpisodeData.getEpisodeWatcher(episodeId)
			.startWith(EpisodeData.create(context, episodeId));
		ob.subscribeOn(Schedulers.io());
		ob.observeOn(AndroidSchedulers.mainThread());
		return ob;
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
