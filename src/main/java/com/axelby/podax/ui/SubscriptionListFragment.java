package com.axelby.podax.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.ITunesPodcastLoader;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SubscriptionListFragment extends Fragment
		implements LoaderManager.LoaderCallbacks<Cursor> {

	private SubscriptionAdapter _adapter = null;

	private Cursor _cursor;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter();
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	public static class AddSubscriptionDialog extends DialogFragment {
		public AddSubscriptionDialog() {
		}

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.subscription_list_add, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			EditText url = (EditText) view.findViewById(R.id.url);
			url.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView url, int actionId, KeyEvent event) {
					SubscriptionProvider.addNewSubscription(url.getContext(), url.getText().toString());
					dismiss();
					return true;
				}
			});

			view.findViewById(R.id.subscribe).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View button) {
					EditText url = (EditText) ((ViewGroup)button.getParent()).findViewById(R.id.url);
					SubscriptionProvider.addNewSubscription(button.getContext(), url.getText().toString());
					dismiss();
				}
			});
		}
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		RecyclerView subscriptionList = (RecyclerView) view.findViewById(R.id.subscription_list);
		subscriptionList.setLayoutManager(new WrappingLinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
		subscriptionList.setItemAnimator(new DefaultItemAnimator());
		subscriptionList.setAdapter(_adapter);

		LinearLayout layout = (LinearLayout) view.findViewById(R.id.layout);

		LinearLayout titleLayout = (LinearLayout) LayoutInflater.from(view.getContext())
				.inflate(R.layout.subscription_list_title, layout, false);
		TextView title = (TextView) titleLayout.findViewById(R.id.title);
		title.setText("iTunes Top Podcasts");
		layout.addView(titleLayout);

		RecyclerView rv = new RecyclerView(view.getContext());
		rv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		rv.setLayoutManager(new WrappingLinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
		rv.setItemAnimator(new DefaultItemAnimator());
		rv.setAdapter(new iTunesAdapter());
		layout.addView(rv);


		View.OnClickListener addListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//startActivity(PodaxFragmentActivity.createIntent(getActivity(), AddSubscriptionFragment.class, null));
				AddSubscriptionDialog dialog = new AddSubscriptionDialog();
				dialog.show(getFragmentManager(), "addSubscription");
			}
		};
		getActivity().findViewById(R.id.add).setOnClickListener(addListener);

		/*
		View settings = view.findViewById(R.id.settings);
		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), SubscriptionSettingsFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, _selectedId));
			}
		});

		View unsub = view.findViewById(R.id.unsubscribe);
		unsub.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().getContentResolver().delete(SubscriptionProvider.getContentUri(_selectedId), null, null);
				_adapter.notifyItemRemoved(_selectedPosition);
			}
		});
		*/
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
		_adapter.notifyDataSetChanged();
	}

	private void clearCursor() {
		_cursor = null;
		_adapter.notifyDataSetChanged();
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

	private class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionListViewHolder> {

		private final View.OnClickListener _subscriptionChoiceHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				long subId = (long) view.getTag();
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, subId));
			}
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
		private final ArrayList<ITunesPodcastLoader.Podcast> _podcasts = new ArrayList<>(100);

		public iTunesAdapter() {
			setHasStableIds(true);

			ITunesPodcastLoader.getPodcasts()
					.onBackpressureBuffer(100)
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(new Subscriber<ITunesPodcastLoader.Podcast>() {
						@Override
						public void onError(Throwable e) {
							Log.e("itunesloader", "subscriber error", e);
						}

						@Override
						public void onNext(ITunesPodcastLoader.Podcast podcast) {
							_podcasts.add(podcast);
							notifyItemInserted(_podcasts.size() - 1);
							notifyDataSetChanged();
						}

						@Override
						public void onCompleted() {
						}
					});
		}

		@Override
		public SubscriptionListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			return new SubscriptionListViewHolder(view);
		}

		@Override
		public void onBindViewHolder(SubscriptionListViewHolder holder, int position) {
			ITunesPodcastLoader.Podcast p = _podcasts.get(position);
			holder.holder.setTag(p.id);
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
