package com.axelby.podax;

import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class GPodderTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testDB() {
		PodaxDB.gPodder.add("http://www.axelby.com/podcast1.xml");
		List<String> toAdd = PodaxDB.gPodder.getToAdd();
		Assert.assertEquals(1, toAdd.size());
		Assert.assertEquals("http://www.axelby.com/podcast1.xml", toAdd.get(0));

		PodaxDB.gPodder.remove("http://www.axelby.com/podcast2.xml");
		List<String> toRemove = PodaxDB.gPodder.getToAdd();
		Assert.assertEquals(1, toRemove.size());
		Assert.assertEquals("http://www.axelby.com/podcast1.xml", toRemove.get(0));

		PodaxDB.gPodder.clear();
		Assert.assertEquals(0, PodaxDB.gPodder.getToAdd().size());
		Assert.assertEquals(0, PodaxDB.gPodder.getToRemove().size());
	}

	@Test
	public void subscriptionsHandledProperly() {
		long sub1Id = SubscriptionEditor.create("test://1").commit();
		Assert.assertNotEquals("failed to create first subscription", -1, sub1Id);
		Assert.assertEquals("subscription not queued for gpodder", 1, PodaxDB.gPodder.getToAdd().size());

		long sub2Id = SubscriptionEditor.createViaGPodder("test://2").commit();
		Assert.assertNotEquals("failed to create second subscription", -1, sub2Id);
		Assert.assertEquals("subscription queued for gpodder but shouldn't be", 1, PodaxDB.gPodder.getToAdd().size());
	}

	@Test
	public void episodePositionUpdates() {
		long sub1Id = SubscriptionEditor.create("test://1").commit();
		Assert.assertNotEquals("failed to create subscription", -1, sub1Id);

		long epId = EpisodeEditor.fromNew(sub1Id, "test://1.mp3").commit();
		Assert.assertNotEquals("failed to create episode", -1, epId);
		Assert.assertEquals("creating episode should not sync position to gpodder", 0, PodaxDB.gPodder.getEpisodesToUpdate().size());

		new EpisodeEditor(epId).setLastPosition(1000).commit();
		Assert.assertEquals("episode position not synced to gpodder", 1, PodaxDB.gPodder.getEpisodesToUpdate().size());

		PodaxDB.gPodder.clear();
		Assert.assertEquals("episode position should not be synced again", 0, PodaxDB.gPodder.getEpisodesToUpdate().size());
	}
}
