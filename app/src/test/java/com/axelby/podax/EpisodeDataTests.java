package com.axelby.podax;

import android.content.Context;

import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
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

		long epId = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("huh?")
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		TestSubscriber<EpisodeData> epSubscriber = new TestSubscriber<>();
		EpisodeDB.getObservable(epId).subscribe(epSubscriber);
		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(1);
		Assert.assertEquals("original title is incorrect", "huh?", epSubscriber.getOnNextEvents().get(0).getTitle());

		new EpisodeEditor(epId).setTitle("oh i see").commit();

		epSubscriber.assertNoErrors();
		epSubscriber.assertValueCount(2);
		Assert.assertEquals("original title is incorrect", "oh i see", epSubscriber.getOnNextEvents().get(1).getTitle());
	}

	@Test
	public void testGetPlaylist() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long ep1Id = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("unable to create episode 1", -1, ep1Id);

		long ep2Id = EpisodeEditor.fromNew(subId, "test://2")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("unable to create episode 2", -1, ep2Id);

		TestSubscriber<List<EpisodeData>> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getPlaylist().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should be two items in playlist", 2, testSubscriber.getOnNextEvents().get(0).size());

		new EpisodeEditor(ep1Id).setPlaylistPosition(null).commit();

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("should be one items in playlist", 1, testSubscriber.getOnNextEvents().get(1).size());
	}

	@Test
	public void testGetFinished() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		TestSubscriber<List<EpisodeData>> testSubscriber = new TestSubscriber<>();
		EpisodeDB.getFinished().subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should be no finished episodes", 0, testSubscriber.getOnNextEvents().get(0).size());

		new EpisodeEditor(epId).setFinishedDate(new Date(new Date().getTime() / 1000)).commit();
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

		long ep1Id = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.setPubDate(new Date(now.plusDays(-1).toDate().getTime()))
			.commit();
		Assert.assertNotEquals("unable to create episode 1", -1, ep1Id);

		long ep2Id = EpisodeEditor.fromNew(subId, "test://2")
			.setTitle("two")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.setPubDate(new Date(now.plusDays(-8).toDate().getTime()))
			.commit();
		Assert.assertNotEquals("unable to create episode 2", -1, ep2Id);

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

		long ep1Id = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPubDate(now.plusDays(-1).toDate())
			.commit();
		Assert.assertNotEquals("unable to create episode 1", -1, ep1Id);

		long ep2Id = EpisodeEditor.fromNew(subId, "test://2")
			.setTitle("two")
			.setPubDate(now.plusDays(-8).toDate())
			.commit();
		Assert.assertNotEquals("unable to create episode 2", -1, ep2Id);

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

		long when = LocalDateTime.now().plusDays(-1).toDate().getTime();

		long epId = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPubDate(new Date(when))
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		Assert.assertTrue("latest activity should be after when - 1", EpisodeDB.isLastActivityAfter(when / 1000 - 1));
		Assert.assertFalse("latest activity should not be after when + 1", EpisodeDB.isLastActivityAfter(when / 1000 + 1));
	}

	@Test
	public void getNeedsDownload() throws IOException {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1.mp3")
			.setTitle("one")
			.setPlaylistPosition(0)
			.setFileSize(5)
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

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