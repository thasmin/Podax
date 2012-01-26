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
import android.util.Log;
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
	static final int OPTION_MOVETOFIRSTINQUEUE = 3;

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
				
				menu.add(ContextMenu.NONE, OPTION_MOVETOFIRSTINQUEUE,
						ContextMenu.NONE, R.string.move_to_first_in_queue);
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
		case OPTION_MOVETOFIRSTINQUEUE:
			podcast.moveToFirstInQueue();
			break;
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

			for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); ++i)
			{
				Rect bounds = new Rect();
				listView.getChildAt(i - listView.getFirstVisiblePosition()).getHitRect(bounds);
				dragLog(String.format("position %d: top: %d, bottom: %d, height %d, centerY: %d", i, bounds.top, bounds.bottom, bounds.height(), bounds.centerY()));
			}
			dragLog(String.format("pointing to y %f, position %d", event.getY(), position));
			View listItem = listView.getChildAt(position - listView.getFirstVisiblePosition());
			// no listview means we're below the last one
			if (listItem == null) {
				dragLogEnd(String.format("moving to last position: %d", getListAdapter().getCount()));
				adapter.setQueuePosition(getListAdapter().getCount());
				return true;
			}

			// don't change anything if we're hovering over this one
			if (position == adapter.getHeldQueuePosition()) {
				dragLogEnd("hovering over held podcast");
				return true;
			}

			// remove hidden podcast from ordering
			dragLog(String.format("comparing position %d and geld position %d", position, adapter.getHeldQueuePosition()));
			if (position >= adapter.getHeldQueuePosition()) {
				dragLog("subtracting 1 because we're past held");
				position -= 1;
			}

			// move podcast to proper position
			Rect bounds = new Rect();
			listItem.getHitRect(bounds);
			dragLog(String.format("height: %d, centerY: %d, eventY: %f", bounds.height(), bounds.centerY(), event.getY()));
			// if pointer is in top half of item then put separator above,
			// otherwise below
			if (event.getY() > bounds.centerY())
				position += 1;
			dragLogEnd(String.format("moving to position %d", position));
			adapter.setQueuePosition(position);
			return true;
		}
		return false;
	}

	private final boolean logDragMessages = false;
	private void dragLog(String message) {
		if (logDragMessages) {
			Log.d("Podax", message);
		}
	}
	private void dragLogEnd(String message) {
		if (logDragMessages) {
			Log.d("Podax", message);
			Log.d("Podax", " ");
		}
	}

	private class QueueListAdapter extends ResourceCursorAdapter {

		private OnTouchListener downListener = new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					int position = getListView().getPositionForView(view);

					position -= getListView().getFirstVisiblePosition();
					dragLog(String.format("holding podcast at position %d", position));
					QueueListAdapter.this.holdPodcast(position);
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

		public void holdPodcast(int position) {
			_heldQueuePosition = position;
			_heldPodcastId = (Long)getListView().getChildAt(position).getTag();
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
			heldValues.put(PodcastProvider.COLUMN_QUEUE_POSITION, position + 1);
			Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _heldPodcastId);
			getContentResolver().update(podcastUri, heldValues, null, null);
			_heldQueuePosition = position;
			
			getCursor().close();
			changeCursor(managedQuery(queueUri, projection, null, null, null));
		}
	}
	
}
