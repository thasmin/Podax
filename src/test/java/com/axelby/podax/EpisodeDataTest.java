package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class EpisodeDataTest {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Test
	public void testGetEpisode() {
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
		Assert.assertEquals("original title is incorrect", "huh?", epSubscriber.getOnNextEvents().get(0).getTitle());

		values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_TITLE, "oh i see");
		context.getContentResolver().update(epUri, values, null, null);

		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(2);
		Assert.assertEquals("original title is incorrect", "oh i see", epSubscriber.getOnNextEvents().get(1).getTitle());
	}

	@Test
	public void testGetPlaylist() {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues(2);
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		values = new ContentValues(4);
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		values = new ContentValues(4);
		values.put(EpisodeProvider.COLUMN_TITLE, "two");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://2");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		Uri ep2Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep2Uri);

		TestSubscriber<List<EpisodeData>> testSubscriber = new TestSubscriber<>();
		EpisodeData.getPlaylist(context).subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should be two items in playlist", 2, testSubscriber.getOnNextEvents().get(0).size());

		values = new ContentValues(1);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
		context.getContentResolver().update(ep1Uri, values, null, null);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("should be one items in playlist", 1, testSubscriber.getOnNextEvents().get(1).size());
	}
}