package com.axelby.podax;

import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

public class GPodderReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		ContentResolver contentResolver = context.getContentResolver();
		if (intent.getAction().equals("com.axelby.gpodder.INITIALIZE")) {
			// update the gpodder db with all of our subscriptions
			Vector<String> in_podax = getPodaxUrls(contentResolver);
			for (String url : in_podax)
				contentResolver.insert(Constants.GPODDER_URI, makeUrlValues(url));
		} else if (intent.getAction().equals(
				"com.axelby.gpodder.SUBSCRIPTION_UPDATE")) {
			syncWithProvider(context);
		}
	}

	public static void syncWithProvider(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		Vector<String> at_gpodder = getGPodderUrls(contentResolver);
		Vector<String> in_podax = getPodaxUrls(contentResolver);

		for (String url : at_gpodder)
			if (!in_podax.contains(url))
				contentResolver.insert(SubscriptionProvider.URI, makeUrlValues(url));

		for (String url : in_podax)
			if (!at_gpodder.contains(url))
				contentResolver.delete(SubscriptionProvider.URI,
						SubscriptionProvider.COLUMN_URL + "=?",
						new String[] { url });
	}

	private static Vector<String> getPodaxUrls(ContentResolver contentResolver) {
		return retrieveUrls(contentResolver, SubscriptionProvider.URI);
	}

	private static Vector<String> getGPodderUrls(ContentResolver contentResolver) {
		return retrieveUrls(contentResolver, Constants.GPODDER_URI);
	}

	private static Vector<String> retrieveUrls(ContentResolver contentResolver, Uri uri) {
		Vector<String> urls = new Vector<String>();
		Cursor c = contentResolver.query(uri, new String[] { "url" }, null, null, null);
		// not sure why this happens
		if (c == null)
			return urls;
		while (c.moveToNext())
			urls.add(c.getString(0));
		c.close();
		return urls;
	}

	private static ContentValues makeUrlValues(String url) {
		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_URL, url);
		return values;
	}

}
