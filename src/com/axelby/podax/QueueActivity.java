package com.axelby.podax;

import android.app.ListActivity;
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

	Uri queueURI = Uri.withAppendedPath(PodcastProvider.URI, "queue");
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

		Cursor cursor = managedQuery(queueURI, projection, null, null, null);
		cursor.setNotificationUri(getContentResolver(), queueURI);
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
				
				try {
					if (podcast.isDownloaded())
						menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE,
								R.string.play);
				} catch (MissingFieldException e) {
					e.printStackTrace();
				}
			}
		});
		
		getListView().setOnTouchListener(this);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		PodcastCursor podcast = new PodcastCursor(this, cursor);

		try {
			switch (item.getItemId()) {
			case OPTION_REMOVEFROMQUEUE:
				podcast.removeFromQueue();
				break;
			case OPTION_PLAY:
				PodaxApp.getApp().play(podcast);
			}
		} catch (MissingFieldException e) {
			e.printStackTrace();
		}

		return true;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		QueueListAdapter adapter = (QueueListAdapter)getListAdapter();
		if (adapter.getHeldPodcastIndex() == -1)
			return false;
		
		ListView listView = getListView();
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			ViewSwitcher switcher = (ViewSwitcher) listView.getChildAt(adapter.getHeldPodcastIndex());
			switcher.showPrevious();
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			try {
				int position = listView.pointToPosition((int) event.getX(),
						(int) event.getY());
				View listItem = listView.getChildAt(position);

				// don't change anything if we're hovering over this one
				if (position == adapter.getHeldPodcastIndex())
					return true;

				// no listview means we're below the last one
				if (listItem == null) {
					adapter.setSeparatorAt(listView.getChildCount());
					return true;
				}

				// remove hidden podcast from ordering
				if (position > adapter.getHeldPodcastIndex())
					position -= 1;

				// if pointer is in top half of item then put separator above,
				// otherwise below
				Rect bounds = new Rect();
				listItem.getHitRect(bounds);
				if (event.getY() > (bounds.top + bounds.bottom) / 2.0f)
					position += 1;
				adapter.setSeparatorAt(position);
				return true;
			} catch (MissingFieldException e) {
				e.printStackTrace();
			} finally {
			}
		}
		return false;
	}

	private class QueueListAdapter extends ResourceCursorAdapter {

		private OnTouchListener downListener = new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					int position = getListView().getPositionForView(view);
					QueueListAdapter.this.holdPodcastAt(position);

					ViewSwitcher switcher = (ViewSwitcher)getListView().getChildAt(position);
					switcher.showNext();
					return true;
				}
				return false;
			}
		};

		private int _heldPosition = -1;
		
		public QueueListAdapter(Context context, Cursor cursor) {
			super(context, R.layout.queue_list_item, cursor);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewSwitcher switcher = (ViewSwitcher)view;
			switcher.setMeasureAllChildren(false);

			View btn = view.findViewById(R.id.dragable);
			btn.setOnTouchListener(downListener);

			try {
				PodcastCursor podcast = new PodcastCursor(QueueActivity.this,
						cursor);
				TextView queueText = (TextView) view.findViewById(R.id.title);
				queueText.setText(podcast.getTitle());

				TextView subscriptionText = (TextView) view
						.findViewById(R.id.subscription);
				subscriptionText.setText(podcast.getSubscriptionTitle());
			} catch (MissingFieldException e) {
				e.printStackTrace();
			}
		}

		public void holdPodcastAt(int position) {
			_heldPosition = position;
		}
		
		public int getHeldPodcastIndex() {
			return _heldPosition;
		}

		public void setSeparatorAt(int position) throws MissingFieldException {
			if (_heldPosition == -1 || _heldPosition == position)
				return;

			// update the held cursor to have the new queue position
			// the queue will automatically reorder
			Cursor held = (Cursor)getItem(_heldPosition);
			PodcastCursor heldPodcast = new PodcastCursor(QueueActivity.this, held);
			ContentValues heldValues = new ContentValues();
			heldValues.put(PodcastProvider.COLUMN_QUEUE_POSITION, position);
			getContentResolver().update(heldPodcast.getContentUri(), heldValues, null, null);
			
			ViewSwitcher heldView = (ViewSwitcher)getListView().getChildAt(position);
			heldView.showNext();
			
			ViewSwitcher switchedView = (ViewSwitcher)getListView().getChildAt(_heldPosition);
			switchedView.showNext();

			_heldPosition = position;
		}
	}
	
}
