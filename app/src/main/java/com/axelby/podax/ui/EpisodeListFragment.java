package com.axelby.podax.ui;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.axelby.podax.AppFlow;
import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.UpdateService;
import com.axelby.podax.databinding.EpisodelistFragmentBinding;
import com.axelby.podax.itunes.RSSUrlFetcher;
import com.axelby.podax.model.DBAdapter;
import com.axelby.podax.model.SubscriptionDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.Subscriptions;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EpisodeListFragment extends RxFragment {
	private SubscriptionData _subscription = null;
	private EpisodelistFragmentBinding _binding = null;
	private LinearLayout _listView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (_listView != null)
			_listView.removeAllViews();

		// extract subscription id
		if (getArguments() == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id or itunes id url");
		long subscriptionId = getArguments().getLong(Constants.EXTRA_SUBSCRIPTION_ID, -1);
		String itunesIdUrl = getArguments().getString(Constants.EXTRA_ITUNES_ID);
		String rssUrl = getArguments().getString(Constants.EXTRA_RSSURL);
		if (subscriptionId == -1 && itunesIdUrl == null && rssUrl == null)
			throw new IllegalArgumentException("EpisodeListFragment needs a subscription id, itunes id url, or rss url");

		Observable<SubscriptionData> subIdObservable;
		if (subscriptionId != -1) {
			subIdObservable = Subscriptions.watch(subscriptionId);
		} else {
			// get subscription id from either rss url or itunes id url
			Observable<String> rssUrlObservable;
			if (itunesIdUrl != null)
				rssUrlObservable = getRSSUrlFromITunesUrl(itunesIdUrl);
			else
				rssUrlObservable = Observable.just(rssUrl);

			subIdObservable = rssUrlObservable
				.subscribeOn(Schedulers.io())
				.flatMap(this::getSubscriptionIdFromRSSUrl);
		}

		subIdObservable
			.observeOn(AndroidSchedulers.mainThread())
			.compose(RxLifecycle.bindFragment(lifecycle()))
			.subscribe(
				this::setSubscription,
				e -> Log.e("EpisodeListFragment", "error while retrieving episodes", e)
			);
	}

	private Observable<SubscriptionData> getSubscriptionIdFromRSSUrl(String rssUrl) {
		SubscriptionData sub = Subscriptions.getForRSSUrl(rssUrl);
		if (sub != null)
			return Observable.just(sub);

		long subId = SubscriptionDB.addSingleUseSubscription(getActivity(), rssUrl);
		return Observable.just(SubscriptionData.create(subId));
	}

	private Observable<String> getRSSUrlFromITunesUrl(String iTunesUrl) {
		SQLiteDatabase db = new DBAdapter(getActivity()).getReadableDatabase();
		Cursor c = db.rawQuery("SELECT " + SubscriptionDB.COLUMN_URL + " FROM subscriptions WHERE _id = (SELECT subscriptionID FROM itunes WHERE idUrl = ?)", new String[]{iTunesUrl});
		if (c != null) {
			if (c.moveToFirst() && c.isNull(0)) {
				String url = c.getString(0);
				c.close();
				db.close();
				return Observable.just(url);
			}
			c.close();
		}
		db.close();

		return new RSSUrlFetcher(getActivity(), iTunesUrl).getRSSUrl();
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		_binding = EpisodelistFragmentBinding.inflate(inflater, container, false);
		return _binding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = _binding.list;
	}

	public void setSubscription(SubscriptionData subscription) {
		_subscription = subscription;
		_binding.setSubscription(subscription);
	}

	private Subscriber<Long> _updateActivityObserver = new Subscriber<Long>() {
		@Override public void onCompleted() { }
		@Override public void onError(Throwable e) {
			Log.e("EpisodeListFragment", "error listening for subscription updates", e);
		}

		@Override public void onNext(Long updatingId) {
			if (updatingId == null || updatingId != _subscription.getId()) {
				_binding.currentlyUpdating.setVisibility(View.GONE);
				if (_binding.currentlyUpdating.getVisibility() == View.VISIBLE)
					updateSubscription();
				return;
			}

			_binding.currentlyUpdating.setVisibility(View.VISIBLE);
		}
	};

	private void updateSubscription() {
		if (_subscription == null)
			return;

		Subscriptions.watch(_subscription.getId())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(
				EpisodeListFragment.this::setSubscription,
				e -> Log.e("EpisodeListFragment", "error while refreshing episodes after update", e)
			);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (getView() == null)
			return;

		_updateActivityObserver.unsubscribe();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (_subscription != null)
			_binding.currentlyUpdating.setVisibility(_subscription.isCurrentlyUpdating() ? View.VISIBLE : View.GONE);

		if (getView() == null)
			return;
		UpdateService.getUpdatingObservable().subscribe(_updateActivityObserver);

		// refresh changes from subscription settings fragment
		updateSubscription();
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            AppFlow.get(getActivity()).displaySubscriptionSettings(_subscription.getId());
            return true;
        }
        return false;
    }
}
