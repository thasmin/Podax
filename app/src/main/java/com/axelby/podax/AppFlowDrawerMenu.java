package com.axelby.podax;

import android.app.Fragment;
import android.support.annotation.MenuRes;
import android.support.v7.view.menu.MenuBuilder;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.axelby.podax.podcastlist.DiscoverFragment;
import com.axelby.podax.podcastlist.NetworksFragment;
import com.axelby.podax.podcastlist.SubscriptionListFragment;
import com.axelby.podax.ui.AboutFragment;
import com.axelby.podax.ui.FinishedEpisodeFragment;
import com.axelby.podax.ui.LatestActivityFragment;
import com.axelby.podax.ui.LogViewerFragment;
import com.axelby.podax.ui.PlaylistFragment;
import com.axelby.podax.ui.PodaxPreferenceFragment;
import com.axelby.podax.ui.StatsFragment;
import com.axelby.podax.ui.WeeklyPlannerFragment;

import java.util.Map;

class AppFlowDrawerMenu {
	private SparseArray<CharSequence> _titles = new SparseArray<>(12);
	private SparseArray<Class<? extends Fragment>> _fragmentClasses = new SparseArray<>(12);
	private Map<Class<? extends Fragment>, Integer> _fragmentClassLookup = new ArrayMap<>(12);

	AppFlowDrawerMenu(PodaxApplication app) {
		_fragmentClasses.put(R.id.playlist, PlaylistFragment.class);
		_fragmentClasses.put(R.id.subscriptions, SubscriptionListFragment.class);
		_fragmentClasses.put(R.id.discover, DiscoverFragment.class);
		_fragmentClasses.put(R.id.networks, NetworksFragment.class);
		_fragmentClasses.put(R.id.latest_activity, LatestActivityFragment.class);
		_fragmentClasses.put(R.id.weekly_planner, WeeklyPlannerFragment.class);
		_fragmentClasses.put(R.id.finished_episodes, FinishedEpisodeFragment.class);
		//addMainMenuActivity(R.id.gpodder, "gpodder-sync", GPodderActivity.class);
		_fragmentClasses.put(R.id.stats, StatsFragment.class);
		_fragmentClasses.put(R.id.preferences, PodaxPreferenceFragment.class);
		_fragmentClasses.put(R.id.about, AboutFragment.class);
		_fragmentClasses.put(R.id.log_viewer, LogViewerFragment.class);

		_fragmentClassLookup.put(PlaylistFragment.class, R.id.playlist);
		_fragmentClassLookup.put(SubscriptionListFragment.class, R.id.subscriptions);
		_fragmentClassLookup.put(DiscoverFragment.class, R.id.discover);
		_fragmentClassLookup.put(NetworksFragment.class, R.id.networks);
		_fragmentClassLookup.put(LatestActivityFragment.class, R.id.latest_activity);
		_fragmentClassLookup.put(WeeklyPlannerFragment.class, R.id.weekly_planner);
		_fragmentClassLookup.put(FinishedEpisodeFragment.class, R.id.finished_episodes);
		//addMainMenuActivity(R.id.gpodder, "gpodder-sync", GPodderActivity.class);
		_fragmentClassLookup.put(StatsFragment.class, R.id.stats);
		_fragmentClassLookup.put(PodaxPreferenceFragment.class, R.id.preferences);
		_fragmentClassLookup.put(AboutFragment.class, R.id.about);
		_fragmentClassLookup.put(LogViewerFragment.class, R.id.log_viewer);

		MenuInflater menuInflater = new MenuInflater(app);
		MenuBuilder menu = new MenuBuilder(app);
		menuInflater.inflate(R.menu.app, menu);
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			_titles.put(item.getItemId(), item.getTitle());
			//int groupId = item.getGroupId();
		}
	}

	boolean contains(@MenuRes int menuId) {
		return _titles.get(menuId) != null && _fragmentClasses.get(menuId) != null;
	}

	AppFlow.ScreenChange getScreenChange(int id) {
		return AppFlow.ScreenChange.mainFragment(_titles.get(id), _fragmentClasses.get(id));
	}

	AppFlow.ScreenChange find(Class<? extends Fragment> fragmentClass) {
		return getScreenChange(_fragmentClassLookup.get(fragmentClass));
	}
}
