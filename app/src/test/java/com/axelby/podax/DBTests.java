package com.axelby.podax;

import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DBTests {

	@Test
	public void testSubscriptionSearch() throws Exception {
		SubscriptionEditor.create("test://1").setRawTitle("Good Subscription").commit();
		SubscriptionEditor.create("test://2").setRawTitle("Bad Subscription").commit();

		List<SubscriptionData> subs = PodaxDB.subscriptions.search("good");
		Assert.assertEquals("search results", 1, subs.size());
	}

}

