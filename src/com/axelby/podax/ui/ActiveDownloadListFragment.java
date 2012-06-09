package com.axelby.podax.ui;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class ActiveDownloadListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	Runnable refresher;
	Handler handler = new Handler();
	private ActiveDownloadAdapter _adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);

		_adapter = new ActiveDownloadAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		return inflater.inflate(R.layout.downloads_list, null, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		updateWifiNotice();
		
		refresher = new Runnable() {
			public void run() {
				getLoaderManager().restartLoader(0, null, ActiveDownloadListFragment.this);
				handler.postDelayed(this, 1000);
			}
		};
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		final Uri toDownloadUri = Uri.withAppendedPath(PodcastProvider.URI, "to_download");
		final String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_FILE_SIZE,
		};
		return new CursorLoader(getActivity(), toDownloadUri, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		_adapter.changeCursor(null);
	}

	@Override
	public void onResume() {
		updateWifiNotice();
		refresher.run();
		super.onResume();
	}

	@Override
	public void onPause() {
		handler.removeCallbacks(refresher);
		super.onPause();
	}

	public void updateWifiNotice() {
		boolean wifiOnly = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("wifiPref", true);
		TextView wifiNotice = (TextView) getActivity().findViewById(R.id.wifinotice);
		wifiNotice.setText(wifiOnly ? R.string.download_only_on_wifi : R.string.download_anytime);
	}

	public class ActiveDownloadAdapter extends ResourceCursorAdapter {
		LayoutInflater _layoutInflater;
		
		public ActiveDownloadAdapter(Context context, Cursor cursor) {
			super(context, R.layout.downloads_list_item, cursor, true);
			
			_layoutInflater = LayoutInflater.from(getActivity());
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			LinearLayout layout = (LinearLayout)view;
			PodcastCursor podcast = new PodcastCursor(cursor);
			
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
		}
	}
}
