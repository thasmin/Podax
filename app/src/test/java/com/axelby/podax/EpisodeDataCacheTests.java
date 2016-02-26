package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class EpisodeDataCacheTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Ignore
	@Test
	public void testGetEpisode() {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "huh?");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
		Uri epUri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", epUri);

		Cursor c = context.getContentResolver().query(EpisodeProvider.URI, null, null, null, null);
		Assert.assertNotNull(c);
		int loops = 1000000;
		double n;

		n = System.nanoTime();
		for (int i = 0; i < loops; ++i) {
			c.moveToFirst();
			new EpisodeCursor(c);
		}
		System.out.println("baseline: " + (System.nanoTime() - n) / 1e+9);

		n = System.nanoTime();
		for (int i = 0; i < loops; ++i) {
			c.moveToFirst();
			EpisodeCursor ec = new EpisodeCursor(c);
			EpisodeData.evictCache();
			EpisodeData.from(ec);
		}
		System.out.println("no cache: " + (System.nanoTime() - n) / 1e+9);

		n = System.nanoTime();
		for (int i = 0; i < loops; ++i) {
			c.moveToFirst();
			EpisodeCursor ec = new EpisodeCursor(c);
			EpisodeData.from(ec);
		}
		System.out.println("with cache: " + (System.nanoTime() - n) / 1e+9);

	}
}
