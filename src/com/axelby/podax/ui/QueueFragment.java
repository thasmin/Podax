package com.axelby.podax.ui;

import java.io.File;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.axelby.podax.Constants;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PodaxLog;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

public class QueueFragment extends SherlockListFragment implements DropListener, LoaderManager.LoaderCallbacks<Cursor> {
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;
	static final int OPTION_MOVETOFIRSTINQUEUE = 3;

	Runnable _refresher = new Runnable() {
		public void run() {
			if (getActivity() == null)
				return;
			getLoaderManager().restartLoader(0, null, QueueFragment.this);
			_handler.postDelayed(_refresher, 1000);
		}
	};
	Handler _handler = new Handler();
	QueueListAdapter _adapter;

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
		return inflater.inflate(R.layout.queue, null, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(getActivity(), PodcastDetailActivity.class);
				intent.putExtra(Constants.EXTRA_PODCAST_ID, id);
				startActivity(intent);
			}
		});
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
				Cursor c = (Cursor) getListAdapter().getItem(mi.position);
				PodcastCursor podcast = new PodcastCursor(c);

				if (podcast.isDownloaded())
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE, R.string.play);

				if (mi.position != 0)
					menu.add(ContextMenu.NONE, OPTION_MOVETOFIRSTINQUEUE, ContextMenu.NONE, R.string.move_to_first_in_queue);

				menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE, ContextMenu.NONE, R.string.remove_from_queue);
			}
		});

        DragSortListView lv = (DragSortListView) getListView(); 
        lv.setDropListener(this);
    }

	@Override
	public void onPause() {
		super.onPause();
		_handler.removeCallbacks(_refresher);
	}

	@Override
	public void onResume() {
		super.onResume();
		_refresher.run();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		PodcastCursor podcast = new PodcastCursor(cursor);

		switch (item.getItemId()) {
		case OPTION_MOVETOFIRSTINQUEUE:
			podcast.moveToFirstInQueue(getActivity());
			return true;
		case OPTION_REMOVEFROMQUEUE:
			podcast.removeFromQueue(getActivity());
			return true;
		case OPTION_PLAY:
			PodaxLog.log(getActivity(), "playing a specific podcast from queuefragment");
			PlayerService.play(getActivity(), podcast);
			return true;
		}

		return false;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[] {
			PodcastProvider.COLUMN_ID,
			PodcastProvider.COLUMN_TITLE,
			PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
			PodcastProvider.COLUMN_QUEUE_POSITION,
			PodcastProvider.COLUMN_MEDIA_URL,
			PodcastProvider.COLUMN_FILE_SIZE,
			PodcastProvider.COLUMN_SUBSCRIPTION_ID,
		};
		return new CursorLoader(getActivity(), PodcastProvider.QUEUE_URI, projection, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (getActivity() == null)
			return;

		_adapter.changeCursor(null);
	}

	private class QueueListAdapter extends ResourceCursorAdapter {

		public QueueListAdapter(Context context, Cursor cursor) {
			super(context, R.layout.queue_list_item, cursor, true);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			PodcastCursor podcast = new PodcastCursor(cursor);

			view.setTag(podcast.getId());

			// more button handler
			view.findViewById(R.id.more).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					getActivity().openContextMenu((View)(view.getParent()));
				}
			});

			// set the title
			TextView queueText = (TextView) view.findViewById(R.id.title);
			queueText.setText(podcast.getTitle());
			// set the subscription title
			TextView subscriptionText = (TextView) view.findViewById(R.id.subscription);
			subscriptionText.setText(podcast.getSubscriptionTitle());

			// set the thumbnail
			ImageView thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
			File thumbnailFile = new File(podcast.getThumbnailFilename());
			if (!thumbnailFile.exists())
				thumbnail.setImageDrawable(null);
			else
			{
				try {
					thumbnail.setImageBitmap(BitmapFactory.decodeFile(podcast.getThumbnailFilename()));
					thumbnail.setVisibility(View.VISIBLE);
				} catch (OutOfMemoryError e) {
					thumbnail.setVisibility(View.INVISIBLE);
				}
			}

			// if the podcast is not downloaded, add the download indicator
			ViewStub dlprogressStub = (ViewStub)view.findViewById(R.id.dlprogress_stub);
			long downloaded = new File(podcast.getFilename()).length();
			if (podcast.getFileSize() != null && downloaded != podcast.getFileSize())
			{
				View dlprogress;
				if (dlprogressStub != null)
					dlprogress = dlprogressStub.inflate();
				else
					dlprogress = view.findViewById(R.id.dlprogress);
				dlprogress.setVisibility(View.VISIBLE);
				ProgressBar progressBar = (ProgressBar)dlprogress.findViewById(R.id.progressBar);
				progressBar.setMax(podcast.getFileSize());
				progressBar.setProgress((int)downloaded);
				TextView progressText = (TextView)dlprogress.findViewById(R.id.progressText);
				progressText.setText(Math.round(100.0f * downloaded / podcast.getFileSize()) + "% downloaded");

				// make sure list is refreshed to update downloading files
				_handler.removeCallbacks(_refresher);
				_handler.postDelayed(_refresher, 1000);
			}
			else
			{
				View dlprogress = view.findViewById(R.id.dlprogress);
				if (dlprogress != null)
					dlprogress.setVisibility(View.GONE);
			}

		}

	}

	@Override
	public void drop(int from, int to) {
		Long podcastId = _adapter.getItemId(from);
		// update position
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, to);
		Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, podcastId);
		getActivity().getContentResolver().update(podcastUri, values, null, null);
	}
}
