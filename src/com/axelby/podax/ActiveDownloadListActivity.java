package com.axelby.podax;

import java.io.File;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class ActiveDownloadListActivity extends ListActivity {
	Runnable refresher;
	Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.downloads_list);
		
		Runnable refresher = new Runnable() {
			public void run() {

				final Uri toDownloadURI = Uri.withAppendedPath(PodcastProvider.URI, "to_download");
				final String[] projection = new String[] {
						PodcastProvider.COLUMN_ID,
						PodcastProvider.COLUMN_TITLE,
						PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
						PodcastProvider.COLUMN_MEDIA_URL,
						PodcastProvider.COLUMN_FILE_SIZE,
				};

				Cursor cursor = managedQuery(toDownloadURI, projection, null, null, null);
				cursor.setNotificationUri(getContentResolver(), toDownloadURI);
				setListAdapter(new ActiveDownloadAdapter(ActiveDownloadListActivity.this, cursor));
				handler.postDelayed(this, 1000);
			}
		};
		refresher.run();
	}

	public class ActiveDownloadAdapter extends ResourceCursorAdapter {
		LayoutInflater _layoutInflater;
		
		public ActiveDownloadAdapter(Context context, Cursor cursor) {
			super(context, R.layout.downloads_list_item, cursor);
			
			_layoutInflater = LayoutInflater.from(ActiveDownloadListActivity.this);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			LinearLayout layout = (LinearLayout)view;
			PodcastCursor podcast = new PodcastCursor(context, cursor);
			
			try {
				TextView title = (TextView)layout.findViewById(R.id.title); 
				title.setText(podcast.getTitle());
				TextView subscription = (TextView)layout.findViewById(R.id.subscription); 
				subscription.setText(podcast.getSubscriptionTitle());
				
				View extras = layout.findViewById(R.id.active);
				long downloaded = new File(podcast.getFilename()).length();
				if (podcast.getFileSize() != null && downloaded > 0)
				{
					int max = podcast.getFileSize();
					if (extras == null) {
						extras = _layoutInflater.inflate(R.layout.downloads_list_active_item, null);
						layout.addView(extras);
					}
					ProgressBar progressBar = (ProgressBar)extras.findViewById(R.id.progressBar);
					progressBar.setMax(max);
					progressBar.setProgress((int)downloaded);
					TextView progressText = (TextView)extras.findViewById(R.id.progressText);
					progressText.setText(Math.round(100.0f * downloaded / max) + "% done");
				}
				else
				{
					if (extras != null)
						layout.removeView(extras);
				}
			} catch (MissingFieldException e) {
				e.printStackTrace();
			}
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
