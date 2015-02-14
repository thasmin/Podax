package com.axelby.podax.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

import javax.annotation.Nonnull;

public class SubscriptionListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private SubscriptionAdapter _adapter = null;
	private RecyclerView _listView;

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

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) getActivity().findViewById(R.id.list);
		_listView.setLayoutManager(new CoverFlowLayoutManager());
		_listView.setItemAnimator(new DefaultItemAnimator());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_listView.setAdapter(_adapter);

        View.OnClickListener addListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(PodaxFragmentActivity.createIntent(getActivity(), AddSubscriptionFragment.class, null));
            }
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

		_adapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		_adapter.clear();
	}

	private class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.ViewHolder> {
		private Cursor _cursor;

        public class ViewHolder extends RecyclerView.ViewHolder {
			/*
            public final TextView title;
            public final TextView description;
            public final ImageButton more;
            */
			public final ImageView thumbnail;

            public ViewHolder(View v) {
				super(v);

				/*
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
                more = (ImageButton) v.findViewById(R.id.more);
				thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
				*/
				thumbnail = (ImageView) v;
				thumbnail.setOnClickListener(_subscriptionChoiceHandler);
            }
        }

		private final View.OnClickListener _subscriptionChoiceHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, (long) view.getTag()));
			}
		};

        private final View.OnClickListener _moreClickHandler = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final long subscriptionId = (Long) view.getTag();
                PopupMenu menu = new PopupMenu(getActivity(), view);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.refresh) {
                            UpdateService.updateSubscription(getActivity(), subscriptionId);
                            return true;
                        } else if (menuItem.getItemId() == R.id.unsubscribe) {
                            Uri subscriptionUri= SubscriptionProvider.getContentUri(subscriptionId);
                            getActivity().getContentResolver().delete(subscriptionUri, null, null);
                            return true;
                        } else if (menuItem.getItemId() == R.id.settings) {
                            startActivity(PodaxFragmentActivity.createIntent(getActivity(), SubscriptionSettingsFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId));
                            return true;
                        }
                        return false;
                    }
                });
                menu.inflate(R.menu.subscription_list_item);
                menu.show();
            }
        };

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscription_list_item, parent, false);
			ImageView view = new ImageView(parent.getContext());
			view.setLayoutParams(new ViewGroup.LayoutParams(300, 300));
			view.setScaleType(ImageView.ScaleType.FIT_XY);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			_cursor.moveToPosition(position);
			SubscriptionCursor subscription = new SubscriptionCursor(_cursor);

			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(holder.thumbnail.getContext(), subscription.getId()));
			holder.thumbnail.setTag(subscription.getId());
			/*
			holder.title.setText(subscription.getTitle());
            if (subscription.getDescription() != null)
                holder.description.setText(subscription.getDescription().trim());
            else
                holder.description.setText(R.string.description_not_available);
			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), subscription.getId()));
            holder.more.setTag(subscription.getId());
            */

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

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
			notifyDataSetChanged();
		}

		public void clear() {
			_cursor = null;
			notifyDataSetChanged();
		}

	}
}
