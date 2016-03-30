package com.axelby.podax;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.axelby.podax.model.DBAdapter;
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

		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, subId);

		long ep1Id = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.setFileSize(5)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, ep1Id);

		long ep2Id = EpisodeEditor.fromNew(subId, "test://2")
			.setTitle("two")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.setFileSize(5)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, ep2Id);

		EpisodeData ep = EpisodeData.create(ep2Id);
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
			ep2Id, EpisodeProvider.getActiveEpisodeId(context));

		PodaxDB.episodes.delete(ep2Id);
		Assert.assertEquals("active episode id should be not be undownloaded episode",
			-1, EpisodeProvider.getActiveEpisodeId(context));
	}

	@Test
	public void monotonicQueue() {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, subId);

		long ep1Id = EpisodeEditor.fromNew(subId, "test://1")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, ep1Id);

		long ep2Id = EpisodeEditor.fromNew(subId, "test://2")
			.setTitle("two")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, ep2Id);

		long ep3Id = EpisodeEditor.fromNew(subId, "test://3")
			.setTitle("two")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, ep3Id);

		DBAdapter dbAdapter = new DBAdapter(context);
		String sql = "SELECT queuePosition FROM podcasts ORDER BY queuePosition";

		Cursor c = dbAdapter.getReadableDatabase().rawQuery(sql, null);
		Assert.assertNotNull(c);
		Assert.assertTrue(c.moveToNext());
		Assert.assertEquals("first item in queue", 0, c.getInt(0));
		Assert.assertTrue(c.moveToNext());
		Assert.assertEquals("second item in queue", 1, c.getInt(0));
		Assert.assertTrue(c.moveToNext());
		Assert.assertEquals("third item in queue", 2, c.getInt(0));
		Assert.assertFalse(c.moveToNext());
		c.close();

		PodaxDB.episodes.delete(ep2Id);

		c = dbAdapter.getReadableDatabase().rawQuery(sql, null);
		Assert.assertNotNull(c);
		Assert.assertTrue(c.moveToNext());
		Assert.assertEquals("first item in queue", 0, c.getInt(0));
		Assert.assertTrue(c.moveToNext());
		Assert.assertEquals("second item in queue", 1, c.getInt(0));
		Assert.assertFalse(c.moveToNext());
		c.close();
	}
}
