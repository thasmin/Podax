package com.axelby.podax;

import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import rx.Observable;
import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DBTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testSubscriptionSearch() throws Exception {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("Good Subscription").commit();
		SubscriptionEditor.create("test://2").setRawTitle("Bad Subscription").commit();

		TestSubscriber<SubscriptionData> testSubscriber = new TestSubscriber<>();
		Observable.from(PodaxDB.subscriptions.search("good")).subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("search results", subId, testSubscriber.getOnNextEvents().get(0).getId());
	}

}

