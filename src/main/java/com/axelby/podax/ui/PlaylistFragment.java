package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.axelby.podax.EpisodeData;
import com.axelby.podax.EpisodeDownloadService;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;
import com.trello.rxlifecycle.components.RxFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import rx.Subscriber;

public class PlaylistFragment extends RxFragment {
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
					int newPosition = rv.getChildLayoutPosition(underView);
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

		@Override
		public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		}
	};
	private RecyclerView _listView;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setHasOptionsMenu(true);

		_adapter = new PlaylistListAdapter();
		EpisodeData.getObservables(activity, EpisodeData.PLAYLIST)
			.toList()
			.compose(bindToLifecycle())
			.subscribe(
				_adapter::setEpisodes,
				e -> Log.e("PlaylistFragment", "unable to retrieve episodes", e)
			);
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
			EpisodeDownloadService.downloadEpisodes(getActivity());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private Bitmap viewToBitmap(View view) {
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
		return bitmap;
	}

	private class PlaylistListAdapter extends RecyclerView.Adapter<PlaylistListAdapter.ViewHolder> {

		private List<EpisodeData> _episodes = new ArrayList<>(0);
		private TreeMap<Long, Integer> _ids = new TreeMap<>();
		private final Subscriber<EpisodeData> _episodeSubscriber = new Subscriber<EpisodeData>() {
			@Override public void onCompleted() { }

			@Override
			public void onError(Throwable e) {
				Log.e("FinishedEpisodeFragment", "error while updating episode", e);
			}

			@Override
			public void onNext(EpisodeData episodeData) {
				updateEpisode(episodeData);
			}
		};

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

		private final OnClickListener _clickHandler = view -> {
			long episodeId = (Long) view.getTag();
			startActivity(PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episodeId));
		};

        private final OnClickListener _playHandler = view -> {
			long episodeId = (Long) view.getTag();
			PlayerService.play(getActivity(), episodeId);
		};

        private final OnClickListener _removeHandler = view -> {
			long episodeId = (Long) view.getTag();

			ContentValues values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
			getActivity().getContentResolver().update(EpisodeProvider.getContentUri(episodeId), values, null, null);
		};

		public PlaylistListAdapter() {
			setHasStableIds(true);
		}

		public void setEpisodes(List<EpisodeData> episodes) {
			_episodes = episodes;
			notifyDataSetChanged();

			_ids.clear();
			for (int i = 0; i < _episodes.size(); ++i)
				_ids.put(_episodes.get(i).getId(), i);

			_episodeSubscriber.unsubscribe();
			EpisodeData.getEpisodeWatcher()
				.compose(bindToLifecycle())
				.subscribe(_episodeSubscriber);
		}

		public void updateEpisode(EpisodeData episode) {
			Integer position = _ids.get(episode.getId());
			if (position == null)
				return;
			_episodes.set(position, episode);
			notifyItemChanged(position);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_list_item, parent, false);
			return new ViewHolder(view);
		}

        @Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			EpisodeData episode = _episodes.get(position);

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
            } else if (EpisodeDownloadService.isDownloading(episodeFilename)) {
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
			return _episodes.size();
		}

		@Override
		public long getItemId(int position) {
			return _episodes.get(position).getId();
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
