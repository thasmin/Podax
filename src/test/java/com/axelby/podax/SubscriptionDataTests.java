package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SubscriptionDataTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Test
	public void testGetSubscription() {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		long subId = ContentUris.parseId(subUri);
		TestSubscriber<SubscriptionData> testSubscriber = new TestSubscriber<>();
		SubscriptionData.getObservable(context, subId).subscribe(testSubscriber);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(1);
		Assert.assertEquals("huh?", testSubscriber.getOnNextEvents().get(0).getTitle());

		values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE_OVERRIDE, "nevermind");
		context.getContentResolver().update(subUri, values, null, null);

		testSubscriber.assertNoErrors();
		testSubscriber.assertValueCount(2);
		Assert.assertEquals("nevermind", testSubscriber.getOnNextEvents().get(1).getTitle());
	}

}
