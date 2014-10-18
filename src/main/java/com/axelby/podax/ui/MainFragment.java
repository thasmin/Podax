package com.axelby.podax.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class MainFragment extends Fragment {

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment _fragment;
        private final Context _context;
        private final String _tag;
        private final Class<T> _class;

        public TabListener(Context context, String tag, Class<T> clz) {
            _context = context;
            _tag = tag;
            _class = clz;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (_fragment == null) {
                _fragment = Fragment.instantiate(_context, _class.getName());
                ft.add(R.id.fragment, _fragment, _tag);
            } else {
                ft.attach(_fragment);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (_fragment != null) {
                ft.detach(_fragment);
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

        }
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		FrameLayout frame = new FrameLayout(container.getContext());
		frame.setId(R.id.fragment);
        frame.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		return frame;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBarActivity activity = (ActionBarActivity) getActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            actionBar.addTab(actionBar.newTab()
                            .setText(R.string.playlist)
                            .setTabListener(new TabListener<PlaylistFragment>(
                                    activity, "playlist", PlaylistFragment.class))
            );

            actionBar.addTab(actionBar.newTab()
                            .setText(R.string.podcasts)
                            .setTabListener(new TabListener<SubscriptionListFragment>(
                                    activity, "podcasts", SubscriptionListFragment.class))
            );
        }
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.now_playing) {
            Helper.changeFragment(getActivity(), EpisodeDetailFragment.class, null);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
