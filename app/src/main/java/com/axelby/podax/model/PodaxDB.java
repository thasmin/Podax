package com.axelby.podax.model;

import android.content.Context;

public class PodaxDB {
	public static GPodderDB gPodder;
	public static SubscriptionDB subscriptions;
	public static EpisodeDB episodes;

	public static void setContext(Context context) {
		DBAdapter dbAdapter = new DBAdapter(context);
		gPodder = new GPodderDB(dbAdapter);
		subscriptions = new SubscriptionDB(dbAdapter);
		episodes = new EpisodeDB(dbAdapter);
	}
}
