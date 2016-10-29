package com.axelby.podax;

import android.content.Context;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class HelperTests {
	@Test
	public void testTimes() {
		Assert.assertEquals("00:00", Helper.getTimeString(0));
		Assert.assertEquals("00:30", Helper.getTimeString(30 * 1000));
		Assert.assertEquals("01:27", Helper.getTimeString(87 * 1000));
		Assert.assertEquals("2:10:34", Helper.getTimeString((2*60*60 + 10*60 + 34) * 1000));
	}

	@Test
	public void testVerboseTimes() {
		Context context = RuntimeEnvironment.application;

		Assert.assertEquals("0 seconds", Helper.getVerboseTimeString(context, 0, false));
		Assert.assertEquals("30 seconds", Helper.getVerboseTimeString(context, 30, false));
		Assert.assertEquals("1 minute", Helper.getVerboseTimeString(context, 87, false));
		Assert.assertEquals("2 hours, 10 minutes", Helper.getVerboseTimeString(context, 2*60*60 + 10*60 + 34, false));
		Assert.assertEquals("1 minute, 27 seconds", Helper.getVerboseTimeString(context, 87, true));
		Assert.assertEquals("2 hours, 1 minute, 34 seconds", Helper.getVerboseTimeString(context, 2*60*60 + 60 + 34, true));
	}
}
