package com.axelby.podax;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DBTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testSubscriptionSearch() throws Exception {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("Good Subscription").commit();
		SubscriptionEditor.create("test://2").setRawTitle("Bad Subscription").commit();

		TestSubscriber<SubscriptionData> testSubscriber = new TestSubscriber<>();
		Observable.from(PodaxDB.subscriptions.search("good")).subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("search results", subId, testSubscriber.getOnNextEvents().get(0).getId());
	}

	@Test
	public void testEpisodeReordering() throws Exception {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("Subscription").commit();

		long ep1Id = EpisodeEditor.fromNew(subId, "test://1").setTitle("first").setPlaylistPosition(0).commit();
		long ep2Id = EpisodeEditor.fromNew(subId, "test://2").setTitle("second").commit();

		List<EpisodeData> playlist;
		playlist = PodaxDB.episodes.getPlaylist();
		Assert.assertEquals("first playlist size", 1, playlist.size());
		Assert.assertEquals("first playlist ep1 not only one in playlist", ep1Id, playlist.get(0).getId());
		Assert.assertEquals("first playlist ep1 not position 0", 0, playlist.get(0).getPlaylistPosition().intValue());

		new EpisodeEditor(ep2Id).setPlaylistPosition(0).commit();
		playlist = PodaxDB.episodes.getPlaylist();
		Assert.assertEquals("second playlist size", 2, playlist.size());
		Assert.assertEquals("second playlist ep2 not first",  ep2Id, playlist.get(0).getId());
		Assert.assertEquals("second playlist ep1 not second", ep1Id, playlist.get(1).getId());
		Assert.assertEquals("second playlist ep1 not position 0", 0, playlist.get(0).getPlaylistPosition().intValue());
		Assert.assertEquals("second playlist ep2 not position 1", 1, playlist.get(1).getPlaylistPosition().intValue());

		new EpisodeEditor(ep2Id).setPlaylistPosition(1).commit();
		playlist = PodaxDB.episodes.getPlaylist();
		Assert.assertEquals("third playlist size", 2, playlist.size());
		Assert.assertEquals("third playlist ep1 not first",  ep1Id, playlist.get(0).getId());
		Assert.assertEquals("third playlist ep2 not second", ep2Id, playlist.get(1).getId());
		Assert.assertEquals("third playlist ep1 not position 0", 0, playlist.get(0).getPlaylistPosition().intValue());
		Assert.assertEquals("third playlist ep2 not position 1", 1, playlist.get(1).getPlaylistPosition().intValue());

		new EpisodeEditor(ep1Id).setPlaylistPosition(null).commit();
		playlist = PodaxDB.episodes.getPlaylist();
		Assert.assertEquals("fourth playlist size", 1, playlist.size());
		Assert.assertEquals("fourth playlist ep1 only one in playlist", ep2Id, playlist.get(0).getId());
		Assert.assertEquals("fourth playlist ep1 not position 0", 0, playlist.get(0).getPlaylistPosition().intValue());

		new EpisodeEditor(ep1Id).setPlaylistPosition(0).commit();
		long ep3Id = EpisodeEditor.fromNew(subId, "test://3").setTitle("third").setPlaylistPosition(Integer.MAX_VALUE).commit();
		playlist = PodaxDB.episodes.getPlaylist();
		Assert.assertEquals("fifth playlist size", 3, playlist.size());
		Assert.assertEquals("fifth playlist ep1 not first",  ep1Id, playlist.get(0).getId());
		Assert.assertEquals("fifth playlist ep2 not second", ep2Id, playlist.get(1).getId());
		Assert.assertEquals("fifth playlist ep3 not third",  ep3Id, playlist.get(2).getId());
		Assert.assertEquals("sixth playlist ep1 not position 0", 0, playlist.get(0).getPlaylistPosition().intValue());
		Assert.assertEquals("sixth playlist ep2 not position 1", 1, playlist.get(1).getPlaylistPosition().intValue());
		Assert.assertEquals("sixth playlist ep3 not position 2", 2, playlist.get(2).getPlaylistPosition().intValue());

		new EpisodeEditor(ep3Id).setPlaylistPosition(1).commit();
		playlist = PodaxDB.episodes.getPlaylist();
		Assert.assertEquals("sixth playlist size", 3, playlist.size());
		Assert.assertEquals("sixth playlist ep1 not first",  ep1Id, playlist.get(0).getId());
		Assert.assertEquals("sixth playlist ep3 not second", ep3Id, playlist.get(1).getId());
		Assert.assertEquals("sixth playlist ep2 not third",  ep2Id, playlist.get(2).getId());
		Assert.assertEquals("sixth playlist ep1 not position 0", 0, playlist.get(0).getPlaylistPosition().intValue());
		Assert.assertEquals("sixth playlist ep3 not position 1", 1, playlist.get(1).getPlaylistPosition().intValue());
		Assert.assertEquals("sixth playlist ep2 not position 2", 2, playlist.get(2).getPlaylistPosition().intValue());
	}

}

