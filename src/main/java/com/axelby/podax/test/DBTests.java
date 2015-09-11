package com.axelby.podax.test;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;

import com.axelby.podax.SubscriptionProvider;

public class DBTests extends AndroidTestCase {
	public void testSubscriptionSearch() throws Exception {
		Context context = getContext();
		ContentResolver resolver = context.getContentResolver();

		resolver.delete(SubscriptionProvider.URI, "url = ?", new String[] { "test"});

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "Test Subscription");
		values.put(SubscriptionProvider.COLUMN_URL, "test");
		resolver.insert(SubscriptionProvider.URI, values);

		String[] selectionArgs = {"test"};
		Cursor c = resolver.query(SubscriptionProvider.SEARCH_URI, null, null, selectionArgs, null);
		if (c == null) {
			fail("unable to get cursor");
			return;
		}
		assertEquals("search results", 1, c.getCount());
		c.close();
	}
}
