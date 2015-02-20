package com.axelby.podax.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeDownloader;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.UpdateService;

import java.io.File;

import javax.annotation.Nonnull;

public class PlaylistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private PlaylistListAdapter _adapter;

	private ImageView _overlay;
	private boolean _isDragging;
	private long _dragEpisodeId;

	private final RecyclerView.OnItemTouchListener _touchListener = new RecyclerView.OnItemTouchListener() {
		float _dragStartMouseY;
		int _dragStartTop;

		private boolean containsDragHandle(View itemView, MotionEvent e) {
			int itemTop = itemView.getTop();
			View dragHandle = itemView.findViewById(R.id.drag);
			Rect r = new Rect();
			dragHandle.getHitRect(r);
			return r.contains((int) e.getX(), (int) e.getY() - itemTop);
		}

		@Override
		public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
			// intercept the touch event if currently dragging
			if (_isDragging)
				return true;

			// intercept the touch event if it's a drag handle
			View itemView = rv.findChildViewUnder(e.getX(), e.getY());
			if (itemView == null)
				return false;
			if (containsDragHandle(itemView, e)) {
				_isDragging = true;

				// keep track of the mouse location, top of item, and episode id
				_dragStartMouseY = e.getY();
				_dragStartTop = itemView.getTop();
				_dragEpisodeId = rv.getChildItemId(itemView);

				// remove the episode from the list and show the overlay
				_overlay.setImageBitmap(viewToBitmap(itemView));
				_overlay.setPadding(0, _dragStartTop, 0, 0);
				_overlay.setVisibility(View.VISIBLE);
				itemView.setVisibility(View.INVISIBLE);

				return true;
			}
			return false;
		}

		@Override
		public void onTouchEvent(RecyclerView rv, MotionEvent e) {
			if (e.getAction() == MotionEvent.ACTION_UP) {
				_isDragging = false;
				_overlay.setVisibility(View.GONE);

				int position = _adapter.getPositionForId(_dragEpisodeId);
				View draggedView = rv.getChildAt(position);
				if (draggedView != null)
					draggedView.setVisibility(View.VISIBLE);
			} else if (e.getAction() == MotionEvent.ACTION_CANCEL) {
				_isDragging = false;
				_overlay.setVisibility(View.GONE);

				int position = _adapter.getPositionForId(_dragEpisodeId);
				View draggedView = rv.getChildAt(position);
				draggedView.setVisibility(View.VISIBLE);
			} else if (e.getAction() == MotionEvent.ACTION_MOVE) {
				float deltaY = e.getY() - _dragStartMouseY;
				if (_overlay.getPaddingTop() < 0 && deltaY < -5) {
					rv.scrollBy(0, -10);
				}

				// switch if the overlay isn't on top of the original view
				View underView = rv.findChildViewUnder(0, (int) e.getY());
				if (underView != null && rv.getChildItemId(underView) != _dragEpisodeId) {
					int newPosition = rv.getChildPosition(underView);
					_adapter.moveItem(_dragEpisodeId, newPosition);
					if (newPosition == 0)
						rv.scrollToPosition(0);
				}

				// scroll down if overlay is below bottom
				int overlayPaddingTop = (int) (_dragStartTop + deltaY);
				int maxPadding = rv.getHeight() - (_overlay.getHeight() - _overlay.getPaddingTop());
				if (overlayPaddingTop > maxPadding) {
					rv.scrollBy(0, overlayPaddingTop - maxPadding);
					overlayPaddingTop = maxPadding;
				}
				_overlay.setPadding(0, overlayPaddingTop, 0, 0);
			}
		}
	};
	private RecyclerView _listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		_adapter = new PlaylistListAdapter();
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.playlist, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) view.findViewById(R.id.list);
		_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
		_listView.setItemAnimator(new DefaultItemAnimator());

		_overlay = (ImageView) getActivity().findViewById(R.id.overlay);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		_listView.addOnItemTouchListener(_touchListener);
		_listView.setAdapter(_adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.playlist_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.download) {
			UpdateService.downloadEpisodes(getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[]{
				EpisodeProvider.COLUMN_ID,
				EpisodeProvider.COLUMN_TITLE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
				EpisodeProvider.COLUMN_SUBSCRIPTION_TITLE,
				EpisodeProvider.COLUMN_PLAYLIST_POSITION,
				EpisodeProvider.COLUMN_MEDIA_URL,
				EpisodeProvider.COLUMN_FILE_SIZE,
				EpisodeProvider.COLUMN_SUBSCRIPTION_ID,
				EpisodeProvider.COLUMN_DURATION,
		};
		return new CursorLoader(getActivity(), EpisodeProvider.PLAYLIST_URI, projection, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (getActivity() == null)
			return;

		_adapter.changeCursor(null);
	}

	private Bitmap viewToBitmap(View view) {
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
		return bitmap;
	}

	private class PlaylistListAdapter extends RecyclerView.Adapter<PlaylistListAdapter.ViewHolder> {

		private Cursor _cursor = null;

		class ViewHolder extends RecyclerView.ViewHolder {
			public final View container;
            public final TextView title;
            public final ImageView thumbnail;
			public final TextView duration;
            public final TextView downloaded;
			public final View play;
			public final View remove;

            public ViewHolder(View view) {
				super(view);

				container = view;
				container.setOnClickListener(_clickHandler);

                title = (TextView) view.findViewById(R.id.title);
                thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
				duration = (TextView) view.findViewById(R.id.duration);
                downloaded = (TextView) view.findViewById(R.id.downloaded);

				play = view.findViewById(R.id.play);
				play.setOnClickListener(_playHandler);

				remove = view.findViewById(R.id.remove);
				remove.setOnClickListener(_removeHandler);
            }
        }

		private final OnClickListener _clickHandler = new OnClickListener() {
			@Override
			public void onClick(View view) {
				long episodeId = (Long) view.getTag();
				startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
			}
		};

        private final OnClickListener _playHandler = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag();
				PlayerService.play(getActivity(), episodeId);
            }
        };

        private final OnClickListener _removeHandler = new OnClickListener() {
            @Override
            public void onClick(View view) {
                long episodeId = (Long) view.getTag();

                ContentValues values = new ContentValues();
                values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
                getActivity().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
            }
        };

		public PlaylistListAdapter() {
			setHasStableIds(true);
		}

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
			notifyDataSetChanged();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_list_item, parent, false);
			return new ViewHolder(view);
		}

        @Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (_cursor == null)
				return;
			_cursor.moveToPosition(position);
			EpisodeCursor episode = new EpisodeCursor(_cursor);

			holder.play.setTag(episode.getId());
			holder.remove.setTag(episode.getId());
			holder.container.setTag(episode.getId());

            holder.title.setText(episode.getTitle());
            holder.thumbnail.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));
			holder.duration.setText(Helper.getTimeString(episode.getDuration()));

			String episodeFilename = episode.getFilename(getActivity());
			float downloaded = new File(episodeFilename).length();
            if (episode.getFileSize() == downloaded) {
				holder.downloaded.setTextColor(0xff669900); //android.R.color.holo_green_dark
				holder.downloaded.setText(R.string.downloaded);
            } else if (EpisodeDownloader.isDownloading(episodeFilename)) {
				holder.downloaded.setTextColor(0xff669900); //android.R.color.holo_green_dark
				holder.downloaded.setText(R.string.now_downloading);
			} else {
				holder.downloaded.setTextColor(0xffcc0000); //android.R.color.holo_red_dark
				holder.downloaded.setText(R.string.not_downloaded);
			}

			// handle dragging status
			if (!_isDragging || episode.getId() != _dragEpisodeId)
				holder.container.setVisibility(View.VISIBLE);
			else
				holder.container.setVisibility(View.GONE);
		}

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _cursor.getCount();
		}

		@Override
		public long getItemId(int position) {
			_cursor.moveToPosition(position);
			return new EpisodeCursor(_cursor).getId();
		}

		public int getPositionForId(long id) {
			for (int i = 0; i < getItemCount(); ++i)
				if (getItemId(i) == id)
					return i;
			return -1;
		}

		public void moveItem(long id, int newPosition) {
			ContentValues values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, newPosition);
			Uri podcastUri = EpisodeProvider.getContentUri(id);
			getActivity().getContentResolver().update(podcastUri, values, null, null);
			notifyItemMoved(getPositionForId(id), newPosition);
		}
	}
}
