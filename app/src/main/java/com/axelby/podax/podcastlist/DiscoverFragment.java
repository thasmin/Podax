package com.axelby.podax.podcastlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.R;
import com.axelby.podax.itunes.PodcastFetcher;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;
import com.trello.rxlifecycle.components.RxFragment;

import rx.Observable;

public class DiscoverFragment extends RxFragment {

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
		super.onViewCreated(view, savedInstanceState);

		final int[] _categoryIds = {
			0,
			1301, 1321, 1303, 1304, 1323, 1325, 1307, 1305,
			1310, 1311, 1314, 1315, 1324, 1316, 1309, 1318,
		};

		final int[] _titles = {
			R.string.itunes_top_podcasts,
			R.string.itunes_arts_podcasts,
			R.string.itunes_business_podcasts,
			R.string.itunes_comedy_podcasts,
			R.string.itunes_education_podcasts,
			R.string.itunes_games_hobbies_podcasts,
			R.string.itunes_government_organizations_podcasts,
			R.string.itunes_health_podcasts,
			R.string.itunes_kids_podcasts,
			R.string.itunes_music_podcasts,
			R.string.itunes_news_politics_podcasts,
			R.string.itunes_religion_spirituality_podcasts,
			R.string.itunes_science_medicine_podcasts,
			R.string.itunes_society_culture_podcasts,
			R.string.itunes_sports_recreation_podcasts,
			R.string.itunes_tv_film_podcasts,
			R.string.itunes_technology_podcasts,
		};

		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		pager.setAdapter(new CategoryAdapter(getActivity(), _titles,
			position ->
				new ListAdapter(
					new PodcastFetcher(getActivity(), _categoryIds[position]).getPodcasts()
					.flatMap(Observable::from)
					.map(podcast -> ItemModel.fromITunes(podcast.name, podcast.imageUrl, podcast.idUrl))
					.toList()
					.compose(RxLifecycleAndroid.bindFragment(lifecycle()))
				)
			)
		);
	}
}
