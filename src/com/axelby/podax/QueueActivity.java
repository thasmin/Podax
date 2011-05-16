package com.axelby.podax;

import java.util.Vector;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class QueueActivity extends PlayerActivity {
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;

	private ListView _view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.queue);

		super.onCreate(savedInstanceState);

		_view = (ListView) findViewById(R.id.list);
		_view.setAdapter(new QueueListAdapter());
		_view.setEmptyView(findViewById(R.id.empty));
		_view.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfo;
				Podcast p = (Podcast) _view.getItemAtPosition(mi.position);
				
				menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE,
						ContextMenu.NONE, "Remove from Queue");
				
				if (p.isDownloaded())
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE,
							"Play");
			}
		});
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		DBAdapter adapter = DBAdapter.getInstance(this);
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		Podcast podcast = (Podcast)_view.getAdapter().getItem(info.position);

		switch (item.getItemId()) {
		case OPTION_REMOVEFROMQUEUE:
			adapter.removePodcastFromQueue(podcast.getId());
			_view.setAdapter(new QueueListAdapter());
			break;
		case OPTION_PLAY:
			PodaxApp.getApp().playPodcast(podcast);
		}

		return true;
	}

	private class QueueListAdapter extends BaseAdapter {
		private Vector<Podcast> _queue;
		private LayoutInflater _layoutInflater;
		
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
			LinearLayout view;
			if (convertView == null) {
				view = (LinearLayout)_layoutInflater.inflate(R.layout.queue_list_item, null);
			}
			else {
				view = (LinearLayout)convertView;
			}
			
			Podcast p = _queue.get(position);
			
			TextView queueText = (TextView)view.findViewById(R.id.title);
			queueText.setText(p.getTitle());
			
			TextView subscriptionText = (TextView)view.findViewById(R.id.subscription);
			subscriptionText.setText(p.getSubscription().getTitle());
			
			return view;
		}

	}
	
}
