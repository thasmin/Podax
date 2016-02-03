package com.axelby.podax;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IntDef;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
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
		Observable<EpisodeData> ob = Observable.just(EpisodeData.create(context, episodeId));
		ob.subscribeOn(Schedulers.io());
		ob.observeOn(AndroidSchedulers.mainThread());
		return ob;
	}

	public static final int FINISHED = 0;
	public static final int TO_DOWNLOAD = 1;
	public static final int PLAYLIST = 2;
	@IntDef({FINISHED, TO_DOWNLOAD, PLAYLIST})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Filter {}

	// by default, runs on io thread and is observed on main thread
	public static Observable<EpisodeData> getObservables(Context context, @Filter int filter) {
		Observable<EpisodeData> ob = Observable.create(o -> {
			Cursor c = null;
			switch (filter) {
				case FINISHED:
					c = context.getContentResolver().query(EpisodeProvider.FINISHED_URI, null, null, null, null);
					break;
				case TO_DOWNLOAD:
					c = context.getContentResolver().query(EpisodeProvider.TO_DOWNLOAD_URI, null, null, null, null);
					break;
				case PLAYLIST:
					c = context.getContentResolver().query(EpisodeProvider.PLAYLIST_URI, null, null, null, null);
					break;
			}
			if (c == null) {
				o.onError(new Exception("cursor came back null"));
				return;
			}

			while (c.moveToNext())
				o.onNext(new EpisodeData(new EpisodeCursor(c)));
			o.onCompleted();

			c.close();
		});
		ob.subscribeOn(Schedulers.io());
		ob.observeOn(AndroidSchedulers.mainThread());
		return ob;
	}

	private static PublishSubject<EpisodeCursor> _changeSubject = PublishSubject.create();
	public static void notifyChange(EpisodeCursor c) { _changeSubject.onNext(c); }
	private static Observable<EpisodeData> _changeWatcher = _changeSubject.map(EpisodeData::new);
	public static Observable<EpisodeData> getEpisodeWatcher() { return _changeWatcher.observeOn(AndroidSchedulers.mainThread()); }
	public static Observable<EpisodeData> getEpisodeWatcher(long id) { return _changeWatcher.filter(d -> d.getId() == id).observeOn(AndroidSchedulers.mainThread()); }
}
