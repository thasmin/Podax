package com.axelby.podax;

import android.content.Context;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class StorageTests {

	@Test
	public void testDeletePodcast() throws Exception {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(context, subId, "test://test.mp3")
			.setTitle("Test Episode")
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, epId);

		EpisodeData ep = EpisodeData.create(context, epId);
		if (ep == null)
			throw new Exception("couldn't load episode data");

		String filename = ep.getFilename(context);
		try {
			boolean created = new File(filename).createNewFile();
			if (!created)
				throw new Exception("unable to create filename");
			PodaxDB.episodes.delete(epId);
			Assert.assertEquals(false, new File(filename).exists());
		} finally {
			if (filename != null)
				new File(filename).delete();
		}
	}
}

