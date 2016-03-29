package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.SearchManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.R;
import com.axelby.podax.model.EpisodeData;
import com.axelby.podax.model.Episodes;
import com.axelby.podax.model.PodaxDB;
import com.axelby.podax.model.SubscriptionData;

import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SearchPodaxFragment extends Fragment implements SearchActivity.QueryChangedHandler {
	private GridLayout _subscriptionList;
	private TextView _subscriptionEmpty;

	private LinearLayout _episodeList;
	private TextView _episodeEmpty;

	private View.OnClickListener _subscriptionClickHandler = view -> {
		long id = (long) view.getTag();
		View thumbnail = view.findViewById(R.id.thumbnail);
		TextView title = (TextView) view.findViewById(R.id.title);
		AppFlow.get(getActivity()).displaySubscription(title.getText(), id, thumbnail, title);
	};

	private final View.OnClickListener _episodeClickHandler = view -> {
		long id = (long) view.getTag();
		View thumbnail = view.findViewById(R.id.thumbnail);
		View title = view.findViewById(R.id.title);
		AppFlow.get(getActivity()).displayEpisode(id, thumbnail, title);
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_podax_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		_subscriptionList = (GridLayout) view.findViewById(R.id.subscription_list);
		_subscriptionEmpty = (TextView) view.findViewById(R.id.subscription_empty);

		_episodeList = (LinearLayout) view.findViewById(R.id.episode_list);
		_episodeEmpty = (TextView) view.findViewById(R.id.episode_empty);

		String query = getArguments().getString(SearchManager.QUERY);
		onQueryChanged(query);
	}

	@Override
	public void onQueryChanged(String query) {
		Observable.just(PodaxDB.subscriptions.search(query))
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(
				this::updateSubscriptions,
				e -> Log.e("SearchPodaxFragment", "unable to run searches", e)
			);

		Episodes.search(query)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(
				this::updateEpisodes,
				e -> Log.e("SearchPodaxFragment", "unable to run searches", e)
			);
	}

	private void updateSubscriptions(List<SubscriptionData> subscriptions) {
		boolean isEmpty = subscriptions.size() == 0;
		_subscriptionList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
		_subscriptionEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

		_subscriptionList.removeAllViews();
		int rowCount = (int) Math.ceil(subscriptions.size() / 3.0f);
		_subscriptionList.setRowCount(rowCount);

		int thumbSize = _subscriptionList.getMeasuredWidth() / 3;
		thumbSize -= getResources().getDisplayMetrics().density * 10;

		LayoutInflater inflater = LayoutInflater.from(getActivity());
		for (SubscriptionData sub : subscriptions) {
			View view = inflater.inflate(R.layout.search_item_subscription, _subscriptionList, false);
			view.setOnClickListener(_subscriptionClickHandler);
			view.setTag(sub.getId());

			ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
			thumbnail.setLayoutParams(new LinearLayout.LayoutParams(thumbSize, thumbSize));
			sub.getThumbnailImage().into(thumbnail);

			TextView title = (TextView) view.findViewById(R.id.title);
			title.setLayoutParams(new LinearLayout.LayoutParams(thumbSize, ViewGroup.LayoutParams.WRAP_CONTENT));
			title.setText(sub.getTitle());

			_subscriptionList.addView(view);
		}
	}

	private void updateEpisodes(List<EpisodeData> episodes) {
		boolean isEmpty = episodes.size() == 0;
		_episodeList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
		_episodeEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

		LayoutInflater inflater = LayoutInflater.from(getActivity());
		for (EpisodeData ep : episodes) {
			View view = inflater.inflate(R.layout.search_item_episode, _episodeList, false);
			view.setOnClickListener(_episodeClickHandler);
			view.setTag(ep.getId());

			ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
			ep.getSubscriptionImage().into(thumbnail);

			TextView title = (TextView) view.findViewById(R.id.title);
			title.setText(ep.getTitle());

			_episodeList.addView(view);
		}
	}
}
