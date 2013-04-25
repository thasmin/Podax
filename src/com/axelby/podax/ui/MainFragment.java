package com.axelby.podax.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.androidquery.AQuery;
import com.androidquery.callback.ImageOptions;
import com.axelby.podax.PlayerService;
import com.axelby.podax.PlayerStatus;
import com.axelby.podax.PodcastCursor;
import com.axelby.podax.PodcastProvider;
import com.axelby.podax.R;

public class MainFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private long _podcastId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.main, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		AQuery aq = new AQuery(getActivity());
		aq.find(R.id.podcast_pic).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), PodcastDetailActivity.class));
			}
		});
		aq.find(R.id.episode).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), PodcastDetailActivity.class));
			}
		});
		aq.find(R.id.podcast).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), PodcastDetailActivity.class));
			}
		});
		aq.find(R.id.restart_btn).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PodcastProvider.restart(getActivity(), _podcastId);
			}
		});
		aq.find(R.id.rewind_btn).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PodcastProvider.movePositionBy(getActivity(), _podcastId, -15);
			}
		});
		aq.find(R.id.play_btn).clicked(new OnClickListener() {
			@Override
			public void onClick(View view) {
				PlayerService.playstop(getActivity());
			}
		});
		aq.find(R.id.forward_btn).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PodcastProvider.movePositionBy(getActivity(), _podcastId, 30);
			}
		});
		aq.find(R.id.skiptoend_btn).clicked(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PodcastProvider.skipToEnd(getActivity(), _podcastId);
			}
		});
		aq.find(R.id.btn_playlist).clicked(new StartActivityOnClickListener(QueueActivity.class));
		aq.find(R.id.btn_podcasts).clicked(new StartActivityOnClickListener(SubscriptionActivity.class));
		aq.find(R.id.btn_search).clicked(new StartActivityOnClickListener(SearchActivity.class));
		aq.find(R.id.btn_prefs).clicked(new StartActivityOnClickListener(Preferences.class));
		aq.find(R.id.btn_about).clicked(new StartActivityOnClickListener(AboutActivity.class));
	}

	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(0, null, this);
	}

	public class StartActivityOnClickListener implements OnClickListener {
		Class<? extends Activity> _activityClass;

		public StartActivityOnClickListener(Class<? extends Activity> activityClass) {
			_activityClass = activityClass;
		}

		@Override
		public void onClick(View view) {
			startActivity(new Intent(getActivity(), _activityClass));
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[] {
				PodcastProvider.COLUMN_ID,
				PodcastProvider.COLUMN_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_TITLE,
				PodcastProvider.COLUMN_SUBSCRIPTION_THUMBNAIL,
				PodcastProvider.COLUMN_DESCRIPTION,
				PodcastProvider.COLUMN_DURATION,
				PodcastProvider.COLUMN_LAST_POSITION,
				PodcastProvider.COLUMN_QUEUE_POSITION,
				PodcastProvider.COLUMN_MEDIA_URL,
				PodcastProvider.COLUMN_PAYMENT,
		};
		return new CursorLoader(getActivity(), PodcastProvider.ACTIVE_PODCAST_URI, projection, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		if (!cursor.moveToFirst()) {
			return;
		} else {
			PodcastCursor podcast = new PodcastCursor(cursor);
			_podcastId = podcast.getId();
			AQuery aq = new AQuery(getActivity());
			aq.find(R.id.episode).text(podcast.getTitle());
			aq.find(R.id.podcast).text(podcast.getSubscriptionTitle());
			
			PodcastProgress progress = (PodcastProgress) aq.find(R.id.podcastprogress).getView();
			progress.set(podcast);
	
			ImageOptions opts = new ImageOptions();
			AQuery pic = aq.find(R.id.podcast_pic);
			opts.targetWidth = pic.getImageView().getWidth();
			opts.fallback = R.drawable.icon;
			pic.image(podcast.getSubscriptionThumbnailUrl(), opts);
	
			PlayerStatus playerState = PlayerStatus.getCurrentState(getActivity());
			int playResource = playerState.isPlaying() ? R.drawable.ic_media_pause : R.drawable.ic_media_play;
			aq.find(R.id.play_btn).image(playResource);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
}
