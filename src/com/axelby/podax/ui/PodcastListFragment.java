package com.axelby.podax.ui;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.axelby.podax.Constants;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;
import com.axelby.podax.UpdateService;

public class PodcastListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	static final int OPTION_ADDTOQUEUE = 3;
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;

	private PodcastAdapter _adapter = null;
	private int _subscriptionId = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		_subscriptionId = getActivity().getIntent().getIntExtra("subscriptionId", 0);
		getLoaderManager().initLoader(0, null, this);
		_adapter = new PodcastAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	public void setSubscriptionId(int id) {
		_subscriptionId = id;
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.podcastlist_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
				Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
				PodcastCursor podcast = new PodcastCursor(cursor);

				if (podcast.getQueuePosition() == null)
					menu.add(ContextMenu.NONE, OPTION_ADDTOQUEUE,
							ContextMenu.NONE, R.string.add_to_queue);
				else
					menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE,
							ContextMenu.NONE, R.string.remove_from_queue);

				if (podcast.isDownloaded())
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE,
							R.string.play);
			}
		});
	}

	CharSequence _originalTitle = null;
	public boolean setTitle() {
		if (_originalTitle == null)
			_originalTitle = getActivity().getTitle();

		if (_subscriptionId == 0) {
			getActivity().setTitle(_originalTitle);
			return true;
		}

		// set the title before loading the layout
		Uri subscriptionUri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);
		String[] subscriptionProjection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
		};
		Cursor subscriptionCursor = getActivity().getContentResolver().query(subscriptionUri, subscriptionProjection, null, null, null);
		if (!subscriptionCursor.moveToNext()) {
			return false;
		}
		SubscriptionCursor subscription = new SubscriptionCursor(subscriptionCursor);
		try {
			getActivity().setTitle(subscription.getTitle() + " Podcasts");
		} finally {
			subscriptionCursor.close();
		}
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
		PodcastCursor podcast = new PodcastCursor(cursor);

		switch (item.getItemId()) {
		case OPTION_ADDTOQUEUE:
			podcast.addToQueue(getActivity());
			break;
		case OPTION_REMOVEFROMQUEUE:
			podcast.removeFromQueue(getActivity());
			break;
		case OPTION_PLAY:
			PlayerService.play(getActivity(), podcast);
		}

		return true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.podcast_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh_subscription:
			UpdateService.updateSubscription(getActivity(), _subscriptionId);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		Intent intent = new Intent(getActivity(), PodcastDetailActivity.class);
		Cursor cursor = (Cursor) list.getItemAtPosition(position);
		PodcastCursor podcast = new PodcastCursor(cursor);
		intent.putExtra(Constants.EXTRA_PODCAST_ID, (int)(long)podcast.getId());
		startActivity(intent);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);
		uri = Uri.withAppendedPath(uri, "podcasts");
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_FILE_SIZE,
				PodcastProvider.COLUMN_QUEUE_POSITION,
		};
		return new CursorLoader(getActivity(), uri, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
		setTitle();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		_adapter.changeCursor(null);
		setTitle();
	}

	private class PodcastAdapter extends ResourceCursorAdapter {
		public PodcastAdapter(Context context, Cursor cursor) {
			super(context, R.layout.list_item, cursor, true);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView textview = (TextView)view;
			String podcastTitle = new PodcastCursor(cursor).getTitle();
			textview.setText(podcastTitle);
		}
	}
}
