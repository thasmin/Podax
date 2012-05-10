package com.axelby.podax.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.axelby.podax.R;
import com.axelby.podax.UpdateService;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends SherlockFragmentActivity {
	protected int _focusedPage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		TabsAdapter tabsAdapter = new TabsAdapter(getSupportFragmentManager());
		pager.setAdapter(tabsAdapter);

		TitlePageIndicator titleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
		titleIndicator.setViewPager(pager);
		titleIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				_focusedPage = position;
				invalidateOptionsMenu();
			}
		});

        final ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    switch (_focusedPage) {
	    case 1:
	    	inflater.inflate(R.menu.subscriptionlist_activity, menu);
	    	return true;
	    case 2:
	    	inflater.inflate(R.menu.downloadlist_activity, menu);
	    	return true;
	    }
	    return false;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.add_subscription) {
			startActivity(new Intent(this, AddSubscriptionActivity.class));
			return true;
		} else if (item.getItemId() == R.id.discover) {
			startActivity(new Intent(this, DiscoverActivity.class));
			return true;
		} else if (item.getItemId() == R.id.restart) {
			UpdateService.downloadPodcasts(this);
			return true;
		} else if (item.getItemId() == R.id.preferences) {
			startActivity(new Intent(this, Preferences.class));
			return true;
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
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
			return new String[] { "Queue", "Subscriptions", "Downloads", "About" }[position].toUpperCase();
		}

		@Override
		public int getCount() {
			return 4;
		}
	}
}