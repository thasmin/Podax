package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class EpisodeDataTest {

	@Test
	public void testGetObservable() throws Exception {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues(2);
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		values = new ContentValues(3);
		values.put(EpisodeProvider.COLUMN_TITLE, "huh?");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
		Uri epUri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", epUri);

		TestSubscriber<EpisodeData> epSubscriber = new TestSubscriber<>();
		long epId = ContentUris.parseId(epUri);
		EpisodeData.getObservable(context, epId).subscribe(epSubscriber);
		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(1);

		values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_TITLE, "oh i see");
		context.getContentResolver().update(epUri, values, null, null);

		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(1);
	}
}