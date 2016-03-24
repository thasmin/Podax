package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.axelby.podax.model.EpisodeData;

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
		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		// put one episode in queue
		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
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
