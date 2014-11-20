package com.axelby.podax.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

import javax.annotation.Nonnull;

public class SubscriptionListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private SubscriptionAdapter _adapter = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter(getActivity(), null);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        setListAdapter(_adapter);

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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(0, 0, 0, R.string.unsubscribe);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0:
				AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				Cursor cursor = (Cursor) _adapter.getItem(menuInfo.position);
				SubscriptionCursor subscription = new SubscriptionCursor(cursor);
				getActivity().getContentResolver().delete(subscription.getContentUri(), null, null);
				break;
			default:
				return super.onContextItemSelected(item);
		}
		return true;
	}

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, id));
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
		_adapter.changeCursor(null);
	}

	private class SubscriptionAdapter extends ResourceCursorAdapter {
        private View.OnClickListener _moreClickHandler = new View.OnClickListener() {
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

        public class ViewHolder {
            public TextView title;
            public TextView description;
            public ImageView thumbnail;
            public ImageButton more;

            public ViewHolder(View v) {
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
                thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
                more = (ImageButton) v.findViewById(R.id.more);
            }
        }

		public SubscriptionAdapter(Context context, Cursor cursor) {
			super(context, R.layout.subscription_list_item, cursor, true);
		}

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            ViewHolder holder = new ViewHolder(view);
            holder.more.setOnClickListener(_moreClickHandler);
            view.setTag(holder);
            return view;
        }

        @Override
		public void bindView(View view, Context context, Cursor cursor) {
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);
            ViewHolder holder = (ViewHolder) view.getTag();

			holder.title.setText(subscription.getTitle());
            if (subscription.getDescription() != null)
                holder.description.setText(subscription.getDescription().trim());
            else
                holder.description.setText(R.string.description_not_available);
			holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), subscription.getId()));
            holder.more.setTag(subscription.getId());
		}

        @Override
        public long getItemId(int position) {
            return new SubscriptionCursor((Cursor) getItem(position)).getId();
        }
    }
}
