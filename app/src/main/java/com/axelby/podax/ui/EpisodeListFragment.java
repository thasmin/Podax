package com.axelby.podax.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.AppFlow;
import com.axelby.podax.BR;
import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.UpdateService;
import com.axelby.podax.databinding.EpisodelistFragmentBinding;
import com.axelby.podax.itunes.RSSUrlFetcher;
import com.axelby.podax.model.DBAdapter;
import com.axelby.podax.model.EpisodeDB;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionDB;
import com.axelby.podax.model.SubscriptionData;
import com.axelby.podax.model.SubscriptionEditor;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EpisodeListFragment extends RxFragment {
	private SubscriptionData _subscription = null;
	private EpisodelistFragmentBinding _binding = null;
	private EpisodeListAdapter _adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

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
			subIdObservable = PodaxDB.subscriptions.watch(subscriptionId);
		} else {
			// get subscription id from either rss url or itunes id url
			Observable<String> rssUrlObservable;
			if (itunesIdUrl != null)
				rssUrlObservable = getRSSUrlFromITunesUrl(itunesIdUrl);
			else
				rssUrlObservable = Observable.just(rssUrl);

			subIdObservable = rssUrlObservable
				.subscribeOn(Schedulers.io())
				.flatMap(this::getSubscriptionFromRSSUrl);
		}

		subIdObservable
			.map(this::ensureSubscriptionPopulated)
			.observeOn(AndroidSchedulers.mainThread())
			.compose(RxLifecycle.bindFragment(lifecycle()))
			.subscribe(
				this::setSubscription,
				e -> Log.e("EpisodeListFragment", "error while retrieving episodes", e)
			);
	}

	private SubscriptionData ensureSubscriptionPopulated(SubscriptionData sub) {
		if (sub.getTitle() == null)
			UpdateService.updateSubscription(getActivity(), sub.getId());
		return sub;
	}

	private Observable<SubscriptionData> getSubscriptionFromRSSUrl(String rssUrl) {
		SubscriptionData sub = PodaxDB.subscriptions.getForRSSUrl(rssUrl);
		if (sub != null)
			return Observable.just(sub);

		long subId = SubscriptionEditor.addSingleUseSubscription(getActivity(), rssUrl);
		return Observable.just(SubscriptionData.create(subId));
	}

	private Observable<String> getRSSUrlFromITunesUrl(String iTunesUrl) {
		// TODO: put in model
		SQLiteDatabase db = new DBAdapter(getActivity()).getReadableDatabase();
		String sql =
			"SELECT " +
			SubscriptionDB.COLUMN_URL +
			" FROM subscriptions WHERE _id = (SELECT subscriptionID FROM itunes WHERE idUrl = ?)";
		Cursor c = db.rawQuery(sql, new String[]{iTunesUrl});
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

		RecyclerView list = _binding.list;
		list.setLayoutManager(new LinearLayoutManager(list.getContext()));
	}

	public void setSubscription(SubscriptionData subscription) {
		_subscription = subscription;
		_binding.setSubscription(subscription);

		if (_adapter != null)
			_adapter.stopWatching();
		_adapter = new EpisodeListAdapter(subscription.getEpisodes());
		_binding.list.setAdapter(_adapter);
	}

	private Subscriber<Long> _updateActivityObserver = new Subscriber<Long>() {
		@Override public void onCompleted() { }
		@Override public void onError(Throwable e) {
			Log.e("EpisodeListFragment", "error listening for subscription updates", e);
		}

		@Override public void onNext(Long updatingId) {
			boolean wasUpdating = _binding.currentlyUpdating.getVisibility() == View.VISIBLE;
			boolean isUpdating = _subscription.isCurrentlyUpdating();
			_binding.currentlyUpdating.setVisibility(isUpdating ? View.VISIBLE : View.GONE);
			if (wasUpdating && !isUpdating)
				updateSubscription();
		}
	};

	private void updateSubscription() {
		if (_subscription == null)
			return;

		PodaxDB.subscriptions.watch(_subscription.getId())
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

		if (_adapter != null)
			_adapter.stopWatching();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (_subscription != null)
			_binding.currentlyUpdating.setVisibility(_subscription.isCurrentlyUpdating() ? View.VISIBLE : View.GONE);

		if (getView() == null)
			return;
		UpdateService.watch().observeOn(AndroidSchedulers.mainThread()).subscribe(_updateActivityObserver);

		// refresh changes from subscription settings fragment
		updateSubscription();

		if (_adapter != null)
			_adapter.startWatching();
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
        if (item.getItemId() == R.id.unsubscribe) {
			new ConfirmUnsubscribeDialog().setSubscription(_subscription).show(getFragmentManager(), "confirmUnsubscribe");
            return true;
        }
		return false;
    }

	public static class ConfirmUnsubscribeDialog extends DialogFragment {
		private SubscriptionData _sub;

		public ConfirmUnsubscribeDialog() { }

		public ConfirmUnsubscribeDialog setSubscription(SubscriptionData subscription) {
			_sub = subscription;
			return this;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.episodelist_unsubscribe_dialog, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			view.findViewById(R.id.unsubscribe).setOnClickListener(button -> {
				PodaxDB.subscriptions.delete(_sub.getId());
				AppFlow.get(getActivity()).goBack();
				dismiss();
			});
			view.findViewById(R.id.cancel).setOnClickListener(button -> dismiss());
		}

		@Override
		public void onStart() {
			super.onStart();
			if (getDialog() != null && getDialog().getWindow() != null)
				getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
	}

	private class EpisodeListAdapter extends RecyclerView.Adapter<DataBoundViewHolder> {
		private final List<EpisodeData> _episodes;
		private final List<Long> _ids;
		private final Subscriber<? super EpisodeDB.EpisodeChange> _episodeUpdater = new Subscriber<EpisodeDB.EpisodeChange>() {
			@Override public void onCompleted() { }
			@Override public void onError(Throwable e) { }

			@Override
			public void onNext(EpisodeDB.EpisodeChange episodeChange) {
				int index = Observable.from(_episodes)
					.zipWith(Observable.range(0, _episodes.size()),
						(ep, i) -> { return new Pair<>(ep.getId(), i); } )
					.filter(pair -> Objects.equals(pair.first, episodeChange.getId()))
					.toBlocking()
					.single().second;
				_episodes.set(index, episodeChange.getNewData());
				notifyItemChanged(index, episodeChange.getNewData());
			}
		};

		EpisodeListAdapter(List<EpisodeData> episodes) {
			_episodes = episodes;
			_ids = Observable.from(_episodes).map(EpisodeData::getId).toList().toBlocking().single();

			PodaxDB.episodes.watchAll()
				.filter(change -> _ids.contains(change.getId()))
				.subscribe(_episodeUpdater);
		}

		@Override
		public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return DataBoundViewHolder.from(parent, R.layout.episodelist_item);
		}

		@Override
		public void onBindViewHolder(DataBoundViewHolder holder, int position) {
			holder.binding.setVariable(BR.episode, _episodes.get(position));
		}

		@Override
		public int getItemCount() {
			return _episodes.size();
		}

		void startWatching() {
			if (_episodeUpdater.isUnsubscribed())
				PodaxDB.episodes.watchAll()
					.filter(change -> _ids.contains(change.getId()))
					.subscribe(_episodeUpdater);
		}

		void stopWatching() {
			_episodeUpdater.unsubscribe();
		}
	}
}
