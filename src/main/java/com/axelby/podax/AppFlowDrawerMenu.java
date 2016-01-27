package com.axelby.podax;

import android.app.Fragment;
import android.support.annotation.MenuRes;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.axelby.podax.ui.AboutFragment;
import com.axelby.podax.ui.DiscoverFragment;
import com.axelby.podax.ui.FinishedEpisodeFragment;
import com.axelby.podax.ui.LatestActivityFragment;
import com.axelby.podax.ui.LogViewerFragment;
import com.axelby.podax.ui.PlaylistFragment;
import com.axelby.podax.ui.PodaxPreferenceFragment;
import com.axelby.podax.ui.StatsFragment;
import com.axelby.podax.ui.SubscriptionListFragment;
import com.axelby.podax.ui.WeeklyPlannerFragment;

import java.util.HashMap;
import java.util.Map;

public class AppFlowDrawerMenu {
	private Map<Integer, AppFlow.ScreenChange> _mainMenuMap = new HashMap<>(12);

	private void addMainMenuFragment(int id, String description, Class<? extends Fragment> fragmentClass) {
		AppFlow.ScreenChange sc = new AppFlow.ScreenChangeBuilder(id, description, AppFlow.Frame.MainFragment).fragment(fragmentClass).build();
		_mainMenuMap.put(id, sc);
	}

	public AppFlowDrawerMenu(PodaxApplication app) {
		addMainMenuFragment(R.id.playlist, "playlist", PlaylistFragment.class);
		addMainMenuFragment(R.id.subscriptions, "subscriptions", SubscriptionListFragment.class);
		addMainMenuFragment(R.id.discover, "discover", DiscoverFragment.class);
		addMainMenuFragment(R.id.latest_activity, "latest-activity", LatestActivityFragment.class);
		addMainMenuFragment(R.id.weekly_planner, "weekly-planner", WeeklyPlannerFragment.class);
		addMainMenuFragment(R.id.finished_episodes, "finished-episodes", FinishedEpisodeFragment.class);
		//addMainMenuActivity(R.id.gpodder, "gpodder-sync", GPodderActivity.class);
		addMainMenuFragment(R.id.stats, "stats", StatsFragment.class);
		addMainMenuFragment(R.id.preferences, "preferences", PodaxPreferenceFragment.class);
		addMainMenuFragment(R.id.about, "about", AboutFragment.class);
		addMainMenuFragment(R.id.log_viewer, "debug", LogViewerFragment.class);

		MenuInflater menuInflater = new MenuInflater(app);
		MenuBuilder menu = new MenuBuilder(app);
		menuInflater.inflate(R.menu.app, menu);
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if (!_mainMenuMap.containsKey(item.getItemId())) {
				Log.w("AppFlowDrawerMenu", String.format("potentially unhandled menu item: %1$s (%2$d)", item.getTitle(), item.getItemId()));
				continue;
			}
			_mainMenuMap.get(item.getItemId()).setTitle(item.getTitle());
			//int groupId = item.getGroupId();
		}
	}

	public boolean contains(@MenuRes int menuId) {
		return _mainMenuMap.containsKey(menuId);
	}

	public AppFlow.ScreenChange get(@MenuRes int menuId) {
		return _mainMenuMap.get(menuId);
	}

}
