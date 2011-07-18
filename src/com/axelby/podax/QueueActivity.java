package com.axelby.podax;

import java.util.Vector;

import android.app.ListActivity;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.queue);

		setListAdapter(new QueueListAdapter());
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfo;
				Podcast p = (Podcast) getListView().getItemAtPosition(mi.position);
				
				menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE,
						ContextMenu.NONE, R.string.remove_from_queue);
				
				if (p.isDownloaded())
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE,
							R.string.play);
			}
		});
		
		PlayerActivity.injectPlayerFooter(this);
		
		getListView().setOnTouchListener(this);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		DBAdapter adapter = DBAdapter.getInstance(this);
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		Podcast podcast = (Podcast)getListAdapter().getItem(info.position);

		switch (item.getItemId()) {
		case OPTION_REMOVEFROMQUEUE:
			adapter.removePodcastFromQueue(podcast.getId());
			setListAdapter(new QueueListAdapter());
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
			try {
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					int position = getListView().getPositionForView(_queueItemView);
					((QueueListAdapter)getListAdapter()).setSeparatorAt(position);
					return true;
				}
				return false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	};
	
	public boolean onTouch(View v, MotionEvent event) {
		try {
			if (((QueueListAdapter)getListAdapter()).getSeparatorAt() == -1)
				return false;
			
			ListView listView = getListView();
			if (event.getAction() == MotionEvent.ACTION_UP)
			{
				QueueListAdapter adapter = (QueueListAdapter)getListAdapter();
				DBAdapter.getInstance(this).changePodcastQueuePosition(adapter.getHeldPodcast(), adapter.getSeparatorAt());
				adapter.removeSeparator();
	
				return true;
			}
			if (event.getAction() == MotionEvent.ACTION_MOVE)
			{
				int position = listView.pointToPosition((int)event.getX(), (int)event.getY());
				QueueListAdapter adapter = (QueueListAdapter)getListAdapter();
				adapter.setSeparatorAt(position);
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private class QueueListAdapter extends BaseAdapter {
		private Vector<Podcast> _queue;
		private LayoutInflater _layoutInflater;
		private Podcast _heldPodcast;
		
		public QueueListAdapter() {
			_layoutInflater = LayoutInflater.from(QueueActivity.this);
			
			DBAdapter dbAdapter = DBAdapter.getInstance(QueueActivity.this);
			
			_queue = new Vector<Podcast>();
			for (int id : dbAdapter.getQueueIds())
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
			Podcast p = _queue.get(position);
			if (p == _heldPodcast) {
				return _layoutInflater.inflate(R.layout.separator, null);
			} else {
				View view = _layoutInflater.inflate(R.layout.queue_list_item, null);
				View btn = view.findViewById(R.id.dragable);
				btn.setOnTouchListener(new DownListener(view));
				updateListItemView(view, p);
				return view;
			}
		}

		private void updateListItemView(View view, Podcast p) {
			TextView queueText = (TextView)view.findViewById(R.id.title);
			queueText.setText(p.getTitle());
			
			TextView subscriptionText = (TextView)view.findViewById(R.id.subscription);
			subscriptionText.setText(p.getSubscription().getTitle());
		}
		
		public int getSeparatorAt() {
			return _heldPodcast == null ? -1 : _queue.indexOf(_heldPodcast);
		}

		public void setSeparatorAt(int position) {
			if (_heldPodcast == null) {
				// save the one at current
				_heldPodcast = _queue.get(position);
			} else {
				int heldAt = _queue.indexOf(_heldPodcast);

				// don't include the separator
				if (position == AdapterView.INVALID_POSITION)
					position = getCount() - 1;
				else {
					if (position >= heldAt)
						position -= 1;
				}
				
				// swap the held podcast with the one at the new position
				_queue.set(heldAt, _queue.get(position));
				_queue.set(position, _heldPodcast);
			}
			this.notifyDataSetChanged();
		}
		
		public void removeSeparator() {
			_heldPodcast = null;
			this.notifyDataSetChanged();
		}
		
		public Podcast getHeldPodcast() {
			return _heldPodcast;
		}
	}
	
}
