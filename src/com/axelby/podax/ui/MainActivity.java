package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.axelby.podax.R;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		TabsAdapter tabsAdapter = new TabsAdapter(getSupportFragmentManager());
		pager.setAdapter(tabsAdapter);

		TitlePageIndicator titleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
		titleIndicator.setViewPager(pager);

        final ActionBar bar = getSupportActionBar();
        /*
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
		*/

		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}

	public static class TabsAdapter extends FragmentStatePagerAdapter
		implements TitleProvider
	{

		public TabsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int item) {
			switch (item) {
			case 0:
				return new QueueFragment();
			case 1:
				return new SubscriptionListFragment();
			case 2:
				return new ActiveDownloadListFragment();
			case 3:
				return new AboutFragment();
			}
			throw new IllegalArgumentException();
		}

		@Override
		public String getTitle(int position) {
			return new String[] { "Queue", "Subscriptions", "Downloads", "About" }[position];
		}

		@Override
		public int getCount() {
			return 4;
		}
	}
}