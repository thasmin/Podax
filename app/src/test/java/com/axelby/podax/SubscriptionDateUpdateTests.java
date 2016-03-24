package com.axelby.podax;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class SubscriptionDateUpdateTests {

	@Rule
	public RxSchedulerSwitcher _rxSchedulerSwitcher = new RxSchedulerSwitcher();

	@Rule
	public DataCacheClearer _dataCacheClearer = new DataCacheClearer();

	@Test
	public void testUpdateTitle() {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		long subId = ContentUris.parseId(subUri);
		SubscriptionData sub = SubscriptionData.create(context, subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals("huh?", sub.getTitle());

		new SubscriptionEditor(context, subId)
			.setTitleOverride("oh i see")
			.commit();

		sub = SubscriptionData.create(context, subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals("oh i see", sub.getTitle());
	}

	@Test
	public void testUpdateExpiration() {
		Context context = RuntimeEnvironment.application;

		ContentValues values = new ContentValues();
		values.put(SubscriptionProvider.COLUMN_TITLE, "huh?");
		values.put(SubscriptionProvider.COLUMN_URL, "test://1");
		Uri subUri = context.getContentResolver().insert(SubscriptionProvider.URI, values);
		Assert.assertNotNull("subscription uri should not be null", subUri);

		long subId = ContentUris.parseId(subUri);
		SubscriptionData sub = SubscriptionData.create(context, subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals("huh?", sub.getTitle());

		new SubscriptionEditor(context, subId).setExpirationDays(0).commit();
		sub = SubscriptionData.create(context, subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals((Integer) 0, sub.getExpirationDays());

		new SubscriptionEditor(context, subId).setExpirationDays(null).commit();
		sub = SubscriptionData.create(context, subId);
		Assert.assertNotNull(sub);
		Assert.assertNull(sub.getExpirationDays());
	}
}
