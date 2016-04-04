package com.axelby.podax;

import android.content.Context;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
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
		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("huh?")
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		TestSubscriber<EpisodeData> epSubscriber = new TestSubscriber<>();
		PodaxDB.episodes.watch(epId).subscribe(epSubscriber);
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
		PodaxDB.episodes.watchPlaylist().subscribe(testSubscriber);
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
		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		TestSubscriber<List<EpisodeData>> testSubscriber = new TestSubscriber<>();
		PodaxDB.episodes.watchFinished().subscribe(testSubscriber);
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

		List<EpisodeData> expired = PodaxDB.episodes.getExpired();
		Assert.assertEquals("should be one expired episode", 1, expired.size());
		Assert.assertEquals("only second episode should be expired", "two", expired.get(0).getTitle());
	}

	@Test
	public void getLatestActivity() {
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

		List<EpisodeData> activity = PodaxDB.episodes.getLatestActivity();
		Assert.assertEquals("should be two episodes", 2, activity.size());
		Assert.assertEquals("one should be first", "one", activity.get(0).getTitle());
		Assert.assertEquals("two should be second", "two", activity.get(1).getTitle());
	}

	@Test
	public void getLastActivity() {
		long subId = SubscriptionEditor.create("test").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long when = LocalDateTime.now().plusDays(-1).toDate().getTime();

		long epId = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPubDate(new Date(when))
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		Assert.assertTrue("latest activity should be after when - 1", PodaxDB.episodes.isLastActivityAfter(when / 1000 - 1));
		Assert.assertFalse("latest activity should not be after when + 1", PodaxDB.episodes.isLastActivityAfter(when / 1000 + 1));
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

		List<EpisodeData> needsDownload = PodaxDB.episodes.getNeedsDownload();
		Assert.assertEquals("should be one episode that needs to be downloaded", 1, needsDownload.size());

		EpisodeData ep = needsDownload.get(0);
		FileWriter fw = new FileWriter(ep.getFilename(context));
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();

		needsDownload = PodaxDB.episodes.getNeedsDownload();
		Assert.assertEquals("should be no episodes that need to be downloaded", 0, needsDownload.size());
	}
}