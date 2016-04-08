package com.axelby.podax;

import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.Toolbar;

import com.axelby.podax.ui.MainActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.contrib.NavigationViewActions.navigateTo;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class DummyTest {

	@Rule
	public ActivityTestRule<MainActivity> _activityRule = new ActivityTestRule<>(MainActivity.class);

	@Rule
	public DBCacheRule _dataCacheClearer = new DBCacheRule();

	@Test
	public void testNothing() {
	}

	@Test
	public void switchToSubscriptions() {
		onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
		onView(withId(R.id.navmenu)).perform(navigateTo(R.id.subscriptions));
		onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(is("Subscriptions"))));
	}

	private static Matcher<Object> withToolbarTitle(final Matcher<CharSequence> textMatcher) {
		return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
			@Override public boolean matchesSafely(Toolbar toolbar) {
				return textMatcher.matches(toolbar.getTitle());
			}
			@Override public void describeTo(Description description) {
				description.appendText("with toolbar title: ");
				textMatcher.describeTo(description);
			}
		};
	}
}
