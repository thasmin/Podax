package com.axelby.podax.podcastlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.R;
import com.axelby.podax.podaxapp.PodaxAppClient;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import rx.Observable;
import rx.schedulers.Schedulers;

public class NetworksFragment extends RxFragment {

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.discovery, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		String[] ids = {
			"npr", "podcastone"
		};
		int[] titles = {
			R.string.npr,
			R.string.podcastone,
		};

		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		pager.setAdapter(new CategoryAdapter(getActivity(), titles,
			position ->
				new ListAdapter(
					PodaxAppClient.get(getActivity()).getNetworkInfo(ids[position])
					.subscribeOn(Schedulers.io())
					.flatMap(network -> Observable.from(network.podcasts))
					.map(podcast -> ItemModel.fromRSS(podcast.title, podcast.imageUrl, podcast.rssUrl))
					.toList()
					.compose(RxLifecycle.bindFragment(lifecycle()))
				)
			)
		);
	}

}
