package com.axelby.podax;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class PodcastListActivity extends ListActivity {
	static final int OPTION_ADDTOQUEUE = 3;
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;

	private int _subscriptionId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();
		_subscriptionId = intent.getIntExtra("subscriptionId", -1);
		if (_subscriptionId == -1) {
			finish();
			return;
		}

		if (!setTitle()) {
			finish();
			return;
		}
		setContentView(R.layout.podcast_list);

		Uri uri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);
		uri = Uri.withAppendedPath(uri, "podcasts");
		String[] projection = {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_FILE_SIZE,
				PodcastProvider.COLUMN_QUEUE_POSITION,
		};
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		getListView().setAdapter(new PodcastAdapter(this, cursor));

		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
				Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
				PodcastCursor podcast = new PodcastCursor(PodcastListActivity.this, cursor);

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

	public boolean setTitle() {
		// set the title before loading the layout
		Uri subscriptionUri = ContentUris.withAppendedId(SubscriptionProvider.URI, _subscriptionId);
		String[] subscriptionProjection = {
				SubscriptionProvider.COLUMN_ID,
				SubscriptionProvider.COLUMN_TITLE,
				SubscriptionProvider.COLUMN_URL,
		};
		Cursor subscriptionCursor = getContentResolver().query(subscriptionUri, subscriptionProjection, null, null, null);
		if (!subscriptionCursor.moveToNext()) {
			return false;
		}
		SubscriptionCursor subscription = new SubscriptionCursor(this, subscriptionCursor);
		try {
			setTitle(subscription.getTitle() + " Podcasts");
		} finally {
			subscriptionCursor.close();
		}
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
		PodcastCursor podcast = new PodcastCursor(this, cursor);

		switch (item.getItemId()) {
		case OPTION_ADDTOQUEUE:
			podcast.addToQueue();
			break;
		case OPTION_REMOVEFROMQUEUE:
			podcast.removeFromQueue();
			break;
		case OPTION_PLAY:
			PodaxApp.play(this, podcast);
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.podcast_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh_subscription:
			UpdateService.updateSubscription(this, _subscriptionId);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		Intent intent = new Intent(this, PodcastDetailActivity.class);
		Cursor cursor = (Cursor) list.getItemAtPosition(position);
		PodcastCursor podcast = new PodcastCursor(this, cursor);
		intent.putExtra(Constants.EXTRA_PODCAST_ID, (int)(long)podcast.getId());
		startActivity(intent);
	}

	private class PodcastAdapter extends ResourceCursorAdapter {
		public PodcastAdapter(Context context, Cursor cursor) {
			super(context, R.layout.list_item, cursor);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView textview = (TextView)view;
			String podcastTitle = new PodcastCursor(context, cursor).getTitle();
			textview.setText(podcastTitle);
		}
	}
}
