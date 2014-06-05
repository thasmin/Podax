package com.axelby.podax.ui;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.axelby.podax.Constants;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;
import com.axelby.podax.UpdateService;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DragListener;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.DragSortListView.RemoveListener;

import java.io.File;

public class QueueFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	static final int OPTION_REMOVEFROMQUEUE = 1;
	static final int OPTION_PLAY = 2;
	static final int OPTION_MOVETOFIRSTINQUEUE = 3;
	// make sure list is refreshed to update downloading files
	Runnable _refresher = new Runnable() {
		public void run() {
			if (getActivity() == null)
				return;

			// make sure the listview is populated
			if (getListView().getChildCount() == 0) {
				_handler.postDelayed(_refresher, 1000);
				return;
			}

			boolean repost = false;
			for (int i = 0; i < getListAdapter().getCount(); ++i) {
				View view = getListView().getChildAt(i);
				if (view == null)
					continue;
				View progress = view.findViewById(R.id.dlprogress);
				if (progress == null || progress.getVisibility() == View.GONE)
					continue;

				PodcastCursor podcast = new PodcastCursor((Cursor) getListAdapter().getItem(i));
				long downloaded = new File(podcast.getFilename(getActivity())).length();
				if (podcast.getFileSize() != null && downloaded == podcast.getFileSize()) {
					progress.setVisibility(View.GONE);
				} else {
					repost = true;
					ProgressBar progressBar = (ProgressBar) progress.findViewById(R.id.progressBar);
					progressBar.setProgress((int) downloaded);
					TextView progressText = (TextView) progress.findViewById(R.id.progressText);
					progressText.setText(Math.round(100.0f * downloaded / podcast.getFileSize()) + "% downloaded");
				}
			}

			if (repost)
				_handler.postDelayed(_refresher, 1000);
		}
	};
	Handler _handler = new Handler();
	QueueListAdapter _adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);

		_adapter = new QueueListAdapter(getActivity(), null);
		setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.queue, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getActivity().registerForContextMenu(getListView());

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Bundle args = new Bundle();
				args.putLong(Constants.EXTRA_PODCAST_ID, id);

				FragmentTransaction ft = getFragmentManager().beginTransaction();
				PodcastDetailFragment fragment = new PodcastDetailFragment();
				fragment.setArguments(args);
				ft.replace(R.id.fragment, fragment).addToBackStack(null).commit();
			}
		});
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfo;
				Cursor c = (Cursor) getListAdapter().getItem(mi.position);
				PodcastCursor podcast = new PodcastCursor(c);

				if (podcast.isDownloaded(getActivity()))
					menu.add(ContextMenu.NONE, OPTION_PLAY, ContextMenu.NONE, R.string.play);

				if (mi.position != 0)
					menu.add(ContextMenu.NONE, OPTION_MOVETOFIRSTINQUEUE, ContextMenu.NONE, R.string.move_to_first_in_queue);

				menu.add(ContextMenu.NONE, OPTION_REMOVEFROMQUEUE, ContextMenu.NONE, R.string.remove_from_queue);
			}
		});

		// disable swipe to remove according to preference
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (!preferences.getBoolean("allowPlaylistSwipeToRemove", true))
		{
			DragSortListView dragSortListView = (DragSortListView) getListView();
			DragSortController dragSortController = (DragSortController) dragSortListView.getFloatViewManager();
			dragSortController.setRemoveEnabled(false);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.queue_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.download) {
			UpdateService.downloadPodcasts(getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
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
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		PodcastCursor podcast = new PodcastCursor(cursor);

		switch (item.getItemId()) {
			case OPTION_MOVETOFIRSTINQUEUE:
				podcast.moveToFirstInQueue(getActivity());
				return true;
			case OPTION_REMOVEFROMQUEUE:
				podcast.removeFromQueue(getActivity());
				return true;
			case OPTION_PLAY:
				PlayerService.play(getActivity(), podcast.getId());

				Bundle args = new Bundle();
				args.putLong(Constants.EXTRA_PODCAST_ID, podcast.getId());
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				PodcastDetailFragment fragment = new PodcastDetailFragment();
				fragment.setArguments(args);
				ft.replace(R.id.fragment, fragment).addToBackStack(null).commit();

				return true;
		}

		return false;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[]{
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
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

	private class QueueListAdapter extends ResourceCursorAdapter implements DragListener, DropListener, RemoveListener {

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
					getActivity().openContextMenu((View) (view.getParent()));
				}
			});

			((TextView) view.findViewById(R.id.title)).setText(podcast.getTitle());
			((TextView) view.findViewById(R.id.subscription)).setText(podcast.getSubscriptionTitle());
			((NetworkImageView) view.findViewById(R.id.thumbnail)).setImageUrl(podcast.getSubscriptionThumbnailUrl(), Helper.getImageLoader(context));

			// if the podcast is not downloaded, add the download indicator
			long downloaded = new File(podcast.getFilename(getActivity())).length();
			if (podcast.getFileSize() != null && downloaded != podcast.getFileSize()) {
				View dlprogress;
				ViewStub dlprogressStub = (ViewStub) view.findViewById(R.id.dlprogress_stub);
				if (dlprogressStub != null)
					dlprogress = dlprogressStub.inflate();
				else
					dlprogress = view.findViewById(R.id.dlprogress);
				dlprogress.setVisibility(View.VISIBLE);
				ProgressBar progressBar = (ProgressBar) dlprogress.findViewById(R.id.progressBar);
				progressBar.setMax(podcast.getFileSize());
				progressBar.setProgress((int) downloaded);
				TextView progressText = (TextView) dlprogress.findViewById(R.id.progressText);
				progressText.setText(Math.round(100.0f * downloaded / podcast.getFileSize()) + "% downloaded");

				// make sure list is refreshed to update downloading files
				_handler.removeCallbacks(_refresher);
				_handler.postDelayed(_refresher, 1000);
			} else {
				if (view.findViewById(R.id.dlprogress) != null)
					view.findViewById(R.id.dlprogress).setVisibility(View.GONE);
			}
		}

		@Override
		public void drop(int from, int to) {
			Long podcastId = _adapter.getItemId(from);
			ContentValues values = new ContentValues();
			values.put(PodcastProvider.COLUMN_QUEUE_POSITION, to);
			Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, podcastId);
			getActivity().getContentResolver().update(podcastUri, values, null, null);
		}

		@Override
		public void drag(int from, int to) {
		}

		@Override
		public void remove(int which) {
			Long podcastId = _adapter.getItemId(which);
			ContentValues values = new ContentValues();
			values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer) null);
			Uri podcastUri = ContentUris.withAppendedId(PodcastProvider.URI, podcastId);
			getActivity().getContentResolver().update(podcastUri, values, null, null);
		}
	}
}
