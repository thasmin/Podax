package com.axelby.podax;

import java.io.File;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.SherlockListFragment;

public class QueueFragment extends SherlockListFragment implements OnTouchListener, LoaderManager.LoaderCallbacks<Cursor> {
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;
	static final int OPTION_MOVETOFIRSTINQUEUE = 3;

	QueueListAdapter _adapter;
	Uri queueUri = Uri.withAppendedPath(PodcastProvider.URI, "queue");
	String[] projection = new String[] {
		PodcastProvider.COLUMN_ID,
		PodcastProvider.COLUMN_TITLE,
		PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
		PodcastProvider.COLUMN_QUEUE_POSITION,
		PodcastProvider.COLUMN_MEDIA_URL,
		PodcastProvider.COLUMN_FILE_SIZE,
		PodcastProvider.COLUMN_SUBSCRIPTION_ID,
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);
		_adapter = new QueueListAdapter(getActivity(), null);

		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.queue, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(getActivity(), PodcastDetailActivity.class);
				intent.putExtra(Constants.EXTRA_PODCAST_ID, (int)id);
				startActivity(intent);
			}
		});
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
				Cursor c = (Cursor) getListAdapter().getItem(mi.position);
				PodcastCursor podcast = new PodcastCursor(getActivity(), c);

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
		PodcastCursor podcast = new PodcastCursor(getActivity(), cursor);

		switch (item.getItemId()) {
		case OPTION_MOVETOFIRSTINQUEUE:
			podcast.moveToFirstInQueue();
			break;
		case OPTION_REMOVEFROMQUEUE:
			podcast.removeFromQueue();
			break;
		case OPTION_PLAY:
			PlayerService.play(getActivity(), podcast);
		}

		return true;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), queueUri, projection, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		_adapter.changeCursor(null);
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
			super(context, R.layout.queue_list_item, cursor, true);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewSwitcher switcher = (ViewSwitcher)view;
			switcher.setMeasureAllChildren(false);

			View btn = view.findViewById(R.id.dragable);
			btn.setOnTouchListener(downListener);

			PodcastCursor podcast = new PodcastCursor(getActivity(), cursor);
			view.setTag(podcast.getId());
			TextView queueText = (TextView) view.findViewById(R.id.title);
			queueText.setText(podcast.getTitle());

			TextView subscriptionText = (TextView) view
					.findViewById(R.id.subscription);
			subscriptionText.setText(podcast.getSubscriptionTitle());

			ImageView thumbnail = (ImageView)view.findViewById(R.id.thumbnail);

			File thumbnailFile = new File(podcast.getThumbnailFilename());
			if (!thumbnailFile.exists())
				thumbnail.setImageDrawable(null);
			else
			{
				thumbnail.setImageBitmap(BitmapFactory.decodeFile(podcast.getThumbnailFilename()));
				thumbnail.setVisibility(1);
			}

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
			heldValues.put(PodcastProvider.COLUMN_QUEUE_POSITION, position);
			Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, _heldPodcastId);
			getActivity().getContentResolver().update(podcastUri, heldValues, null, null);
			_heldQueuePosition = position;
		}
	}

}
