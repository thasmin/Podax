package com.axelby.podax;

import java.util.Vector;

import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class QueueActivity extends ListActivity implements OnTouchListener {
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;

	private ListView _listView;
	private Podcast _draggedPodcast;
	private int _droppedPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.queue);

		_listView = getListView();
		_listView.setAdapter(new QueueListAdapter());
		_listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfo;
				Podcast p = (Podcast) _listView.getItemAtPosition(mi.position);
				
				menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE,
						ContextMenu.NONE, "Remove from Queue");
				
				if (p.isDownloaded())
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE,
							"Play");
			}
		});
		
		PlayerActivity.injectPlayerFooter(this);
		
		_listView.setOnTouchListener(this);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		DBAdapter adapter = DBAdapter.getInstance(this);
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		Podcast podcast = (Podcast)_listView.getAdapter().getItem(info.position);

		switch (item.getItemId()) {
		case OPTION_REMOVEFROMQUEUE:
			adapter.removePodcastFromQueue(podcast.getId());
			_listView.setAdapter(new QueueListAdapter());
			break;
		case OPTION_PLAY:
			PodaxApp.getApp().playPodcast(podcast);
		}

		return true;
	}
	
	private class DownListener implements OnTouchListener {
		View _queueItemView;
		public DownListener(View v) {
			_queueItemView = v;
		}
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				int position = _listView.getPositionForView(_queueItemView);
				Podcast p = (Podcast)_listView.getItemAtPosition(position);
				_draggedPodcast = p;
				QueueListAdapter adapter = (QueueListAdapter)getListView().getAdapter();
				adapter.removePodcast(_draggedPodcast);
				adapter.notifyDataSetChanged();
				return true;
			}
			return false;
		}
	};
	
	public boolean onTouch(View v, MotionEvent event) {
		if (_draggedPodcast == null)
			return false;
		
		ListView listView = getListView();
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			if (_droppedPosition == AdapterView.INVALID_POSITION)
				_droppedPosition = listView.getAdapter().getCount();
			DBAdapter.getInstance(this).changePodcastQueuePosition(_draggedPodcast, _droppedPosition);
			_draggedPodcast.setQueuePosition(_droppedPosition);
			
			_draggedPodcast = null;
			_droppedPosition = AdapterView.INVALID_POSITION;
			listView.setAdapter(new QueueListAdapter());

			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			if (_droppedPosition != AdapterView.INVALID_POSITION)
				listView.getChildAt(_droppedPosition).setBackgroundColor(Color.TRANSPARENT);
			_droppedPosition = listView.pointToPosition((int)event.getX(), (int)event.getY());
			if (_droppedPosition == AdapterView.INVALID_POSITION)
				return true;
			// TODO: tell adapter to put the separator before _droppedposition and redraw
			View before = listView.getChildAt(_droppedPosition);
			before.setBackgroundColor(Color.BLUE);
			return true;
		}
		return false;
	}

	private class QueueListAdapter extends BaseAdapter {
		private Vector<Podcast> _queue;
		private LayoutInflater _layoutInflater;
		
		public QueueListAdapter() {
			_layoutInflater = LayoutInflater.from(QueueActivity.this);
			
			DBAdapter dbAdapter = DBAdapter.getInstance(QueueActivity.this);
			
			_queue = new Vector<Podcast>();
			for (int id : dbAdapter.getQueueIds())
				if (_draggedPodcast == null || _draggedPodcast.getId() != id)
					_queue.add(dbAdapter.loadPodcast(id));
		}

		public int getCount() {
			return _queue.size();
		}

		public Object getItem(int position) {
			return _queue.get(position);
		}

		public long getItemId(int position) {
			return _queue.get(position).getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = _layoutInflater.inflate(R.layout.queue_list_item, null);
				View btn = view.findViewById(R.id.dragable);
				btn.setOnTouchListener(new DownListener(view));
			}
			else {
				view = convertView;
			}
			
			Podcast p = _queue.get(position);
			
			TextView queueText = (TextView)view.findViewById(R.id.title);
			queueText.setText(p.getTitle());
			
			TextView subscriptionText = (TextView)view.findViewById(R.id.subscription);
			subscriptionText.setText(p.getSubscription().getTitle());
			
			return view;
		}
		
		public void removePodcast(Podcast p) {
			_queue.removeElement(p);
		}
	}
	
}
