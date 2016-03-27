package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@RunWith(RobolectricGradleTestRunner.class)
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

		// put one episode in queue
		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1.mp3");
		values.put(EpisodeProvider.COLUMN_FILE_SIZE, 5);
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, 0);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		EpisodeData ep = EpisodeData.create(context, ContentUris.parseId(ep1Uri));
		Assert.assertNotNull("episode data should not be null", ep);
		String epfile = ep.getFilename(context);
		FileWriter fw = new FileWriter(epfile);
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();

		String badfile = String.format("%sbad.mp3", EpisodeCursor.getPodcastStoragePath(context));
		fw = new FileWriter(badfile);
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();

		EpisodeDownloadService.verifyDownloadedFiles(context);

		Assert.assertTrue("episode file should exist", new File(epfile).exists());
		Assert.assertTrue("bad file should not exist", !new File(badfile).exists());
	}
}
