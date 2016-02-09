package com.axelby.podax;

import android.content.Context;
import android.database.Cursor;

import java.util.Date;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class SubscriptionData {

	private final long _id;
	private final String _rawTitle;
	private final String _url;
	private final Date _lastModified;
	private final Date _lastUpdate;
	private final String _etag;
	private final String _thumbnail;
	private final String _titleOverride;
	private final String _description;
	private final boolean _singleUse;
	private final boolean _playlistNew;
	private final int _expirationDays;

	private SubscriptionData(SubscriptionCursor sub) {
		_id = sub.getId();
		_rawTitle = sub.getRawTitle();
		_url = sub.getUrl();
		_lastModified = sub.getLastModified();
		_lastUpdate = sub.getLastUpdate();
		_etag = sub.getETag();
		_thumbnail = sub.getThumbnail();
		_titleOverride = sub.getTitleOverride();
		_description = sub.getDescription();
		_singleUse = sub.isSingleUse();
		_playlistNew = sub.areNewEpisodesAddedToPlaylist();
		_expirationDays = sub.getExpirationDays();
	}

	private static SubscriptionData create(Context context, long id) {
		if (id < 0)
			return null;

		SubscriptionCursor ep = SubscriptionCursor.getCursor(context, id);
		if (ep == null)
			return null;

		SubscriptionData data = new SubscriptionData(ep);
		ep.closeCursor();
		return data;
	}

	public long getId() { return _id; }
	public String getRawTitle() { return _rawTitle; }
	public String getUrl() { return _url; }
	public Date getLastModified() { return _lastModified; }
	public Date getLastUpdate() { return _lastUpdate; }
	public String getEtag() { return _etag; }
	public String getThumbnail() { return _thumbnail; }
	public String getTitleOverride() { return _titleOverride; }
	public String getDescription() { return _description; }
	public boolean isSingleUse() { return _singleUse; }
	public boolean areNewEpisodesAddedToPlaylist() { return _playlistNew; }
	public int getExpirationDays() { return _expirationDays; }
	public String getTitle() {
		if (_titleOverride != null && _titleOverride.length() > 0)
			return _titleOverride;
		return _rawTitle;
	}

	private static PublishSubject<SubscriptionCursor> _changeSubject = PublishSubject.create();
	public static void notifyChange(SubscriptionCursor c) {
		_changeSubject.onNext(c);
	}

	private static Observable<SubscriptionData> _changeWatcher = _changeSubject.map(SubscriptionData::new);
	public static Observable<SubscriptionData> getSubscriptionWatcher() {
		return _changeWatcher.observeOn(AndroidSchedulers.mainThread());
	}
	public static Observable<SubscriptionData> getSubscriptionWatcher(long id) {
		return _changeWatcher
			.filter(d -> d.getId() == id)
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<SubscriptionData> getObservable(Context context, long id) {
		if (id < 0)
			return Observable.empty();

		return SubscriptionData.getSubscriptionWatcher(id)
			.startWith(SubscriptionData.create(context, id))
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<SubscriptionData> getAll(Context context) {
		return Observable.create(subscriber -> {
			Cursor c = context.getContentResolver().query(SubscriptionProvider.URI, null, null, null, null);
			if (c != null) {
				while (c.moveToNext())
					subscriber.onNext(new SubscriptionData(new SubscriptionCursor(c)));
				c.close();
			}
			subscriber.onCompleted();
		});
	}
}
