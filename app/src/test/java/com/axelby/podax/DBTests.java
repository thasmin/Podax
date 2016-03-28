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
public class DBTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testSubscriptionSearch() throws Exception {
		SubscriptionEditor.create("test://1").setRawTitle("Good Subscription").commit();
		SubscriptionEditor.create("test://2").setRawTitle("Bad Subscription").commit();

		TestSubscriber<List<SubscriptionData>> testSubscriber = new TestSubscriber<>();
		Subscriptions.search("good").subscribe(testSubscriber);
		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("search results", 1, testSubscriber.getOnNextEvents().get(0).size());
	}

}

