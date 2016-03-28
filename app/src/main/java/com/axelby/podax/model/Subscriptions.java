package com.axelby.podax.model;

import android.content.Context;
import android.util.Log;

import com.axelby.podax.SubscriptionCursor;

import java.util.List;

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
		List<SubscriptionData> subs = PodaxDB.subscriptions.getFor(SubscriptionDB.COLUMN_URL, rssUrl);
		if (subs.size() == 0)
			return null;
		return subs.get(0);
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

		SubscriptionCursor.evictThumbnails(_context, subscriptionId);
		PodaxDB.subscriptions.delete(subscriptionId);

		// TODO: notify everyone somehow
	}

	public static void evictCache() {
		SubscriptionData.evictCache();
		_changeSubject = PublishSubject.create();
	}
}
