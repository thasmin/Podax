package com.axelby.podax;

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
			Cursor c = contentResolver.query(SubscriptionProvider.URI,
					new String[] { "url" }, null, null, null);
			Uri uri = Uri.parse("content://com.axelby.gpodder.podcasts");
			while (c.moveToNext())
				contentResolver.insert(uri, makeUrlValues(c.getString(0)));
		} else if (intent.getAction().equals(
				"com.axelby.gpodder.SUBSCRIPTION_UPDATE")) {
			String[] added = intent
					.getStringArrayExtra("com.axelby.gpodder.SUBSCRIPTION_ADDED");
			String[] removed = intent
					.getStringArrayExtra("com.axelby.gpodder.SUBSCRIPTION_REMOVED");

			for (String url : added)
				contentResolver.insert(SubscriptionProvider.URI,
						makeUrlValues(url));
			for (String url : removed)
				contentResolver.delete(SubscriptionProvider.URI,
						SubscriptionProvider.COLUMN_URL + "=?",
						new String[] { url });
		}
	}

	private ContentValues makeUrlValues(String url) {
		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_URL, url);
		return values;
	}

}
