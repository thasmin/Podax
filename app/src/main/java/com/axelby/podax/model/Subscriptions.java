package com.axelby.podax.model;

import android.content.Context;

import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class Subscriptions {

	private static Context _context;

	public static void setContext(Context context) {
		_context = context;
	}

	private static PublishSubject<SubscriptionData> _changeSubject = PublishSubject.create();
	public static void notifyChange(SubscriptionCursor c) {
		SubscriptionData data = SubscriptionData.cacheSwap(c);
		_changeSubject.onNext(data);
	}

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
			.startWith(SubscriptionData.create(_context, id));
	}

	/* -------
	   helpers
	   ------- */

	public static Observable<SubscriptionData> getAll() {
		return Observable.from(PodaxDB.subscriptions.getAll());
	}

	public static Observable<SubscriptionData> getFor(String field, int value) {
		return Observable.from(PodaxDB.subscriptions.getFor(field, value));
	}

	public static Observable<SubscriptionData> getFor(String field, String value) {
		return Observable.from(PodaxDB.subscriptions.getFor(field, value));
	}

	public static Observable<SubscriptionData> getForRSSUrl(String rssUrl) {
		return getFor(SubscriptionProvider.COLUMN_URL, rssUrl);
	}

	public static void evictCache() {
		SubscriptionData.evictCache();
		_changeSubject = PublishSubject.create();
	}
}
