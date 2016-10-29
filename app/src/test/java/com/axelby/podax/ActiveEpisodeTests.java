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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.FileWriter;
import java.io.IOException;

import rx.observers.TestSubscriber;

@RunWith(RobolectricTestRunner.class)
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
			10, PodaxDB.episodes.getActiveEpisodeId());

		prefs.edit().remove("active").apply();
		Assert.assertEquals("active episode id should be first downloaded episode",
			ep2Id, PodaxDB.episodes.getActiveEpisodeId());

		PodaxDB.episodes.delete(ep2Id);
		Assert.assertEquals("active episode id should be not be undownloaded episode",
			-1, PodaxDB.episodes.getActiveEpisodeId());
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

	@Test
	public void activeNotifier() {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1.mp3")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, epId);

		PodaxDB.episodes.setActiveEpisode(epId);

		TestSubscriber<PlayerStatus> subscriber = new TestSubscriber<>();
		PlayerStatus.watch().subscribe(subscriber);
		subscriber.assertNoErrors();
		subscriber.assertValueCount(1);

		PodaxDB.episodes.updatePlayerPosition(1000);
		subscriber.assertValueCount(2);

		new EpisodeEditor(epId).setLastPosition(2000).commit();
		subscriber.assertValueCount(3);
	}

	@Test
	public void nonPlayerNotifier() {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, subId);

		long epId = EpisodeEditor.fromNew(subId, "test://1.mp3")
			.setTitle("one")
			.setPlaylistPosition(Integer.MAX_VALUE)
			.commit();
		Assert.assertNotEquals("episode id should not be -1", -1, epId);

		PodaxDB.episodes.setActiveEpisode(epId);

		TestSubscriber<PlayerStatus> subscriber = new TestSubscriber<>();
		PlayerStatus.watchNonPlayerUpdates().subscribe(subscriber);
		subscriber.assertNoErrors();
		subscriber.assertValueCount(1);

		PodaxDB.episodes.updatePlayerPosition(1000);
		subscriber.assertValueCount(1);

		new EpisodeEditor(epId).setLastPosition(2000).commit();
		subscriber.assertValueCount(2);
	}
}
