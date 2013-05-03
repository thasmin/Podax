package com.axelby.podax.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.androidquery.AQuery;
import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

public class SubscriptionListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private SubscriptionAdapter _adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, null, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.subscription_list, menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(0, 0, 0, R.string.unsubscribe);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			Cursor cursor = (Cursor) getListAdapter().getItem(menuInfo.position);
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);
			getActivity().getContentResolver().delete(subscription.getContentUri(), null, null);
			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Fragment podcastList = getFragmentManager().findFragmentById(R.id.podcastlist_fragment);
		SubscriptionCursor sub = new SubscriptionCursor((Cursor)list.getItemAtPosition(position));
		long subscriptionId = sub.getId();
		if (podcastList == null) {
			// no need to check the item if it's not side by side
			list.clearChoices();

			Intent intent = new Intent(getActivity(), PodcastListActivity.class);
			intent.putExtra(Constants.EXTRA_SUBSCRIPTION_ID, subscriptionId);
			startActivity(intent);
		} else {
			PodcastListFragment podcastListFragment = (PodcastListFragment) podcastList;
			podcastListFragment.setSubscriptionId(subscriptionId);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
				SubscriptionProvider.COLUMN_THUMBNAIL,
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
		public SubscriptionAdapter(Context context, Cursor cursor) {
			super(context, R.layout.subscription_list_item, cursor, true);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);

			AQuery aq = new AQuery(view);
			aq.find(R.id.text).text(subscription.getTitle());
			aq.find(R.id.thumbnail).image(subscription.getThumbnail(), new QueueFragment.ImageOptions());
			aq.find(R.id.more).clicked(new OnClickListener() {
				@Override
				public void onClick(View view) {
					getActivity().openContextMenu((View)(view.getParent()));
				}
			});
		}
	}
}