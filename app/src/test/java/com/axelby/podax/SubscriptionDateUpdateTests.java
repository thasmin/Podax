package com.axelby.podax;

import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
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
		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		SubscriptionData sub = SubscriptionData.create(subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals("huh?", sub.getTitle());

		new SubscriptionEditor(subId).setTitleOverride("oh i see").commit();

		sub = SubscriptionData.create(subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals("oh i see", sub.getTitle());
	}

	@Test
	public void testUpdateExpiration() {
		long subId = SubscriptionEditor.create("test://1").setRawTitle("huh?").commit();
		Assert.assertNotEquals("subscription id should not be -1", -1, subId);

		SubscriptionData sub = SubscriptionData.create(subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals("huh?", sub.getTitle());

		new SubscriptionEditor(subId).setExpirationDays(0).commit();
		sub = SubscriptionData.create(subId);
		Assert.assertNotNull(sub);
		Assert.assertEquals((Integer) 0, sub.getExpirationDays());

		new SubscriptionEditor(subId).setExpirationDays(null).commit();
		sub = SubscriptionData.create(subId);
		Assert.assertNotNull(sub);
		Assert.assertNull(sub.getExpirationDays());
	}
}
