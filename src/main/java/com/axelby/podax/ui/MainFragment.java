package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.axelby.podax.R;

public class MainFragment extends Fragment {
    int _fragment = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final FragmentActivity activity = getActivity();
        activity.findViewById(R.id.toolbar_playlist_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_fragment == 0)
                    return;
                _fragment = 0;
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new PlaylistFragment()).commit();
            }
        });
        activity.findViewById(R.id.toolbar_subscriptions_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_fragment == 1)
                    return;
                _fragment = 1;
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new SubscriptionListFragment()).commit();
            }
        });

        if (_fragment == 1)
            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.fragment, new SubscriptionListFragment()).commit();
        else
            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.fragment, new PlaylistFragment()).commit();
    }

}
