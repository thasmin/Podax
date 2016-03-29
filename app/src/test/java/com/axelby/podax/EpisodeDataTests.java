package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.SubscriptionEditor;

import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class EpisodeDataTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testGetEpisode() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "huh?");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		Uri epUri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", epUri);

		TestSubscriber<EpisodeData> epSubscriber = new TestSubscriber<>();
		long epId = ContentUris.parseId(epUri);
		EpisodeDB.getObservable(epId).subscribe(epSubscriber);
		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(1);
		Assert.assertEquals("original title is incorrect", "huh?", epSubscriber.getOnNextEvents().get(0).getTitle());

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "oh i see");
		context.getContentResolver().update(epUri, values, null, null);

		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(2);
		Assert.assertEquals("original title is incorrect", "oh i see", epSubscriber.getOnNextEvents().get(1).getTitle());
	}

	@Test
	public void testGetPlaylist() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "two");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://2");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		Uri ep2Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep2Uri);

		TestSubscriber<List<EpisodeData>> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getPlaylist().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should be two items in playlist", 2, testSubscriber.getOnNextEvents().get(0).size());

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
		context.getContentResolver().update(ep1Uri, values, null, null);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("should be one items in playlist", 1, testSubscriber.getOnNextEvents().get(1).size());
	}

	@Test
	public void testGetFinished() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		TestSubscriber<List<EpisodeData>> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getFinished().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should be no finished episodes", 0, testSubscriber.getOnNextEvents().get(0).size());

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_FINISHED_TIME, new Date().getTime() / 1000);
		context.getContentResolver().update(ep1Uri, values, null, null);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("should be one finished episode", 1, testSubscriber.getOnNextEvents().get(1).size());
	}

	@Test
	public void getExpired() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").setExpirationDays(7).commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		LocalDateTime now = LocalDateTime.now();

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		values.put(EpisodeProvider.COLUMN_PUB_DATE, now.plusDays(-1).toDate().getTime() / 1000);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "two");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://2");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		values.put(EpisodeProvider.COLUMN_PUB_DATE, now.plusDays(-8).toDate().getTime() / 1000);
		Uri ep2Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep2Uri);

		TestSubscriber<EpisodeData> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getExpired().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("only second episode should be expired", "two", testSubscriber.getOnNextEvents().get(0).getTitle());
	}

	@Test
	public void getLatestActivity() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		LocalDateTime now = LocalDateTime.now();

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PUB_DATE, now.plusDays(-1).toDate().getTime() / 1000);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "two");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://2");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PUB_DATE, now.plusDays(-8).toDate().getTime() / 1000);
		Uri ep2Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep2Uri);

		TestSubscriber<EpisodeData> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getLatestActivity().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("one should be first", "one", testSubscriber.getOnNextEvents().get(0).getTitle());
		Assert.assertEquals("two should be second", "two", testSubscriber.getOnNextEvents().get(1).getTitle());
	}

	@Test
	public void getLastActivity() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long when = LocalDateTime.now().plusDays(-1).toDate().getTime() / 1000;

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_PUB_DATE, when);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		Assert.assertTrue("latest activity should be after when - 1", EpisodeDB.isLastActivityAfter(when - 1));
		Assert.assertFalse("latest activity should be after when + 1", EpisodeDB.isLastActivityAfter(when + 1));
	}

	@Test
	public void getNeedsDownload() throws IOException {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1.mp3");
		values.put(EpisodeProvider.COLUMN_FILE_SIZE, 5);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, 0);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		TestSubscriber<EpisodeData> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getNeedsDownload().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		testSubscriber.unsubscribe();

		EpisodeData ep = testSubscriber.getOnNextEvents().get(0);
		FileWriter fw = new FileWriter(ep.getFilename(context));
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();

		testSubscriber = new TestSubscriber<>();
		EpisodeDB.getNeedsDownload().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(0);
		testSubscriber.unsubscribe();
	}
}