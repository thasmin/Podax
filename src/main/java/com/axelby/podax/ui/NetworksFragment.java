package com.axelby.podax.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.axelby.podax.BR;
import com.axelby.podax.R;
import com.axelby.podax.podaxapp.PodaxAppClient;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NetworksFragment extends RxFragment {

	private final String[] _categoryIds = {
		"npr", "podcastone"
	};

	private final int[] _titles = {
		R.string.npr,
		R.string.podcastone,
	};

	private final ArrayList<WeakReference<RecyclerView>> _recyclerViews =
		new ArrayList<>(Collections.nCopies(_titles.length, new WeakReference<>(null)));

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
		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		pager.setAdapter(new CategoryAdapter());
	}

	private class CategoryAdapter extends PagerAdapter {
		@Override
		public int getCount() {
			return _titles.length;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (_recyclerViews.get(position).get() != null) {
				container.addView(_recyclerViews.get(position).get());
				return _recyclerViews.get(position).get();
			}

			RecyclerView list = new RecyclerView(container.getContext());
			list.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			list.setLayoutManager(new GridLayoutManager(container.getContext(), 3));
			list.setAdapter(new PodaxAppAdapter(getActivity(), _categoryIds[position]));
			_recyclerViews.set(position, new WeakReference<>(list));
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

	private class PodaxAppAdapter extends RecyclerView.Adapter<DataBoundViewHolder> {
		List<SubscriptionListFragment.ItemModel> _podcasts = new ArrayList<>(0);

		public PodaxAppAdapter(Context context, String shortcode) {
			setHasStableIds(true);

			PodaxAppClient.get(context).getNetworkInfo(shortcode)
				.subscribeOn(Schedulers.io())
				.flatMap(network -> Observable.from(network.podcasts))
				.map(podcast -> SubscriptionListFragment.ItemModel.fromRSS(podcast.title, podcast.imageUrl, podcast.rssUrl))
				.toList()
				.observeOn(AndroidSchedulers.mainThread())
				.compose(RxLifecycle.bindFragment(lifecycle()))
				.subscribe(
					podcasts -> {
						_podcasts = podcasts;
						notifyDataSetChanged();
					},
					e -> Log.e("podaxappadapter", "error while retrieving network", e)
				);
		}

		@Override
		public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return DataBoundViewHolder.from(parent, R.layout.subscription_list_item);
		}

		@Override
		public void onBindViewHolder(DataBoundViewHolder holder, int position) {
			holder.binding.setVariable(BR.model, _podcasts.get(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return _podcasts.size();
		}
	}
}
