package com.axelby.podax;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import com.axelby.podax.ui.AboutFragment;
import com.axelby.podax.ui.EpisodeDetailFragment;
import com.axelby.podax.ui.BareFragmentActivity;
import com.axelby.podax.ui.EpisodeListFragment;
import com.axelby.podax.ui.LatestActivityFragment;
import com.axelby.podax.ui.MainActivity;
import com.axelby.podax.ui.PodaxFragmentActivity;
import com.axelby.podax.ui.SubscriptionSettingsFragment;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

// TODO: fix detail fragment handling
public class AppFlow {
	private static boolean _appSet = false;
	private static AppFlowDrawerMenu _drawerMenu = null;

	public static void setApplication(PodaxApplication app) {
		if (_appSet)
			return;
		app.registerActivityLifecycleCallbacks(new LifecycleCallbackHandler());
		_drawerMenu = new AppFlowDrawerMenu(app);

		_appSet = true;
	}

	private static Deque<ScreenChange> _backstack = new ArrayDeque<>(10);
	private static Activity _currentActivity = null;
	private static WeakReference<MainActivity> _mainActivity = new WeakReference<>(null);
	private static boolean _hasDetailFragment = true;

	static {
		// initial backstack should have mainactivity in it
		_backstack.add(ScreenChange.activity(MainActivity.class));
	}

	public static AppFlow get(Activity activity) {
		return new AppFlow(activity);
	}

	// be sure to use an activity when using this method
	public static AppFlow get(Context context) {
		if (!(context instanceof Activity))
			Log.e("AppFlow", "creating with a Context that is not an Activity");
		return new AppFlow((Activity) context);
	}

	private final Activity _activity;

	public enum Frame {
		MainFragment,
		DetailFragment,
		FragmentActivity,
		Activity
	}

	public static class ScreenChange {
		private Class<? extends Fragment> _fragmentClass = null;
		private Class<? extends Activity> _activityClass = null;
		private Frame _destination;
		private CharSequence _title;
		private Bundle _args;
		private Bundle _options;

		private ScreenChange() { }

		@Override
		public String toString() {
			switch (_destination) {
				case Activity: return "activity " + _activityClass.getSimpleName();
				case FragmentActivity: return "fragment activity " + _fragmentClass.getSimpleName();
				case MainFragment: return _fragmentClass.getSimpleName() + " into main fragment";
				case DetailFragment: return _fragmentClass.getSimpleName() + " into detail fragment";
				default: return "unknown";
			}
		}

		public static ScreenChange activity(Class<? extends Activity> activityClass) {
			return ScreenChange.activity(activityClass, null);
		}

		public static ScreenChange activity(Class<? extends Activity> activityClass, Bundle args) {
			return ScreenChange.activity(activityClass, args, null);
		}

		public static ScreenChange activity(Class<? extends Activity> activityClass, Bundle args, Bundle options) {
			ScreenChange sc = new ScreenChange();
			sc._destination = Frame.Activity;
			sc._activityClass = activityClass;
			sc._args = args;
			sc._options = options;
			return sc;
		}

		public static ScreenChange mainFragment(CharSequence title, Class<? extends Fragment> fragmentClass) {
			ScreenChange sc = new ScreenChange();
			sc._destination = Frame.MainFragment;
			sc._fragmentClass = fragmentClass;
			sc._title = title;
			return sc;
		}

		public static ScreenChange detailFragment(Class<? extends Fragment> fragmentClass, Bundle args) {
			return ScreenChange.detailFragment(fragmentClass, args, null);
		}

		public static ScreenChange detailFragment(Class<? extends Fragment> fragmentClass, Bundle args, Bundle options) {
			ScreenChange sc = new ScreenChange();
			sc._destination = Frame.DetailFragment;
			sc._fragmentClass = fragmentClass;
			sc._args = args;
			sc._options = options;
			return sc;
		}

		public static ScreenChange fragmentActivity(Class<? extends Fragment> fragmentClass) {
			return ScreenChange.fragmentActivity(fragmentClass, null);
		}

		public static ScreenChange fragmentActivity(Class<? extends Fragment> fragmentClass, Bundle args) {
			return ScreenChange.fragmentActivity(fragmentClass, args, null);
		}

		public static ScreenChange fragmentActivity(Class<? extends Fragment> fragmentClass, Bundle args, Bundle options) {
			ScreenChange sc = new ScreenChange();
			sc._destination = Frame.FragmentActivity;
			sc._fragmentClass = fragmentClass;
			sc._args = args;
			sc._options = options;
			return sc;
		}

		public boolean openedActivity() {
			return _destination == Frame.FragmentActivity ||
				_destination == Frame.Activity ||
				(_destination == Frame.DetailFragment && !_hasDetailFragment);
		}

		public boolean apply(Activity activity) {
			if (this._destination == Frame.Activity) {
				Intent intent = new Intent(activity, _activityClass);
				intent.replaceExtras(_args);
				activity.startActivity(intent, _options);
				return true;
			}

			if (this._destination == Frame.FragmentActivity) {
				Intent intent = new Intent(activity, PodaxFragmentActivity.class);
				intent.putExtra(Constants.EXTRA_FRAGMENT_CLASSNAME, _fragmentClass.getCanonicalName());
				intent.putExtra(Constants.EXTRA_ARGS, _args);
				activity.startActivity(intent, _options);
				return true;
			}

			Class<? extends Fragment> fragmentClass = this._fragmentClass;
			if (fragmentClass == null) {
				Log.e("AppFlow", "fragment class not set in main fragment screen change");
				return false;
			}

			if (this._destination == Frame.MainFragment) {
				MainActivity mainActivity = _mainActivity.get();
				if (mainActivity == null) {
					// TODO: if this case is possible, start the main activity
					Log.e("AppFlow", "Main activity isn't present");
					return false;
				}

				if (_title == null) {
					Log.e("AppFlow", "title not set in main fragment screen change");
					return false;
				}

				Fragment fragment = Fragment.instantiate(activity, fragmentClass.getName(), _args);
				mainActivity.showMainFragment(this._title, fragment);
				return true;
			}

			if (this._destination == Frame.DetailFragment) {
				MainActivity mainActivity = _mainActivity.get();
				if (mainActivity != null && _hasDetailFragment) {
					Fragment fragment = Fragment.instantiate(activity, fragmentClass.getName(), _args);
					mainActivity.showDetailFragment(fragment);
				} else {
					Intent intent = new Intent(activity, PodaxFragmentActivity.class);
					intent.putExtra(Constants.EXTRA_FRAGMENT_CLASSNAME, fragmentClass.getCanonicalName());
					intent.putExtra(Constants.EXTRA_ARGS, _args);
					activity.startActivity(intent, _options);
				}

				return true;
			}

			return false;
		}

		public boolean restore(Activity activity) {
			// restore activities by restarting them
			if (_destination == Frame.Activity || _destination == Frame.FragmentActivity)
				return apply(activity);

			// changing fragment must be done on main activity
			if (_mainActivity == null || _mainActivity.get() == null)
				return false;
			MainActivity mainActivity = _mainActivity.get();

			Fragment fragment = Fragment.instantiate(activity, _fragmentClass.getName());

			if (_destination == Frame.MainFragment) {
				mainActivity.showMainFragment(_title, fragment);
				return true;
			}

			if (_destination == Frame.DetailFragment) {
				mainActivity.showDetailFragment(fragment);
				return true;
			}

			return false;
		}
	}

	private void switchTo(ScreenChange sc) {
		Log.d("AppFlow", "switching to " + sc);
		if (sc.apply(_activity))
			_backstack.addFirst(sc);
	}

	public void goBack() {
		// first is starting mainactivity, then changing fragment to playlist
		if (_backstack.size() == 2)
			return;

		ScreenChange ending = _backstack.removeFirst();
		Log.d("AppFlow", "popping " + ending);

		// finishing an activity restores the previous state
		if (ending.openedActivity()) {
			ActivityCompat.finishAfterTransition(_currentActivity);
			return;
		}

		_backstack.peekFirst().restore(_activity);
		Log.d("AppFlow", "restoring " + _backstack.peekFirst());
	}

	public AppFlow(Activity activity) {
		_activity = activity;
	}

	public boolean onMainMenuItem(@MenuRes int itemId) {
		if (!_drawerMenu.contains(itemId)) {
			Log.e("AppFlow", "unhandled menu item: " + itemId);
			return false;
		}
		switchTo(_drawerMenu.getScreenChange(itemId));
		return true;
	}

	public boolean onRequestViewReleaseNotes() {
		startActivityFragment(AboutFragment.class);
		return true;
	}

	public boolean displayActiveEpisode() {
		startActivityFragment(EpisodeDetailFragment.class);
		return true;
	}

	public boolean displayLatestActivity() {
		if (_mainActivity != null && _mainActivity.get() == _currentActivity)
			switchTo(_drawerMenu.find(LatestActivityFragment.class));
		else
			startActivityFragment(LatestActivityFragment.class);
		return true;
	}

	public boolean displayEpisode(long episodeId) {
		Bundle args = new Bundle(1);
		args.putLong(Constants.EXTRA_EPISODE_ID, episodeId);
		switchTo(ScreenChange.detailFragment(EpisodeDetailFragment.class, args));
		return true;
	}

	public boolean displayEpisode(long episodeId, View... transitionViews) {
		Bundle args = new Bundle(1);
		args.putLong(Constants.EXTRA_EPISODE_ID, episodeId);
		Bundle options = getTransitionOptions(transitionViews);
		switchTo(ScreenChange.detailFragment(EpisodeDetailFragment.class, args, options));
		return true;
	}

	public boolean displayPodcastViaRSSUrl(String title, String rssUrl, View... transitions) {
		Bundle args = new Bundle(1);
		args.putString(Constants.EXTRA_RSSURL, rssUrl);
		args.putString(Constants.EXTRA_SUBSCRIPTION_NAME, title);
		Bundle options = getTransitionOptions(transitions);
		switchTo(ScreenChange.detailFragment(EpisodeListFragment.class, args, options));
		return true;
	}

	public boolean displayPodcastViaITunes(String title, String itunesUrl, View... transitions) {
		Bundle args = new Bundle(1);
		args.putString(Constants.EXTRA_FRAGMENT_CLASSNAME, EpisodeListFragment.class.getCanonicalName());
		args.putString(Constants.EXTRA_ITUNES_ID, itunesUrl);
		args.putString(Constants.EXTRA_SUBSCRIPTION_NAME, title);
		Bundle options = getTransitionOptions(transitions);
		switchTo(ScreenChange.detailFragment(EpisodeListFragment.class, args, options));
		return true;
	}

	public boolean displaySubscription(long subscriptionId, View... transitions) {
		Bundle args = new Bundle(2);
		args.putString(Constants.EXTRA_FRAGMENT_CLASSNAME, EpisodeListFragment.class.getCanonicalName());
		args.putLong(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		Bundle options = getTransitionOptions(transitions);
		switchTo(ScreenChange.activity(BareFragmentActivity.class, args, options));
		return true;
	}

	public boolean displaySubscription(CharSequence title, long subscriptionId, View... transitions) {
		Bundle args = new Bundle(3);
		args.putString(Constants.EXTRA_FRAGMENT_CLASSNAME, EpisodeListFragment.class.getCanonicalName());
		args.putLong(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		args.putString(Constants.EXTRA_SUBSCRIPTION_NAME, title.toString());
		Bundle options = getTransitionOptions(transitions);
		switchTo(ScreenChange.activity(BareFragmentActivity.class, args, options));
		return true;
	}

	public boolean displaySubscriptionSettings(long subscriptionId) {
		Bundle args = new Bundle(1);
		args.putLong(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
		switchTo(ScreenChange.detailFragment(SubscriptionSettingsFragment.class, args));
		return true;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private Bundle getTransitionOptions(View... views) {
		if (views.length == 0 || Build.VERSION.SDK_INT < 21)
			return null;

		ArrayList<Pair<View, String>> pairs = new ArrayList<>(views.length);
		for (View view : views) {
			if (view == null)
				continue;
			if (view.getTransitionName() == null || view.getTransitionName().equals("")) {
				Log.e("AppFlow", "transitioning view has no transition name");
				continue;
			}
			pairs.add(Pair.create(view, view.getTransitionName()));
		}
		return ActivityOptions.makeSceneTransitionAnimation(_activity, pairs.toArray(new Pair[pairs.size()])).toBundle();
	}

	private void startActivityFragment(Class<? extends Fragment> fragmentClass) {
		ScreenChange sc = ScreenChange.fragmentActivity(fragmentClass);
		switchTo(sc);
	}

	private static class LifecycleCallbackHandler implements Application.ActivityLifecycleCallbacks {
		@Override
		public void onActivityCreated(Activity activity, Bundle inState) {
			_currentActivity = activity;

			// keep reference to main activity so we can change its fragment
			if (activity instanceof MainActivity) {
				_mainActivity = new WeakReference<>((MainActivity) activity);
				_hasDetailFragment = _mainActivity.get().hasDetailFragment();
			}
		}

		@Override public void onActivityStarted(Activity activity) { }
		@Override public void onActivityResumed(Activity activity) { }
		@Override public void onActivityPaused(Activity activity) { }
		@Override public void onActivityStopped(Activity activity) { }
		@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
		@Override public void onActivityDestroyed(Activity activity) { }
	}
}
