package com.axelby.podax;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class QueueActivity extends ListActivity implements OnTouchListener {
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;

	Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
	String[] projection = new String[] {
		PodcastProvider.COLUMN_ID,
		PodcastProvider.COLUMN_TITLE,
		PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
		PodcastProvider.COLUMN_QUEUE_POSITION,
		PodcastProvider.COLUMN_MEDIA_URL,
		PodcastProvider.COLUMN_FILE_SIZE,
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.queue);

		Cursor cursor = managedQuery(queueUri, projection, null, null, null);
		setListAdapter(new QueueListAdapter(this, cursor));

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(QueueActivity.this, PodcastDetailActivity.class);
		    	intent.putExtra("com.axelby.podax.podcastId", (int)id);
		    	startActivity(intent);
			}
		});
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
				Cursor c = (Cursor) getListAdapter().getItem(mi.position);
				PodcastCursor podcast = new PodcastCursor(QueueActivity.this, c); 
				
				menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE,
						ContextMenu.NONE, R.string.remove_from_queue);
				
				if (podcast.isDownloaded())
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE,
							R.string.play);
			}
		});
		
		getListView().setOnTouchListener(this);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		PodcastCursor podcast = new PodcastCursor(this, cursor);

		switch (item.getItemId()) {
		case OPTION_REMOVEFROMQUEUE:
			podcast.removeFromQueue();
			break;
		case OPTION_PLAY:
			PodaxApp.getApp().play(podcast);
		}

		return true;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		QueueListAdapter adapter = (QueueListAdapter)getListAdapter();
		if (adapter.getHeldPodcastId() == null)
			return false;
		
		ListView listView = getListView();
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			adapter.unholdPodcast();
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			int position = listView.pointToPosition((int) event.getX(),
					(int) event.getY());
			if (position == -1)
				return true;

			View listItem = listView.getChildAt(position - listView.getFirstVisiblePosition());
			// no listview means we're below the last one
			if (listItem == null) {
				adapter.setQueuePosition(getListAdapter().getCount());
				return true;
			}

			Long podcastId = (Long)listItem.getTag();

			// don't change anything if we're hovering over this one
			if (podcastId == adapter.getHeldPodcastId())
				return true;

			// remove hidden podcast from ordering
			if (position > adapter.getHeldQueuePosition())
				position -= 1;

			// if pointer is in top half of item then put separator above,
			// otherwise below
			Rect bounds = new Rect();
			listItem.getHitRect(bounds);
			if (event.getY() > (bounds.top + bounds.bottom) / 2.0f)
				position += 1;
			adapter.setQueuePosition(position);
				return true;
		}
		return false;
	}

	private class QueueListAdapter extends ResourceCursorAdapter {

		private OnTouchListener downListener = new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					int position = getListView().getPositionForView(view);

					position -= getListView().getFirstVisiblePosition();
					ViewSwitcher switcher = (ViewSwitcher)getListView().getChildAt(position);
					QueueListAdapter.this.holdPodcast(switcher.getTag());
					//switcher.showNext();
					return true;
				}
				return false;
			}
		};

		private Long _heldPodcastId = null;
		private int _heldQueuePosition;

		public QueueListAdapter(Context context, Cursor cursor) {
			super(context, R.layout.queue_list_item, cursor);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewSwitcher switcher = (ViewSwitcher)view;
			switcher.setMeasureAllChildren(false);

			View btn = view.findViewById(R.id.dragable);
			btn.setOnTouchListener(downListener);

			PodcastCursor podcast = new PodcastCursor(QueueActivity.this,
					cursor);
			view.setTag(podcast.getId());
			TextView queueText = (TextView) view.findViewById(R.id.title);
			queueText.setText(podcast.getTitle());

			TextView subscriptionText = (TextView) view
					.findViewById(R.id.subscription);
			subscriptionText.setText(podcast.getSubscriptionTitle());

			if (_heldPodcastId != null &&
					(long)podcast.getId() == (long)_heldPodcastId &&
					switcher.getDisplayedChild() == 0)
				switcher.showNext();
			if ((_heldPodcastId == null || (long)podcast.getId() != (long)_heldPodcastId) &&
					switcher.getDisplayedChild() == 1)
				switcher.showPrevious();
		}

		public void holdPodcast(Object podcastId) {
			String[] projection = {
					PodcastProvider.COLUMN_ID,
					PodcastProvider.COLUMN_QUEUE_POSITION,
			};
			Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, (Long)podcastId);
			Cursor held = getContentResolver().query(podcastUri, projection, null, null, null);
			if (!held.moveToNext())
				return;
			_heldPodcastId = (Long)podcastId;
			_heldQueuePosition = held.getInt(held.getColumnIndex(PodcastProvider.COLUMN_QUEUE_POSITION));
			held.close();
			notifyDataSetChanged();
		}
		
		public void unholdPodcast() {
			_heldPodcastId = null;
			notifyDataSetChanged();
		}
		
		public Long getHeldPodcastId() {
			return _heldPodcastId;
		}

		public int getHeldQueuePosition() {
			return _heldQueuePosition;
		}

		public void setQueuePosition(int position) {
			if (_heldPodcastId == null || _heldQueuePosition == position)
				return;

			// update the held cursor to have the new queue position
			// the queue will automatically reorder
			ContentValues heldValues = new ContentValues();
			heldValues.put(PodcastProvider.COLUMN_QUEUE_POSITION, position);
			Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _heldPodcastId);
			getContentResolver().update(podcastUri, heldValues, null, null);
			_heldQueuePosition = position;
			
			getCursor().close();
			changeCursor(managedQuery(queueUri, projection, null, null, null));
		}
	}
	
}
