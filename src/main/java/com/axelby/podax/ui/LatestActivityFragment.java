package com.axelby.podax.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.axelby.podax.Constants;
import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.PlayerService;
import com.axelby.podax.R;
import com.axelby.podax.SubscriptionCursor;

import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.io.File;

import javax.annotation.Nonnull;

public class LatestActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private LatestActivityAdapter _adapter;
	private RecyclerView _listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		_adapter = new LatestActivityAdapter();
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.recyclerview, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listView = (RecyclerView) view.findViewById(R.id.recyclerview);
		_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
		_listView.setItemAnimator(new DefaultItemAnimator());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		_listView.setAdapter(_adapter);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), EpisodeProvider.LATEST_ACTIVITY_URI, null, null, null, EpisodeProvider.COLUMN_PUB_DATE + " DESC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		_adapter.changeCursor(cursor);
		_adapter.notifyDataSetChanged();
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (getActivity() == null)
			return;

		_adapter.changeCursor(null);
		_adapter.notifyDataSetChanged();
	}

	private class LatestActivityAdapter extends RecyclerView.Adapter<LatestActivityAdapter.ViewHolder> {
		private Cursor _cursor;

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ImageView subscription_img;
			public TextView description;
			public TextView downloaded;
			public View play;

			public ViewHolder(View view) {
				super(view);

				subscription_img = (ImageView) view.findViewById(R.id.subscription_img);
				description = (TextView) view.findViewById(R.id.description);
				downloaded = (TextView) view.findViewById(R.id.downloaded);
				play = view.findViewById(R.id.play);
				play.setOnClickListener(_playHandler);
			}
		}

		private final View.OnClickListener _playHandler = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				PlayerService.play(getActivity(), (long) view.getTag());
			}
		};

		public LatestActivityAdapter() {
			setHasStableIds(true);
		}

		public void changeCursor(Cursor cursor) {
			_cursor = cursor;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_latest_activity, parent, false);
			return new ViewHolder(view);
		}

		public class IntentSpan extends ClickableSpan {
			private final Intent _intent;
			public IntentSpan(Intent intent) { _intent = intent; }
			@Override public void onClick(View view) { startActivity(_intent); }
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (_cursor == null)
				return;
			_cursor.moveToPosition(position);
			EpisodeCursor episode = new EpisodeCursor(_cursor);

			holder.play.setTag(episode.getId());
			holder.subscription_img.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));

			// get data for description and spannables
			Period diff = new Period(episode.getPubDate().getTime(), Instant.now().getMillis());
			String timeAgo = PeriodFormat.wordBased().print(diff);
			if (timeAgo.indexOf(',') > 0)
				timeAgo = timeAgo.substring(0, timeAgo.indexOf(','));
			timeAgo = timeAgo + " ago, ";

			String subTitle = episode.getSubscriptionTitle();
			Intent subIntent = PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, episode.getSubscriptionId());
			int subSpanStart = timeAgo.length();

			String epTitle = episode.getTitle();
			Intent epIntent = PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episode.getId());
			int epSpanStart = subSpanStart + subTitle.length() + " released ".length();

			String spanText = String.format("%s%s released %s", timeAgo, subTitle, epTitle);

			// set description and spannables
			SpannableString description = new SpannableString(spanText);
			description.setSpan(new IntentSpan(subIntent), subSpanStart, subSpanStart + subTitle.length(), Spanned.SPAN_MARK_MARK);
			description.setSpan(new IntentSpan(epIntent), epSpanStart, spanText.length(), Spanned.SPAN_MARK_MARK);
			holder.description.setText(description);
			holder.description.setMovementMethod(LinkMovementMethod.getInstance());

            float downloaded = new File(episode.getFilename(getActivity())).length();
            if (episode.getFileSize() != downloaded) {
                holder.downloaded.setTextColor(0xffcc0000); //android.R.color.holo_red_dark
                holder.downloaded.setText(R.string.not_downloaded);
            } else {
                holder.downloaded.setTextColor(0xff669900); //android.R.color.holo_green_dark
                holder.downloaded.setText(R.string.downloaded);
            }
		}

		@Override
		public long getItemId(int position) {
			_cursor.moveToPosition(position);
			return new EpisodeCursor(_cursor).getId();
		}

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _cursor.getCount();
		}
	}
}
