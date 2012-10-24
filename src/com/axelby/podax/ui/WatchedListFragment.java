package com.axelby.podax.ui;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

public class WatchedListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private final int MENU_REMOVE_WATCH = 900;

	private WatchedAdapter _adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new WatchedAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, null, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(0, MENU_REMOVE_WATCH, 0, R.string.remove_watch);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REMOVE_WATCH:
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
				SubscriptionProvider.COLUMN_URL
		};
		return new CursorLoader(getActivity(), SubscriptionProvider.WATCHED_URI, projection, null, null, null);
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

	private class WatchedAdapter extends ResourceCursorAdapter {
		public WatchedAdapter(Context context, Cursor cursor) {
			super(context, R.layout.subscription_list_item, cursor, true);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			SubscriptionCursor subscription = new SubscriptionCursor(cursor);

			TextView text = (TextView)view.findViewById(R.id.text);
			ImageView thumbnail = (ImageView)view.findViewById(R.id.thumbnail);

			text.setText(subscription.getTitle());

			File thumbnailFile = new File(subscription.getThumbnailFilename());
			if (!thumbnailFile.exists())
				thumbnail.setImageDrawable(null);
			else
			{
				try {
					thumbnail.setImageBitmap(BitmapFactory.decodeFile(subscription.getThumbnailFilename()));
					thumbnail.setVisibility(1);
				} catch (OutOfMemoryError ex) {
					thumbnail.setImageDrawable(null);
				}
			}

			// more button handler
			view.findViewById(R.id.more).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					getActivity().openContextMenu((View)(view.getParent()));
				}
			});
		}
	}
}