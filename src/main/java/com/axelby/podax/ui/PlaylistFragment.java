package com.axelby.podax.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.databinding.BaseObservable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.ObservableLong;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.axelby.podax.AppFlow;
import com.axelby.podax.BR;
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
	private RecyclerView _listView;
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
				onTouchEvent(rv, e);
				return true;
			}

			return false;
		}

		@Override
		public void onTouchEvent(RecyclerView rv, MotionEvent e) {
			switch (e.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					_isDragging = true;

					View itemView = rv.findChildViewUnder(e.getX(), e.getY());

					// keep track of the mouse location, top of item, and episode id
					_dragStartMouseY = e.getY();
					_dragStartTop = itemView.getTop();
					_dragEpisodeId = rv.getChildItemId(itemView);

					// remove the episode from the list and show the overlay
					_overlay.setImageBitmap(viewToBitmap(itemView));
					_overlay.setPadding(0, _dragStartTop, 0, 0);
					_overlay.setVisibility(View.VISIBLE);
					itemView.setVisibility(View.INVISIBLE);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					_isDragging = false;
					_overlay.setVisibility(View.GONE);

					int position = _adapter.getPositionForId(_dragEpisodeId);
					View draggedView = rv.getChildAt(position);
					if (draggedView != null)
						draggedView.setVisibility(View.VISIBLE);
					break;
				case MotionEvent.ACTION_MOVE:
					if (_listView.getItemAnimator().isRunning())
						break;

					float deltaY = e.getY() - _dragStartMouseY;
					if (_overlay.getPaddingTop() < 0 && deltaY < -5)
						rv.scrollBy(0, -10);

					// switch if the overlay isn't on top of the original view
					View underView = null;
					for (int i = 0; i < rv.getChildCount(); ++i) {
						View childAt = rv.getChildAt(i);
						if (childAt.getTop() < e.getY() && childAt.getBottom() > e.getY()) {
							underView = childAt;
							break;
						}
					}
					if (underView == null)
						Log.d("PlaylistFragment", "now under null child");
					else
						Log.d("PlaylistFragment", "now under child at " + rv.getChildAdapterPosition(underView) + " id " + rv.getChildItemId(underView));

					if (underView != null && rv.getChildItemId(underView) != _dragEpisodeId) {
						int newPosition = rv.getChildLayoutPosition(underView);
						_adapter.moveItem(_dragEpisodeId, newPosition);
						if (newPosition == 0)
							rv.smoothScrollToPosition(0);
					}

					// scroll down if overlay is below bottom
					int overlayPaddingTop = (int) (_dragStartTop + deltaY);
					int maxPadding = rv.getHeight() - (_overlay.getHeight() - _overlay.getPaddingTop());
					if (overlayPaddingTop > maxPadding) {
						rv.scrollBy(0, overlayPaddingTop - maxPadding);
						overlayPaddingTop = maxPadding;
					}
					_overlay.setPadding(0, overlayPaddingTop, 0, 0);

					break;
			}
		}

		@Override
		public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setHasOptionsMenu(true);

		_adapter = new PlaylistListAdapter();
		EpisodeData.getObservables(activity, EpisodeData.Filter.PLAYLIST)
			.map(EpisodeModel::new)
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

	private class PlaylistListAdapter extends RecyclerView.Adapter<DataBoundViewHolder> {

		private List<EpisodeModel> _episodes = new ArrayList<>(0);
		private TreeMap<Long, Integer> _ids = new TreeMap<>();
		private final Subscriber<EpisodeModel> _episodeSubscriber = new Subscriber<EpisodeModel>() {
			@Override public void onCompleted() { }

			@Override
			public void onError(Throwable e) {
				Log.e("FinishedEpisodeFragment", "error while updating episode", e);
			}

			@Override
			public void onNext(EpisodeModel episodeData) {
				updateEpisode(episodeData);
			}
		};

		public PlaylistListAdapter() {
			setHasStableIds(true);
		}

		public void setEpisodes(List<EpisodeModel> episodes) {
			_episodes = episodes;
			notifyDataSetChanged();

			_ids.clear();
			for (int i = 0; i < _episodes.size(); ++i)
				_ids.put(_episodes.get(i).getId(), i);

			_episodeSubscriber.unsubscribe();
			EpisodeData.getEpisodeWatcher()
				.map(EpisodeModel::new)
				.compose(bindToLifecycle())
				.subscribe(_episodeSubscriber);
		}

		public void updateEpisode(EpisodeModel episode) {
			Integer position = _ids.get(episode.getId());
			if (position == null)
				return;
			_episodes.set(position, episode);
			notifyItemChanged(position);
		}

		@Override
		public DataBoundViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			return new DataBoundViewHolder(inflater.inflate(R.layout.playlist_list_item, parent, false));
		}

        @Override
		public void onBindViewHolder(DataBoundViewHolder holder, int position) {
			holder.binding.setVariable(BR.model, _episodes.get(position));
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

			int oldPosition = getPositionForId(id);
			EpisodeModel ep = _episodes.get(newPosition);
			_episodes.set(newPosition, _episodes.get(oldPosition));
			_episodes.set(oldPosition, ep);

			notifyItemMoved(getPositionForId(id), newPosition);
		}

	}

	@BindingAdapter({"app:subscriptionImageId"})
	@SuppressWarnings("unused")
	public static void loadSubscriptionImage(ImageView image, long subscriptionId) {
		if (subscriptionId != -1)
			SubscriptionCursor.getThumbnailImage(image.getContext(), subscriptionId).into(image);
	}

	@SuppressWarnings("unused")
	public class EpisodeModel extends BaseObservable {
		private final long _id;

		public final ObservableLong subscriptionId = new ObservableLong(-1);
		public final ObservableField<String> title = new ObservableField<>("");
		public final ObservableField<String> downloadStatus = new ObservableField<>("downloaded");
		public final ObservableInt downloadStatusColor = new ObservableInt(0xff669900);
		public final ObservableField<String> duration = new ObservableField<>("");
		public final ObservableBoolean isDragging = new ObservableBoolean(false);

		public EpisodeModel(@NonNull EpisodeData episode) {
			_id = episode.getId();
			title.set(episode.getTitle());
			subscriptionId.set(episode.getSubscriptionId());
			duration.set(Helper.getTimeString(episode.getDuration()));

			String episodeFilename = episode.getFilename(getActivity());
			float downloaded = new File(episodeFilename).length();
            if (episode.getFileSize() == downloaded) {
				downloadStatusColor.set(0xff669900); //android.R.color.holo_green_dark
				downloadStatus.set(getString(R.string.downloaded));
            } else if (EpisodeDownloadService.isDownloading(episodeFilename)) {
				downloadStatusColor.set(0xff669900); //android.R.color.holo_green_dark
				downloadStatus.set(getString(R.string.now_downloading));
			} else {
				downloadStatusColor.set(0xffcc0000); //android.R.color.holo_red_dark
				downloadStatus.set(getString(R.string.not_downloaded));
			}
		}

		public long getId() {
			return _id;
		}

		public void setIsDragging(boolean isDragging) {
			this.isDragging.set(isDragging);
		}

		public void show(View view) {
			View thumbnail = view.findViewById(R.id.thumbnail);
			View title = view.findViewById(R.id.title);
			AppFlow.get(getActivity()).displayEpisode(_id, thumbnail, title);
		}

		public void play(View view) {
			PlayerService.play(getActivity(), _id);
		}

		public void remove(View view) {
			ContentValues values = new ContentValues();
			values.put(EpisodeProvider.COLUMN_PLAYLIST_POSITION, (Integer) null);
			getActivity().getContentResolver().update(EpisodeProvider.getContentUri(_id), values, null, null);
		}
	}

}
