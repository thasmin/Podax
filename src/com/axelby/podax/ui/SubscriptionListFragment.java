package com.axelby.podax.ui;

import java.io.File;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class SubscriptionListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private SubscriptionAdapter _adapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new SubscriptionAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.subscription_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Intent intent = getActivity().getIntent();
		// check if this was opened by android to save an RSS feed
		if (intent.getDataString() != null) {
			ContentValues values = new ContentValues();
			values.put(SubscriptionProvider.COLUMN_URL, intent.getDataString());
			Uri savedSubscription = getActivity().getContentResolver().insert(SubscriptionProvider.URI, values);
			UpdateService.updateSubscription(getActivity(), Integer.valueOf(savedSubscription.getLastPathSegment()));
		}

		registerForContextMenu(getListView());

		// remove any subscription update errors
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(ns);
		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);
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
			SubscriptionCursor subscription = new SubscriptionCursor(getActivity(), cursor);
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
		SubscriptionCursor sub = new SubscriptionCursor(getActivity(), (Cursor)list.getItemAtPosition(position));
		int subscriptionId = (int)(long)sub.getId();
		if (podcastList == null || !podcastList.isInLayout()) {
			Intent intent = new Intent(getActivity(), PodcastListActivity.class);
			intent.putExtra("subscriptionId", subscriptionId);
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
		return new CursorLoader(getActivity(), SubscriptionProvider.URI, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
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
			SubscriptionCursor subscription = new SubscriptionCursor(context, cursor);

			TextView text = (TextView)view.findViewById(R.id.text);
			ImageView thumbnail = (ImageView)view.findViewById(R.id.thumbnail);

			text.setText(subscription.getTitle());

			File thumbnailFile = new File(subscription.getThumbnailFilename());
			if (!thumbnailFile.exists())
				thumbnail.setImageDrawable(null);
			else
			{
				thumbnail.setImageBitmap(BitmapFactory.decodeFile(subscription.getThumbnailFilename()));
				thumbnail.setVisibility(1);
			}
		}
	}
}