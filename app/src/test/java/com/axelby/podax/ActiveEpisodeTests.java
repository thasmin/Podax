package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.axelby.podax.model.EpisodeData;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.FileWriter;
import java.io.IOException;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ActiveEpisodeTests {
	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void activeEpisodePriority() throws IOException {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "one");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://1");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		values.put(EpisodeProvider.COLUMN_FILE_SIZE, 5);
		Uri ep1Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep1Uri);

		values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "two");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test://2");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
		values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, Integer.MAX_VALUE);
		values.put(EpisodeProvider.COLUMN_FILE_SIZE, 5);
		Uri ep2Uri = context.getContentResolver().insert(EpisodeProvider.URI, values);
		Assert.assertNotNull("episode uri should not be null", ep2Uri);

		EpisodeData ep = EpisodeData.create(context, ContentUris.parseId(ep2Uri));
		Assert.assertNotNull("episode data should not be null", ep);
		String epfile = ep.getFilename(context);
		FileWriter fw = new FileWriter(epfile);
		fw.write(new char[] { 32, 32, 32, 32, 32 });
		fw.close();

		SharedPreferences prefs = context.getSharedPreferences("internals", Context.MODE_PRIVATE);
		prefs.edit().putLong("active", 10).apply();
		Assert.assertEquals("active episode id should be taken from prefs",
			10, EpisodeProvider.getActiveEpisodeId(context));

		prefs.edit().remove("active").apply();
		Assert.assertEquals("active episode id should be first downloaded episode",
			ContentUris.parseId(ep2Uri), EpisodeProvider.getActiveEpisodeId(context));

		context.getContentResolver().delete(ep2Uri, null, null);
		Assert.assertEquals("active episode id should be not be undownloaded episode",
			-1, EpisodeProvider.getActiveEpisodeId(context));
	}
}
