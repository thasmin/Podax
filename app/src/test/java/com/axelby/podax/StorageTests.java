package com.axelby.podax;

import android.content.Context;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class StorageTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testDeletePodcast() throws Exception {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://test.mp3")
			.setTitle("Test Episode")
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, epId);

		EpisodeData ep = EpisodeData.create(epId);
		if (ep == null)
			throw new Exception("couldn't load episode data");

		String filename = ep.getFilename(context);
		File file = new File(filename);
		file.getParentFile().mkdirs();
		file.createNewFile();
		PodaxDB.episodes.delete(epId);
		Assert.assertEquals(false, file.exists());
		file.delete();
	}
}

