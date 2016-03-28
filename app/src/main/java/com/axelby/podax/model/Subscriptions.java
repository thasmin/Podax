package com.axelby.podax.model;

import android.content.Context;
import android.util.Log;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class Subscriptions {
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

	private static Context _context;

	public static void setContext(Context context) {
		_context = context;
	}

	private static PublishSubject<SubscriptionData> _changeSubject = PublishSubject.create();
	public static void notifyChange(SubscriptionData sub) {
		SubscriptionData data = SubscriptionData.cacheSwap(sub);
		_changeSubject.onNext(data);
	}

	public static Observable<SubscriptionData> watchAll() {
		return _changeSubject
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread());
	}

	public static Observable<SubscriptionData> watch(long id) {
		if (id < 0)
			return Observable.empty();

		return _changeSubject
			.filter(d -> d.getId() == id)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.startWith(SubscriptionData.create(id));
	}

	public static Observable<SubscriptionData> getAll() {
		return Observable.from(PodaxDB.subscriptions.getAll());
	}

	public static Observable<SubscriptionData> getFor(String field, int value) {
		return Observable.from(PodaxDB.subscriptions.getFor(field, value));
	}

	public static Observable<SubscriptionData> getFor(String field, String value) {
		return Observable.from(PodaxDB.subscriptions.getFor(field, value));
	}

	public static SubscriptionData getForRSSUrl(String rssUrl) {
		List<SubscriptionData> subs = PodaxDB.subscriptions.getFor(Subscriptions.COLUMN_URL, rssUrl);
		if (subs.size() == 0)
			return null;
		return subs.get(0);
	}

	public static Observable<List<SubscriptionData>> search(String query) {
		return Observable.just(PodaxDB.subscriptions.search(query));
	}

	public static void delete(long subscriptionId) {
		SubscriptionData sub = SubscriptionData.create(subscriptionId);
		if (sub != null)
			PodaxDB.gPodder.remove(sub.getUrl());
		doDelete(subscriptionId);
	}

	public static void deleteViaGPodder(String url) {
		SubscriptionData sub = Subscriptions.getForRSSUrl(url);
		if (sub != null)
			doDelete(sub.getId());
	}

	private static void doDelete(long subscriptionId) {
		Episodes.getForSubscriptionId(subscriptionId)
			.flatMapIterable(sub -> sub)
			.subscribe(
				s -> Episodes.delete(s.getId()),
				e -> Log.e("Subscriptions", "unable to retrieve episodes to delete", e)
			);

		SubscriptionData.evictThumbnails(_context, subscriptionId);
		PodaxDB.subscriptions.delete(subscriptionId);

		// TODO: notify everyone somehow
	}

	public static void evictCache() {
		SubscriptionData.evictCache();
		_changeSubject = PublishSubject.create();
	}
}
