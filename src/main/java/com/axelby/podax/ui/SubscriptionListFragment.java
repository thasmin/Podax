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
import com.squareup.picasso.Picasso;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.RxFragment;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SubscriptionListFragment extends RxFragment
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private PodcastListAdapter _adapter = null;
	private SubscriptionAdapter _subscriptionAdapter = null;

	private Cursor _cursor;

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
		/*
		RecyclerView subscriptionList = (RecyclerView) view.findViewById(R.id.subscription_list);
		subscriptionList.setLayoutManager(new WrappingLinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
		subscriptionList.setItemAnimator(new DefaultItemAnimator());
		subscriptionList.setAdapter(_subscriptionAdapter);

		LinearLayout layout = (LinearLayout) view.findViewById(R.id.layout);

		LinearLayout titleLayout = (LinearLayout) LayoutInflater.from(layout.getContext())
			.inflate(R.layout.subscription_list_title, layout, false);
		TextView title = (TextView) titleLayout.findViewById(R.id.title);
		title.setText("iTunes Top Podcasts");
		layout.addView(titleLayout);

		RecyclerView rv = new RecyclerView(layout.getContext());
		rv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		rv.setLayoutManager(new WrappingLinearLayoutManager(layout.getContext(), LinearLayoutManager.HORIZONTAL, false));
		rv.setItemAnimator(new DefaultItemAnimator());
		rv.setAdapter(new iTunesAdapter(layout.getContext()));
		layout.addView(rv);
		*/


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
		changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		clearCursor();
	}

	private void changeCursor(Cursor cursor) {
		_cursor = cursor;
		_subscriptionAdapter.notifyDataSetChanged();
	}

	private void clearCursor() {
		_cursor = null;
		_subscriptionAdapter.notifyDataSetChanged();
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

		private boolean[] expanded = { true, false, false };

		public class TitleHolder extends RecyclerView.ViewHolder {
			public final TextView title;
			public final ImageView expand;
			public int position;

			public TitleHolder(View view) {
				super(view);
				title = (TextView) view.findViewById(R.id.title);
				expand = (ImageView) view.findViewById(R.id.expand);

				expand.setOnClickListener(image -> {
					if (expanded[position / 2])
						return;

					expanded[position / 2] = true;
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

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			if (getItemViewType(position) == TYPE_TITLE) {
				TitleHolder th = (TitleHolder) holder;
				th.position = position;
				switch (position) {
					case 0:
						th.title.setText(R.string.subscriptions);
						break;
					case 2:
						th.title.setText(R.string.itunes_top_podcasts);
						break;
					case 4:
						th.title.setText(R.string.npr_podcasts);
						break;
				}
				return;
			}

			if (!expanded[position / 2])
				return;

			ListHolder lh = (ListHolder) holder;
			if (position == 1)
				lh.list.setAdapter(_subscriptionAdapter);
			if (position == 3)
				lh.list.setAdapter(new iTunesAdapter(getActivity()));
		}

		@Override
		public int getItemCount() {
			return 6;
		}

		@Override
		public int getItemViewType(int position) {
			if (position % 2 == 0)
				return TYPE_TITLE;
			else if (position == 1)
				return TYPE_SUBSCRIPTIONS;
			else if (position == 3)
				return TYPE_ITUNES;
			else
				return TYPE_PODAXAPP;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public void setHasStableIds(boolean hasStableIds) {
			super.setHasStableIds(hasStableIds);
		}
	}

	private class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionListViewHolder> {

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

		public iTunesAdapter(Context context) {
			this(context, 0);
		}

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

			DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
			int px = (int)((128 * displayMetrics.density) + 0.5);
			Picasso.with(holder.thumbnail.getContext())
					.load(p.imageUrl)
					.resize(px, px)
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
}
