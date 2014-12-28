package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.axelby.podax.R;
import com.axelby.podax.Stats;

// todo: fragment doesn't update when stats change
public class StatsFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.stats, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		TextView listenTime = (TextView) getActivity().findViewById(R.id.listen_time);
		listenTime.setText(Stats.getListenTimeString(getActivity()));
		TextView completions = (TextView) getActivity().findViewById(R.id.completions);
		completions.setText(String.valueOf(Stats.getCompletions(getActivity())));
	}
}
