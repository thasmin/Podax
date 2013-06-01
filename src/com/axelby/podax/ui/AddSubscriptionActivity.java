package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.axelby.podax.R;

public class AddSubscriptionActivity extends PodaxFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discover);

		final ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.addTab(bar.newTab().setText("Add").setTabListener(new ActionBar.TabListener() {
			AddSubscriptionFragment _fragment;

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				ft.remove(_fragment);
			}

			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				if (_fragment == null)
					_fragment = new AddSubscriptionFragment();
				ft.add(R.id.tab_content, _fragment, "add");
			}

			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
			}
		}));
		bar.addTab(bar.newTab().setText("iTunes").setTabListener(new ActionBar.TabListener() {
			PopularSubscriptionListFragment _fragment;

			@Override
			public void onTabReselected(Tab tab, FragmentTransaction transaction) {
			}

			@Override
			public void onTabSelected(Tab tab, FragmentTransaction transaction) {
				if (_fragment == null)
					_fragment = new PopularSubscriptionListFragment("iTunes", "http://podax.axelby.com/popularitunes.php");
				transaction.add(R.id.tab_content, _fragment, "iTunes");
			}

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
				transaction.remove(_fragment);
			}
		}));

		if (savedInstanceState != null) {
			bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		}
	}
}
