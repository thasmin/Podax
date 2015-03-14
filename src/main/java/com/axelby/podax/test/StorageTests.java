package com.axelby.podax.test;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.SubscriptionProvider;

import java.io.File;

public class StorageTests extends AndroidTestCase {
	public void testDeletePodcast() throws Exception {
		String filename = null;
		try {
			Context context = getContext();
			ContentResolver resolver = context.getContentResolver();
			context.deleteDatabase("podax.db");

			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_TITLE, "Test Subscription");
			values.put(SubscriptionProvider.COLUMN_URL, "test");
			Uri subUri = resolver.insert(SubscriptionProvider.URI, values);

			values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_TITLE, "Test Episode");
			values.put(EpisodeProvider.COLUMN_MEDIA_URL, "test.mp3");
			values.put(EpisodeProvider.COLUMN_SUBSCRIPTION_ID, ContentUris.parseId(subUri));
			Uri epUri = resolver.insert(EpisodeProvider.URI, values);

			long id = ContentUris.parseId(epUri);
			EpisodeCursor ep = EpisodeCursor.getCursor(context, id);
			if (ep == null)
				throw new Exception("couldn't load episode cursor");
			filename = ep.getFilename(context);
			boolean created = new File(filename).createNewFile();
			if (!created)
				throw new Exception("unable to create filename");
			resolver.delete(ep.getContentUri(), null, null);
			assertEquals(false, new File(filename).exists());
		} finally {
			if (filename != null)
				new File(filename).delete();
		}
	}
}
