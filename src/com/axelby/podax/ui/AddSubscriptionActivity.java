package com.axelby.podax.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.axelby.podax.Helper;
import com.axelby.podax.R;

public class AddSubscriptionActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.discover);

        final ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab().setText("Add")
    		.setTabListener(new ActionBar.TabListener() {
    			AddSubscriptionFragment _fragment;

				@Override
				public void onTabUnselected(Tab tab, FragmentTransaction ft) {
					ft.remove(_fragment);
				}
				
				@Override
				public void onTabSelected(Tab tab, FragmentTransaction ft) {
					if (_fragment == null)
						_fragment = new AddSubscriptionFragment();
					ft.add(R.id.tab_content, _fragment, "add");
				}
				
				@Override
				public void onTabReselected(Tab tab, FragmentTransaction ft) {
				}
			})
		);
        bar.addTab(bar.newTab().setText("Podax")
        		.setTabListener(new TabListener(this, "Podax server", "http://podax.axelby.com/popular.php"))
        		);
        bar.addTab(bar.newTab().setText("iTunes")
        		.setTabListener(new TabListener(this, "iTunes", "http://podax.axelby.com/popularitunes.php"))
        		);

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
	}

	@Override
	protected void onResume() {
		super.onResume();

		Helper.registerMediaButtons(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private class TabListener implements ActionBar.TabListener {
		Fragment _fragment = null;
		private String _source;
		private String _url;

		public TabListener(FragmentActivity activity, String source, String url) {
			_source = source;
			_url = url;
			// use existing fragment if it's there
            _fragment = activity.getSupportFragmentManager().findFragmentByTag(_source);
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction transaction) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction transaction) {
			if (_fragment == null)
				_fragment = new PopularSubscriptionListFragment(_source, _url);
			transaction.add(R.id.tab_content, _fragment, _source);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
			transaction.remove(_fragment);
		}
	}

}
