package com.axelby.podax;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.axelby.podax.ui.AboutFragment;
import com.axelby.podax.ui.DiscoverFragment;
import com.axelby.podax.ui.EpisodeDetailFragment;
import com.axelby.podax.ui.FinishedEpisodeFragment;
import com.axelby.podax.ui.LatestActivityFragment;
import com.axelby.podax.ui.LogViewerFragment;
import com.axelby.podax.ui.MainActivity;
import com.axelby.podax.ui.PlaylistFragment;
import com.axelby.podax.ui.PodaxFragmentActivity;
import com.axelby.podax.ui.PodaxPreferenceFragment;
import com.axelby.podax.ui.StatsFragment;
import com.axelby.podax.ui.SubscriptionListFragment;
import com.axelby.podax.ui.WeeklyPlannerFragment;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class AppFlow {
	// TODO: move static fields to another class

	private static boolean _appSet = false;
	private static WeakReference<MainActivity> _mainActivity;
	private static Map<Integer, ScreenChange> _mainMenuMap = new HashMap<>(12);

	private static void addMainMenuFragment(int id, String description, Class<? extends Fragment> fragmentClass) {
		ScreenChange sc = new ScreenChangeBuilder(id, description, Frame.MainFragment).fragment(fragmentClass).build();
		_mainMenuMap.put(id, sc);
	}

	static {
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

	}

	public static void setApplication(PodaxApplication app) {
		if (_appSet)
			return;
		app.registerActivityLifecycleCallbacks(new LifecycleCallbackHandler());

		MenuInflater menuInflater = new MenuInflater(app);
		MenuBuilder menu = new MenuBuilder(app);
		menuInflater.inflate(R.menu.app, menu);
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if (!_mainMenuMap.containsKey(item.getItemId())) {
				Log.w("AppFlow", String.format("potentially unhandled menu item: %1$s (%2$d)", item.getTitle(), item.getItemId()));
				continue;
			}
			_mainMenuMap.get(item.getItemId()).setTitle(item.getTitle());
			//int groupId = item.getGroupId();
		}

		_appSet = true;
	}

	public static AppFlow get(Context context) {
		return new AppFlow(context);
	}

	private final Context _context;

	// 3 options: open in main fragment, open in detail fragment, or open new activity
	enum Frame {
		MainFragment,
		DetailFragment,
		Activity
	}

	static class ScreenChangeBuilder {
		private int _id;
		private String _description;
		private Class<? extends Fragment> _fragmentClass = null;
		private Class<? extends Activity> _activityClass = null;
		private Frame _destination;

		public ScreenChangeBuilder(int id, String description, Frame destination) {
			this._id = id;
			this._description = description;
			this._destination = destination;
		}

		public ScreenChangeBuilder fragment(Class<? extends Fragment> fragmentClass) {
			_fragmentClass = fragmentClass;
			return this;
		}

		public ScreenChangeBuilder activity(Class<? extends Activity> activityClass) {
			_activityClass = activityClass;
			return this;
		}

		public ScreenChange build() {
			return new ScreenChange(_id, _description, _destination, _fragmentClass, _activityClass);
		}

	}

	static class ScreenChange {
		private int _id;
		private String _description;
		private Class<? extends Fragment> _fragmentClass = null;
		private Class<? extends Activity> _activityClass = null;
		private Frame _destination;
		private CharSequence _title;

		ScreenChange(int id, String description, Frame destination,
					 Class<? extends Fragment> fragmentClass,
					 Class<? extends Activity> activityClass) {
			_id = id;
			_description = description;
			_destination = destination;
			_fragmentClass = fragmentClass;
			_activityClass = activityClass;
		}

		public int getId() {
			return _id;
		}

		public String getDescription() {
			return _description;
		}

		public Frame getDestination() {
			return _destination;
		}

		public Class<? extends Fragment> getFragmentClass() {
			return _fragmentClass;
		}

		public Class<? extends Activity> getActivityClass() {
			return _activityClass;
		}

		void setTitle(CharSequence title) {
			_title = title;
		}

		public CharSequence getTitle() {
			return _title;
		}
	}

	public AppFlow(Context context) {
		_context = context;

		String[] toolbar = new String[]{
			"search"
		};

		String[] mainmenu = new String[]{
			"playlist",
			"subscriptions",
			"discovery",

			"latest-activity",
			"weekly-planner",
			"finished-episodes",

			"gpodder sync",

			"stats",

			"preferences",
			"about",

			"debug"
		};

		String[] subscreens = new String[]{
			"episode-detail",
			"subscription-detail",
			"add-subscription-rss-url",
		};

	}

	public boolean onMainMenuItem(@MenuRes int itemId) {
		if (!_mainMenuMap.containsKey(itemId)) {
			Log.e("AppFlow", "unhandled menu item: " + itemId);
			return false;
		}
		ScreenChange change = _mainMenuMap.get(itemId);

		MainActivity mainActivity = _mainActivity.get();
		if (mainActivity == null)
			return false;

		Class<? extends Fragment> fragmentClass = change.getFragmentClass();
		if (fragmentClass == null)
			return false;

		mainActivity.showMainFragment(change.getTitle(), Fragment.instantiate(mainActivity, fragmentClass.getName()));
		return true;
	}

	// TODO: determine whether to show it in an activity or detail frame
	// TODO: create a screen change action for this?
	public boolean onRequestViewReleaseNotes() {
		startActivityFragment(AboutFragment.class);
		return true;
	}

	public boolean displayActiveEpisode() {
		startActivityFragment(EpisodeDetailFragment.class);
		return true;
	}

	private void startActivityFragment(Class<? extends Fragment> fragmentClass) {
		_context.startActivity(PodaxFragmentActivity.createIntent(_context, fragmentClass, null));
	}

	private static class LifecycleCallbackHandler implements Application.ActivityLifecycleCallbacks {
		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
			if (activity instanceof MainActivity)
				_mainActivity = new WeakReference<>((MainActivity) activity);
		}

		@Override public void onActivityStarted(Activity activity) { }
		@Override public void onActivityResumed(Activity activity) { }
		@Override public void onActivityPaused(Activity activity) { }
		@Override public void onActivityStopped(Activity activity) { }
		@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
		@Override public void onActivityDestroyed(Activity activity) { }
	}
}
