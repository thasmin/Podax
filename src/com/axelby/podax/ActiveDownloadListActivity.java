package com.axelby.podax;

import java.io.File;
import java.util.Vector;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ActiveDownloadListActivity extends ListActivity {
	Runnable refresher;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Runnable refresher = new Runnable() {
			public void run() {
				setListAdapter(new ActiveDownloadAdapter());
				new Handler().postDelayed(this, 1000);
			}
		};

		setContentView(R.layout.downloads_list);

		refresher.run();
		
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
			
			Vector<Podcast> toProcess = dbAdapter.getQueue();
			for (Podcast podcast : toProcess) {
				if (!podcast.needsDownload())
					continue;

				if (activeId != null && podcast.getId() == activeId) {
					_active = podcast;
				}

				_waiting.add(podcast);
			}
		}

		public int getCount() {
			return _waiting.size();
		}

		public Object getItem(int position) {
			return _waiting.get(position);
		}

		public long getItemId(int position) {
			return ((Podcast)getItem(position)).getId();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			Podcast podcast = (Podcast)getItem(position);
			
			LinearLayout view;
			if (convertView == null) {
				view = (LinearLayout)_layoutInflater.inflate(R.layout.downloads_list_item, null);
			}
			else {
				view = (LinearLayout)convertView;
			}
			
			TextView title = (TextView)view.findViewById(R.id.title); 
			title.setText(podcast.getTitle());
			TextView subscription = (TextView)view.findViewById(R.id.subscription); 
			subscription.setText(podcast.getSubscription().getDisplayTitle());
			
			// TODO: figure out when this is null
			if (podcast != null && podcast == _active && podcast.getFileSize() != null)
			{
				int max = podcast.getFileSize();
				int downloaded = (int) new File(podcast.getFilename()).length();
				View extras = _layoutInflater.inflate(R.layout.downloads_list_active_item, null);
				view.addView(extras);
				ProgressBar progressBar = (ProgressBar)extras.findViewById(R.id.progressBar);
				progressBar.setMax(max);
				progressBar.setProgress(downloaded);
				TextView progressText = (TextView)extras.findViewById(R.id.progressText);
				progressText.setText(Math.round(100.0f * downloaded / max) + "% done");
			}
			else
			{
				View extras = view.findViewById(R.id.active);
				if (extras != null)
					view.removeView(extras);
			}
			
			return view;
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Restart Downloader");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			UpdateService.downloadPodcasts(this);
			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
    }
}
