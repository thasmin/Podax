package com.axelby.podax;

import android.content.Context;

import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.EpisodeEditor;
import com.axelby.podax.model.Episodes;
import com.axelby.podax.model.SubscriptionDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;
import com.axelby.podax.model.Subscriptions;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SubscriptionDataTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testGetSubscription() {
		long subId = SubscriptionEditor.create("test").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		TestSubscriber<SubscriptionData> testSubscriber = new TestSubscriber<>();
		Subscriptions.watch(subId).subscribe(testSubscriber);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("huh?", testSubscriber.getOnNextEvents().get(0).getTitle());

		new SubscriptionEditor(subId).setTitleOverride("nevermind").commit();

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("nevermind", testSubscriber.getOnNextEvents().get(1).getTitle());
	}

	@Test
	public void testGetAll() {
		long sub1Id = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, sub1Id);

		long sub2Id = SubscriptionEditor.create("test://2").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, sub2Id);

		TestSubscriber<SubscriptionData> testSubscriber = new TestSubscriber<>();
		Subscriptions.getAll().subscribe(testSubscriber);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);

		List<SubscriptionData> subs = testSubscriber.getOnNextEvents();
		Assert.assertTrue("first sub not found", subs.get(0).getId() == sub1Id || subs.get(1).getId() == sub1Id);
		Assert.assertTrue("second sub not found", subs.get(0).getId() == sub2Id || subs.get(1).getId() == sub2Id);

		new SubscriptionEditor(sub1Id).setTitleOverride("nevermind").commit();

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
	}

	@Test
	public void testGetFor() {
		long sub1Id = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, sub1Id);

		long sub2Id = SubscriptionEditor.create("test://2").setRawTitle("huh?").setSingleUse(true).commit();
		Assert.assertNotEquals("subscription uri should not be null", -1, sub2Id);

		TestSubscriber<SubscriptionData> testSubscriber = new TestSubscriber<>();
		Subscriptions.getFor(SubscriptionDB.COLUMN_SINGLE_USE, 1).subscribe(testSubscriber);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should only have second subscription", testSubscriber.getOnNextEvents().get(0).getId(), sub2Id);
	}

	@Test
	public void testDelete() throws IOException {
		Context context = RuntimeEnvironment.application;

		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		long epId = EpisodeEditor.fromNew(context, subId, "test://2").setTitle("ep title").commit();
		Assert.assertNotEquals("episode id should not be -1", -1, epId);

		String thumbFn = SubscriptionCursor.getThumbnailFilename(context, subId);
		new File(thumbFn).mkdirs();
		new File(thumbFn).createNewFile();
		Assert.assertTrue("subscription thumbnail should have been created", new File(thumbFn).exists());

		Subscriptions.delete(subId);

		TestSubscriber<SubscriptionData> subSubscriber = new TestSubscriber<>();
		Subscriptions.getAll().subscribe(subSubscriber);
		subSubscriber.assertValueCount(0);

		TestSubscriber<List<EpisodeData>> epSubscriber = new TestSubscriber<>();
		Episodes.getAll().subscribe(epSubscriber);
		epSubscriber.assertValueCount(1);
		Assert.assertEquals(0, epSubscriber.getOnNextEvents().get(0).size());

		Assert.assertFalse("subscription thumbnail should have been deleted", new File(thumbFn).exists());
	}

}
