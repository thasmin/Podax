package com.axelby.podax;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DBTests {

	@Test
	public void testSubscriptionSearch() throws Exception {
		Context context = RuntimeEnvironment.application;
		ContentResolver resolver = context.getContentResolver();

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "Test Subscription");
		values.put(SubscriptionProvider.COLUMN_URL, "test");
		resolver.insert(SubscriptionProvider.URI, values);

		String[] selectionArgs = {"test"};
		Cursor c = resolver.query(SubscriptionProvider.SEARCH_URI, null, null, selectionArgs, null);
		if (c == null) {
			Assert.fail("unable to get cursor");
			return;
		}
		Assert.assertEquals("search results", 1, c.getCount());
		c.close();
	}

}

