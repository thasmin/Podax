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
import android.util.TypedValue;
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

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
		// could really use algebraic data types here
		List<Object> items = new ArrayList<>(cursor.getCount() * 2);
		String lastPeriod = null;
		while (cursor.moveToNext()) {
			Date date = new EpisodeCursor(cursor).getPubDate();
			Period timeDiff = new Period(new DateTime(date.getTime()), Instant.now());

			String period = PeriodFormat.wordBased().print(timeDiff);
			if (period.indexOf(',') > 0)
				period = period.substring(0, period.indexOf(','));
			period = period + " ago";

			if (!period.equals(lastPeriod))
				items.add(period);
			items.add(cursor.getPosition());
			lastPeriod = period;
		}
		_adapter.changeItems(items, cursor);
		_adapter.notifyDataSetChanged();
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		if (getActivity() == null)
			return;

		_adapter.clear();
		_adapter.notifyDataSetChanged();
	}

	private class LatestActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private Cursor _cursor = null;
		private List<Object> _items = null;

		private final int TYPE_HEADER = 0;
		private final int TYPE_ACTIVITY = 1;

		public class HeaderHolder extends RecyclerView.ViewHolder {
			public TextView header;
			public HeaderHolder(View view) {
				super(view);
				header = (TextView) view;
			}
		}

		public class ActivityHolder extends RecyclerView.ViewHolder {
			public ImageView subscription_img;
			public TextView description;
			public TextView downloaded;
			public View play;

			public ActivityHolder(View view) {
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

		public void changeItems(List<Object> items, Cursor cursor) {
			_items = items;
			_cursor = cursor;
		}

		public void clear() { _cursor = null; }

		@Override
		public long getItemId(int position) {
			if (_items.get(position) instanceof String) {
				return _items.get(position).hashCode();
			}
			_cursor.moveToPosition((Integer) _items.get(position));
			return new EpisodeCursor(_cursor).getId();
		}

		@Override
		public int getItemCount() {
			if (_cursor == null)
				return 0;
			return _items.size();
		}

		@Override
		public int getItemViewType(int position) {
			return _items.get(position) instanceof String ? TYPE_HEADER : TYPE_ACTIVITY;
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == TYPE_ACTIVITY) {
				View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_latest_activity, parent, false);
				return new ActivityHolder(view);
			} else {
				TextView tv = new TextView(parent.getContext());
				tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
				return new HeaderHolder(tv);
			}
		}

		public class IntentSpan extends ClickableSpan {
			private final Intent _intent;
			public IntentSpan(Intent intent) { _intent = intent; }
			@Override public void onClick(View view) { startActivity(_intent); }
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
			if (_cursor == null)
				return;

			int viewType = getItemViewType(position);
			if (viewType == TYPE_ACTIVITY) {
				ActivityHolder holder = (ActivityHolder) viewHolder;

				_cursor.moveToPosition((Integer) _items.get(position));
				EpisodeCursor episode = new EpisodeCursor(_cursor);

				holder.play.setTag(episode.getId());
				holder.subscription_img.setImageBitmap(SubscriptionCursor.getThumbnailImage(getActivity(), episode.getSubscriptionId()));

				// get data for description and spannables
				String subTitle = episode.getSubscriptionTitle();
				Intent subIntent = PodaxFragmentActivity.createIntent(getActivity(), EpisodeListFragment.class, Constants.EXTRA_SUBSCRIPTION_ID, episode.getSubscriptionId());
				int subSpanStart = 0;

				String epTitle = episode.getTitle();
				Intent epIntent = PodaxFragmentActivity.createIntent(getActivity(), EpisodeDetailFragment.class, Constants.EXTRA_EPISODE_ID, episode.getId());
				int epSpanStart = subSpanStart + subTitle.length() + " released ".length();

				String spanText = String.format("%s released %s", subTitle, epTitle);

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
			} else if (viewType == TYPE_HEADER) {
				HeaderHolder holder = (HeaderHolder) viewHolder;
				String period = (String) _items.get(position);
				holder.header.setText(period);
			}
		}
	}
}
