package com.axelby.podax.ui;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.axelby.podax.itunes.Podcast;
import com.axelby.podax.itunes.PodcastFetcher;
import com.axelby.podax.podaxapp.PodaxAppClient;
import com.squareup.picasso.Picasso;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SubscriptionListFragment extends RxFragment
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private PodcastListAdapter _adapter = null;
	private SubscriptionAdapter _subscriptionAdapter = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setRetainInstance(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new PodcastListAdapter();
		_subscriptionAdapter = new SubscriptionAdapter();
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	public static class AddSubscriptionDialog extends DialogFragment {
		public AddSubscriptionDialog() { }

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.subscription_list_add, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			EditText url = (EditText) view.findViewById(R.id.url);
			url.setOnEditorActionListener((url1, actionId, event) -> {
				SubscriptionProvider.addNewSubscription(url1.getContext(), url1.getText().toString());
				dismiss();
				return true;
			});

			view.findViewById(R.id.subscribe).setOnClickListener(button -> {
				EditText url1 = (EditText) ((ViewGroup)button.getParent()).findViewById(R.id.url);
				SubscriptionProvider.addNewSubscription(button.getContext(), url1.getText().toString());
				dismiss();
			});
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
		list.setAdapter(_adapter);

		View.OnClickListener addListener = view1 -> {
			AddSubscriptionDialog dialog = new AddSubscriptionDialog();
			dialog.show(getFragmentManager(), "addSubscription");
		};
		getActivity().findViewById(R.id.add).setOnClickListener(addListener);
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.subscription_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            UpdateService.updateSubscriptions(getActivity());
            return true;
        }
        return false;
    }

    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
				SubscriptionProvider.COLUMN_THUMBNAIL,
                SubscriptionProvider.COLUMN_DESCRIPTION,
		};
		return new CursorLoader(getActivity(), SubscriptionProvider.URI, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;
		_subscriptionAdapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		_subscriptionAdapter.changeCursor(null);
	}

	public class SubscriptionListViewHolder extends RecyclerView.ViewHolder {
		public final View holder;
		public final ImageView thumbnail;
		public final TextView title;

		public SubscriptionListViewHolder(View v) {
			super(v);

			holder = v;
			thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
			title = (TextView) v.findViewById(R.id.title);
		}
	}

	private class PodcastListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private final int TYPE_TITLE = 0;
		private final int TYPE_SUBSCRIPTIONS = 1;
		private final int TYPE_ITUNES = 2;
		private final int TYPE_PODAXAPP = 3;

		private boolean[] _expanded = { true, false, false,
			false, false, false, false, false, false, false, false,
			false, false, false, false, false, false, false, false  };

		public class TitleHolder extends RecyclerView.ViewHolder {
			public final TextView title;
			public final ImageView expand;
			public int position;

			public TitleHolder(View view) {
				super(view);
				title = (TextView) view.findViewById(R.id.title);
				expand = (ImageView) view.findViewById(R.id.expand);

				expand.setOnClickListener(image -> {
					if (_expanded[position / 2])
						return;

					_expanded[position / 2] = true;
					_adapter.notifyItemChanged(position + 1);
					expand.setImageResource(R.drawable.ic_action_expand);
				});
			}
		}

		public class ListHolder extends RecyclerView.ViewHolder {
			public RecyclerView list;

			public ListHolder(View view) {
				super(view);
				list = (RecyclerView) view;
			}
		}

		public PodcastListAdapter() {
			setHasStableIds(true);
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == TYPE_TITLE) {
				View v = LayoutInflater.from(getActivity()).inflate(R.layout.subscription_list_title, parent, false);
				return new TitleHolder(v);
			}

			RecyclerView rv = new RecyclerView(getActivity());
			rv.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			rv.setLayoutManager(new WrappingLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
			return new ListHolder(rv);
		}

		private final int[] _titles = {
			R.string.subscriptions,
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

		private final int[] _categoryIds = {
			1301, 1321, 1303, 1304, 1323, 1325, 1307, 1305,
			1310, 1311, 1314, 1315, 1324, 1316, 1309, 1318,
		};

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			if (getItemViewType(position) == TYPE_TITLE) {
				TitleHolder th = (TitleHolder) holder;
				th.position = position;
				th.title.setText(_titles[position / 2]);
				return;
			}

			// itunes toplists start expanded if they're cached
			if (position == 3 || position >= 7 && !_expanded[position / 2]) {
				int categoryId = 0;
				if (position >= 7)
					categoryId = _categoryIds[(position - 7) / 2];
				if (new PodcastFetcher(getActivity(), categoryId).isTodayCached())
					_expanded[position / 2] = true;
			}


			if (!_expanded[position / 2])
					return;

			ListHolder lh = (ListHolder) holder;
			if (position == 1)
				lh.list.setAdapter(_subscriptionAdapter);
			else if (position == 3)
				lh.list.setAdapter(new iTunesAdapter(getActivity(), 0));
			else if (position == 5)
				lh.list.setAdapter(new PodaxAppAdapter(getActivity(), "npr"));
			else
				lh.list.setAdapter(new iTunesAdapter(getActivity(), _categoryIds[(position - 7) / 2]));
		}

		@Override
		public int getItemCount() {
			return _titles.length * 2;
		}

		@Override
		public int getItemViewType(int position) {
			if (position % 2 == 0)
				return TYPE_TITLE;
			else if (position == 1)
				return TYPE_SUBSCRIPTIONS;
			else if (position == 5)
				return TYPE_PODAXAPP;
			else
				return TYPE_ITUNES;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	private class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionListViewHolder> {
		private Cursor _cursor;

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
			notifyDataSetChanged();
		}

		private final View.OnClickListener _subscriptionChoiceHandler = view -> {
			long subId = (long) view.getTag();
			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, subId));
		};

		@Override
		public SubscriptionListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			view.setOnClickListener(_subscriptionChoiceHandler);
			return new SubscriptionListViewHolder(view);
		}

		@Override
		public void onBindViewHolder(SubscriptionListViewHolder holder, int position) {
			_cursor.moveToPosition(position);
			SubscriptionCursor subscription = new SubscriptionCursor(_cursor);
			holder.holder.setTag(subscription.getId());
			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(holder.thumbnail.getContext(), subscription.getId()));
			holder.title.setText(subscription.getTitle());
		}

		@Override
        public long getItemId(int position) {
			_cursor.moveToPosition(position);
            return new SubscriptionCursor(_cursor).getId();
        }

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _cursor.getCount();
		}

	}

	private class iTunesAdapter extends RecyclerView.Adapter<SubscriptionListViewHolder> {
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
		public SubscriptionListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			view.setOnClickListener(v -> {
				Podcast p = (Podcast) v.getTag();
				Bundle b = new Bundle(1);
				b.putString(Constants.EXTRA_ITUNES_ID, p.idUrl);
				b.putString(Constants.EXTRA_SUBSCRIPTION_NAME, p.name);
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, b));
			});
			return new SubscriptionListViewHolder(view);
		}

		@Override
		public void onBindViewHolder(SubscriptionListViewHolder holder, int position) {
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

	private class PodaxAppAdapter extends RecyclerView.Adapter<SubscriptionListViewHolder> {
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
		public SubscriptionListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			view.setOnClickListener(v -> {
				com.axelby.podax.podaxapp.Podcast p = (com.axelby.podax.podaxapp.Podcast) v.getTag();
				Bundle b = new Bundle(1);
				b.putString(Constants.EXTRA_RSSURL, p.rssUrl);
				b.putString(Constants.EXTRA_SUBSCRIPTION_NAME, p.title);
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, b));
			});
			return new SubscriptionListViewHolder(view);
		}

		@Override
		public void onBindViewHolder(SubscriptionListViewHolder holder, int position) {
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
