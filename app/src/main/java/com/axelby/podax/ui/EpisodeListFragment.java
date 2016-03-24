package com.axelby.podax.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
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
import com.axelby.podax.model.DBAdapter;
import com.axelby.podax.R;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.model.Subscriptions;
import com.axelby.podax.databinding.EpisodelistFragmentBinding;
import com.axelby.podax.itunes.RSSUrlFetcher;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import javax.annotation.Nonnull;

import rx.Observable;
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
			subIdObservable = Subscriptions.watch(getActivity(), subscriptionId);
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
		Observable<SubscriptionData> addSubscriptionObservable = Observable.create(subscriber -> {
			Uri newUri = SubscriptionProvider.addSingleUseSubscription(getActivity(), rssUrl);
			long subscriptionId = ContentUris.parseId(newUri);
			subscriber.onNext(SubscriptionData.create(getActivity(), subscriptionId));
			subscriber.onCompleted();
		});

		// use the first observable that creates a response
		return Observable.concat(
				Subscriptions.getForRSSUrl(getActivity(), rssUrl),
				addSubscriptionObservable)
			.first();
	}

	private Observable<String> getRSSUrlFromITunesUrl(String iTunesUrl) {
		SQLiteDatabase db = new DBAdapter(getActivity()).getReadableDatabase();
		Cursor c = db.rawQuery("SELECT " + SubscriptionProvider.COLUMN_URL + " FROM subscriptions WHERE _id = (SELECT subscriptionID FROM itunes WHERE idUrl = ?)", new String[]{iTunesUrl});
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

	private BroadcastReceiver _updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long updatingId = ContentUris.parseId(intent.getData());
			if (updatingId == -1 || updatingId != _subscription.getId()) {
				_binding.currentlyUpdating.setVisibility(View.GONE);
				return;
			}

			if (intent.getAction().equals(Constants.ACTION_DONE_UPDATING_SUBSCRIPTION)) {
				_binding.currentlyUpdating.setVisibility(View.GONE);
				updateSubscription();
			} else {
				_binding.currentlyUpdating.setVisibility(View.VISIBLE);
			}
		}
	};

	private void updateSubscription() {
		if (_subscription == null)
			return;

		Subscriptions.watch(getActivity(), _subscription.getId())
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
		LocalBroadcastManager.getInstance(getView().getContext()).unregisterReceiver(_updateReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (_subscription != null)
			_binding.currentlyUpdating.setVisibility(_subscription.isCurrentlyUpdating() ? View.VISIBLE : View.GONE);

		if (getView() == null)
			return;
		IntentFilter intentFilter = new IntentFilter(Constants.ACTION_UPDATE_SUBSCRIPTION);
		intentFilter.addAction(Constants.ACTION_DONE_UPDATING_SUBSCRIPTION);
		intentFilter.addDataScheme("content");
		try {
			intentFilter.addDataType(SubscriptionProvider.ITEM_TYPE);
		} catch (IntentFilter.MalformedMimeTypeException ignored) { }
		LocalBroadcastManager.getInstance(getView().getContext()).registerReceiver(_updateReceiver, intentFilter);

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
