package com.axelby.podax;

import android.content.Context;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class EpisodeDownloaderTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void verifyProperFiles() throws IOException {
		Context context = RuntimeEnvironment.application;

		// create subscription
		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1.mp3")
			.setTitle("one")
			.setFileSize(5)
			.setPlaylistPosition(0)
			.commit();
		Assert.assertNotEquals("unable to create episode", -1, epId);

		EpisodeData ep = EpisodeData.create(epId);
		Assert.assertNotNull("episode data should not be null", ep);
		String epfile = ep.getFilename(context);
		FileWriter fw = new FileWriter(epfile);
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();
		Assert.assertTrue("episode file should exist", new File(epfile).exists());

		String badfile = String.format("%sbad.mp3", Storage.getPodcastStoragePath(context));
		fw = new FileWriter(badfile);
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();

		EpisodeDownloadService.verifyDownloadedFiles(context);

		Assert.assertTrue("episode file should exist", new File(epfile).exists());
		Assert.assertTrue("bad file should not exist", !new File(badfile).exists());
	}
}
