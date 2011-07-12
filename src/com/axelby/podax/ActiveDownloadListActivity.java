package com.axelby.podax;

import java.util.Vector;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ActiveDownloadListActivity extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.downloads_list);
		
		setListAdapter(new ActiveDownloadAdapter());
		
		PlayerActivity.injectPlayerFooter(this);
	}

	public class ActiveDownloadAdapter extends BaseAdapter {
		Podcast _active = null;
		Vector<Podcast> _waiting = new Vector<Podcast>();
		LayoutInflater _layoutInflater;
		
		public ActiveDownloadAdapter() {
			super();
			
			_layoutInflater = LayoutInflater.from(ActiveDownloadListActivity.this);
			
			DBAdapter dbAdapter = DBAdapter.getInstance(ActiveDownloadListActivity.this);
			Integer activeId = dbAdapter.getActiveDownloadId();
			
			Vector<Integer> toProcess = dbAdapter.getQueueIds();
			for (Integer podcastId : toProcess) {
				Podcast podcast = dbAdapter.loadPodcast(podcastId);

				if (podcastId == activeId) {
					_active = podcast;
					continue;
				}

				if (podcast.needsDownload())
					_waiting.add(podcast);
			}
		}

		public int getCount() {
			return _active != null ? 1 : 0 + _waiting.size();
		}

		public Object getItem(int position) {
			if (_active == null)
				return _waiting.get(position);
			if (position == 0)
				return _active;
			return _waiting.get(position - 1);
		}

		public long getItemId(int position) {
			return ((Podcast)getItem(position)).getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			Podcast podcast = (Podcast)getItem(position);
			
			View view;
			if (convertView == null) {
				view = _layoutInflater.inflate(R.layout.downloads_list_item, null);
			}
			else {
				view = convertView;
			}
			
			TextView title = (TextView)view.findViewById(R.id.title); 
			title.setText(podcast.getTitle());
			TextView subscription = (TextView)view.findViewById(R.id.subscription); 
			subscription.setText(podcast.getSubscription().getDisplayTitle());
			
			return view;
		}

	}
}
