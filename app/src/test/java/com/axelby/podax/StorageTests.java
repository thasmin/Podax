package com.axelby.podax;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

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
		ContentResolver resolver = context.getContentResolver();

		long subId = SubscriptionEditor.create("test").setRawTitle("Test Subscription").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		ContentValues values = new ContentValues();
		values.put(EpisodeProvider.COLUMN_TITLE, "Test Episode");
		values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test.mp3");
		values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, subId);
		Uri epUri = resolver.insert(EpisodeProvider.URI, values);

		long id = ContentUris.parseId(epUri);
		EpisodeCursor ep = EpisodeCursor.getCursor(context, id);
		if (ep == null)
			throw new Exception("couldn't load episode cursor");

		String filename = ep.getFilename(context);
		try {
			boolean created = new File(filename).createNewFile();
			if (!created)
				throw new Exception("unable to create filename");
			resolver.delete(ep.getContentUri(), null, null);
			Assert.assertEquals(false, new File(filename).exists());
		} finally {
			if (filename != null)
				new File(filename).delete();
		}
	}
}

