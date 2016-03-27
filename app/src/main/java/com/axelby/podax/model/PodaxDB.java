package com.axelby.podax.model;

import android.content.Context;

public class PodaxDB {
	public static GPodderDB gPodder;

	public static void setContext(Context context) {
		DBAdapter dbAdapter = new DBAdapter(context);
		gPodder = new GPodderDB(dbAdapter);
	}
}
