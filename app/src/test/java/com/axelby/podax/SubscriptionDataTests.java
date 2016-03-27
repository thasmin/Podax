package com.axelby.podax;

import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;
import com.axelby.podax.model.Subscriptions;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

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
		Subscriptions.getFor(SubscriptionProvider.COLUMN_SINGLE_USE, 1).subscribe(testSubscriber);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("should only have second subscription", testSubscriber.getOnNextEvents().get(0).getId(), sub2Id);
	}

}
