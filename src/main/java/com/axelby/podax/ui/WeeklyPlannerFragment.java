package com.axelby.podax.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.axelby.podax.EpisodeCursor;
import com.axelby.podax.EpisodeProvider;
import com.axelby.podax.Helper;
import com.axelby.podax.R;
import com.axelby.podax.Stats;
import com.axelby.podax.SubscriptionCursor;
import com.axelby.podax.SubscriptionProvider;

import org.joda.time.LocalDate;

import javax.annotation.Nonnull;

// TODO: localize, add "find new subscriptions" button
public class WeeklyPlannerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private TextView _listenTime;
	private TextView _autoAddTime;
	private TextView _diffTime;
	private TextView _diffLabel;
	private ViewGroup _subList;
	private View _subEmpty;

	private CheckBox.OnCheckedChangeListener _subCheckHandler = (checkbox, checked) -> {
		long subId = (long) checkbox.getTag();
		ContentValues values = new ContentValues(1);
		values.put(SubscriptionProvider.COLUMN_PLAYLIST_NEW, checked);
		ContentResolver contentResolver = checkbox.getContext().getContentResolver();
		contentResolver.update(SubscriptionProvider.getContentUri(subId), values, null, null);
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.weekly_planner_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		_listenTime = (TextView) view.findViewById(R.id.listen_time);
		_autoAddTime = (TextView) view.findViewById(R.id.autoadd_time);
		_diffTime = (TextView) view.findViewById(R.id.diff_time);
		_diffLabel = (TextView) view.findViewById(R.id.diff_label);
		_subList = (ViewGroup) view.findViewById(R.id.subscription_list);
		_subEmpty = view.findViewById(R.id.subscription_empty);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		float weekListenTime = 0f;
		for (LocalDate date = LocalDate.now().minusDays(1); date.compareTo(LocalDate.now().minusDays(8)) > 0; date = date.minusDays(1))
			weekListenTime += Stats.getListenTime(getActivity(), date);
		_listenTime.setText(Helper.getVerboseTimeString(getActivity(), weekListenTime, false));

		ContentResolver contentResolver = getActivity().getContentResolver();

		StringBuilder subIds = new StringBuilder(200);
		String[] projection = { SubscriptionProvider.COLUMN_ID };
		Cursor c = contentResolver.query(SubscriptionProvider.URI, projection, SubscriptionProvider.COLUMN_PLAYLIST_NEW + " = 1", null, null);
		if (c != null) {
			while (c.moveToNext())
				subIds.append(c.getLong(0)).append(", ");
			c.close();
		}

		float autoAdded = 0f;
		if (subIds.length() > 0) {
			String subIdStr = subIds.toString().substring(0, subIds.length() - 2);
			String selection = EpisodeProvider.COLUMN_SUBSCRIPTION_ID + " IN (" + subIdStr + ")" +
				" AND " + EpisodeProvider.COLUMN_PUB_DATE + " > " + (LocalDate.now().minusDays(7).toDate().getTime() / 1000);
			c = contentResolver.query(EpisodeProvider.URI, null, selection, null, null);
			if (c != null) {
				while (c.moveToNext()) {
					EpisodeCursor ep = new EpisodeCursor(c);
					if (ep.getDuration() == 0)
						ep.determineDuration(getActivity());
					autoAdded += ep.getDuration() / 1000.0f;
				}
				c.close();
			}
		}
		_autoAddTime.setText(Helper.getVerboseTimeString(getActivity(), autoAdded, false));

		float diffTime = Math.abs(autoAdded - weekListenTime);
		_diffTime.setText(Helper.getVerboseTimeString(getActivity(), diffTime, false));
		_diffLabel.setText(autoAdded > weekListenTime ? R.string.extra : R.string.shortage);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		if (id == 0)
			return new CursorLoader(getActivity(), SubscriptionProvider.URI, null, null, null, null);

		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (loader.getId() == 0) {
			boolean isEmpty = cursor.getCount() == 0;
			while (_subList.getChildCount() > 3)
				_subList.removeViewAt(3);
			_subEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

			LayoutInflater inflater = LayoutInflater.from(getActivity());
			while (cursor.moveToNext()) {
				SubscriptionCursor sub = new SubscriptionCursor(cursor);
				CheckBox cb = (CheckBox) inflater.inflate(R.layout.checkbox, _subList, false);
				cb.setText(sub.getTitle());
				cb.setTag(sub.getId());
				cb.setChecked(sub.areNewEpisodesAddedToPlaylist());
				cb.setOnCheckedChangeListener(_subCheckHandler);
				_subList.addView(cb);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == 0) {
			while (_subList.getChildCount() > 3)
				_subList.removeViewAt(3);
			_subEmpty.setVisibility(View.VISIBLE);
		}
	}
}
