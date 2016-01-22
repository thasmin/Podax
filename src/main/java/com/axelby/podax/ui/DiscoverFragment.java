package com.axelby.podax.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.itunes.Podcast;
import com.axelby.podax.itunes.PodcastFetcher;
import com.axelby.podax.podaxapp.PodaxAppClient;
import com.squareup.picasso.Picasso;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class DiscoverFragment extends RxFragment {

	private final int[] _categoryIds = {
		1301, 1321, 1303, 1304, 1323, 1325, 1307, 1305,
		1310, 1311, 1314, 1315, 1324, 1316, 1309, 1318,
	};

	private final int[] _titles = {
		R.string.itunes_top_podcasts,
		R.string.npr_podcasts,
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
	private final RecyclerView[] _recyclerViews = new RecyclerView[_titles.length];

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

	private class CategoryAdapter extends PagerAdapter {
		@Override
		public int getCount() {
			return _titles.length;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (_recyclerViews[position] != null) {
				container.addView(_recyclerViews[position]);
				return _recyclerViews[position];
			}

			RecyclerView list = new RecyclerView(container.getContext());
			list.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			list.setLayoutManager(new GridLayoutManager(container.getContext(), 3));
			if (position == 0)
				list.setAdapter(new iTunesAdapter(getActivity(), 0));
			else if (position == 1)
				list.setAdapter(new PodaxAppAdapter(getActivity(), "npr"));
			else
				list.setAdapter(new iTunesAdapter(getActivity(), _categoryIds[position - 2]));
			_recyclerViews[position] = list;
			container.addView(list);
			return list;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return getString(_titles[position]).toUpperCase();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		pager.setAdapter(new CategoryAdapter());
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public final View holder;
		public final ImageView thumbnail;
		public final TextView title;

		public ViewHolder(View v) {
			super(v);

			holder = v;
			thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
			title = (TextView) v.findViewById(R.id.title);
		}
	}

	private class iTunesAdapter extends RecyclerView.Adapter<ViewHolder> {
		private final ArrayList<Podcast> _podcasts = new ArrayList<>(100);

		public iTunesAdapter(Context context, int category) {
			setHasStableIds(true);

			new PodcastFetcher(context, category).getPodcasts()
				.observeOn(AndroidSchedulers.mainThread())
				.compose(RxLifecycle.bindFragment(lifecycle()))
				.subscribe(new Subscriber<List<Podcast>>() {
					@Override
					public void onError(Throwable e) {
						Log.e("itunesloader", "error while loading itunes toplist", e);
					}

					@Override
					public void onNext(List<Podcast> podcasts) {
						_podcasts.clear();
						_podcasts.addAll(podcasts);
						notifyItemRangeInserted(0, podcasts.size());
					}

					@Override
					public void onCompleted() {
						notifyDataSetChanged();
					}
				});
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			view.setOnClickListener(v -> {
				Podcast p = (Podcast) v.getTag();
				Bundle b = new Bundle(1);
				b.putString(Constants.EXTRA_ITUNES_ID, p.idUrl);
				b.putString(Constants.EXTRA_SUBSCRIPTION_NAME, p.name);
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, b));
			});
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			Podcast p = _podcasts.get(position);
			holder.holder.setTag(p);
			holder.title.setText(p.name);

			Picasso.with(holder.thumbnail.getContext())
				.load(p.imageUrl)
				.fit()
				.into(holder.thumbnail);
		}

		@Override
		public long getItemId(int position) {
			return _podcasts.get(position).id;
		}

		@Override
		public int getItemCount() {
			return _podcasts.size();
		}
	}

	private class PodaxAppAdapter extends RecyclerView.Adapter<ViewHolder> {
		com.axelby.podax.podaxapp.Podcast[] _podcasts = new com.axelby.podax.podaxapp.Podcast[0];

		public PodaxAppAdapter(Context context, String shortcode) {
			setHasStableIds(true);

			PodaxAppClient.get(context).getNetworkInfo(shortcode)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.compose(RxLifecycle.bindFragment(lifecycle()))
				.subscribe(
					network -> {
						_podcasts = network.podcasts;
						notifyDataSetChanged();
					},
					e -> Log.e("podaxappadapter", "error while retrieving network", e)
				);

		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			view.setOnClickListener(v -> {
				com.axelby.podax.podaxapp.Podcast p = (com.axelby.podax.podaxapp.Podcast) v.getTag();
				Bundle b = new Bundle(1);
				b.putString(Constants.EXTRA_RSSURL, p.rssUrl);
				b.putString(Constants.EXTRA_SUBSCRIPTION_NAME, p.title);
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, b));
			});
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			com.axelby.podax.podaxapp.Podcast p = _podcasts[position];
			holder.holder.setTag(p);
			holder.title.setText(p.title);

			DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
			int px = (int)((128 * displayMetrics.density) + 0.5);
			Picasso.with(holder.thumbnail.getContext())
					.load(p.imageUrl)
					.resize(px, px)
					.into(holder.thumbnail);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return _podcasts.length;
		}
	}
}
