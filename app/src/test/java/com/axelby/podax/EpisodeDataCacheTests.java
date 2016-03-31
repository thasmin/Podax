package com.axelby.podax;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class EpisodeDataCacheTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Ignore
	@Test
	public void testGetEpisode() {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("cannot create subscription", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1.mp3").setTitle("huh?").commit();
		Assert.assertNotEquals("cannot create episode", -1, epId);

		int loops = 1000000;
		double n;

		n = System.nanoTime();
		for (int i = 0; i < loops; ++i) {
			EpisodeData.evictCache();
			EpisodeData.create(epId);
		}
		System.out.println("no cache: " + (System.nanoTime() - n) / 1e+9);

		n = System.nanoTime();
		for (int i = 0; i < loops; ++i) {
			EpisodeData.create(epId);
		}
		System.out.println("with cache: " + (System.nanoTime() - n) / 1e+9);
	}
}
